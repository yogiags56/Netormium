/**
 * Netormium Chromium Gateway 1.0.0-rc21
 * Node.js + Playwright. Run only on a trusted server.
 */
'use strict';

const http = require('http');
const { chromium } = require('playwright');
const { URL } = require('url');
const crypto = require('crypto');
const { isSafePublicHttpUrl } = require('./host-policy');
const PendingNavigationRegistry = require('./pending-navigation-registry');
const { isPngBuffer } = require('./response-validation');
const metadataLimits = require('./metadata-limits');
const viewportLimits = require('./viewport-limits');
const sessionProtocol = require('./session-protocol');

const HOST = process.env.HOST || '127.0.0.1';
const PORT = Number(process.env.PORT || 8787);
const MAX_URL_LENGTH = 4096;
const NAV_TIMEOUT = 20000;
const MAX_TEXT = 48000;
const MAX_LINKS = 80;

let browser;
const sessions = new Map();
const pendingNavigations = new PendingNavigationRegistry();
const SESSION_TTL_MS = 60 * 60 * 1000;
const MAX_SESSIONS = 30;
const SNAPSHOT_CACHE_TTL_MS = 5000;
const SNAPSHOT_CACHE_MAX_BYTES = 8 * 1024 * 1024;
const SNAPSHOT_CACHE_MAX_ENTRIES_PER_SESSION = 2;
let snapshotCacheBytes = 0;

async function configurePageRoutes(page, dataSaver, loadImages) {
  await page.route('**/*', async route => {
    const request = route.request();
    if (!await isSafePublicHttpUrl(request.url())) return route.abort();
    const type = request.resourceType();
    if (!loadImages && type === 'image') return route.abort();
    if (dataSaver && (type === 'image' || type === 'media' || type === 'font')) return route.abort();
    return route.continue();
  });
}

function clearSnapshotCache(item) {
  if (!item || !item.snapshotCache) return;
  item.snapshotCache.forEach(entry => { snapshotCacheBytes -= entry.png.length; });
  item.snapshotCache.clear();
  if (snapshotCacheBytes < 0) snapshotCacheBytes = 0;
}

function expireSnapshotCache(item) {
  if (!item || !item.snapshotCache) return;
  const now = Date.now();
  item.snapshotCache.forEach((entry, key) => {
    if (now - entry.createdAt > SNAPSHOT_CACHE_TTL_MS) {
      snapshotCacheBytes -= entry.png.length;
      item.snapshotCache.delete(key);
    }
  });
  if (snapshotCacheBytes < 0) snapshotCacheBytes = 0;
}

function cacheSnapshot(item, key, png) {
  expireSnapshotCache(item);
  if (png.length > SNAPSHOT_CACHE_MAX_BYTES) return;
  const replaced = item.snapshotCache.get(key);
  if (replaced) {
    snapshotCacheBytes -= replaced.png.length;
    item.snapshotCache.delete(key);
  }
  while (snapshotCacheBytes + png.length > SNAPSHOT_CACHE_MAX_BYTES) {
    let oldest = null, owner = null, oldKey = null;
    sessions.forEach(session => {
      if (!session.snapshotCache) return;
      session.snapshotCache.forEach((entry, entryKey) => {
        if (!oldest || entry.createdAt < oldest.createdAt) {
          oldest = entry; owner = session; oldKey = entryKey;
        }
      });
    });
    if (!oldest) break;
    snapshotCacheBytes -= oldest.png.length;
    owner.snapshotCache.delete(oldKey);
  }
  while (item.snapshotCache.size >= SNAPSHOT_CACHE_MAX_ENTRIES_PER_SESSION) {
    let oldestKey = null, oldestEntry = null;
    item.snapshotCache.forEach((entry, entryKey) => {
      if (!oldestEntry || entry.createdAt < oldestEntry.createdAt) {
        oldestEntry = entry;
        oldestKey = entryKey;
      }
    });
    if (oldestKey === null) break;
    snapshotCacheBytes -= oldestEntry.png.length;
    item.snapshotCache.delete(oldestKey);
  }
  item.snapshotCache.set(key, { png: png, createdAt: Date.now() });
  snapshotCacheBytes += png.length;
  if (snapshotCacheBytes < 0) snapshotCacheBytes = 0;
}

function getSession(id) {
  const item = sessions.get(id);
  if (!item) return null;
  if (Date.now() - item.lastUsed > SESSION_TTL_MS) {
    clearSnapshotCache(item);
    item.context.close().catch(() => {});
    sessions.delete(id);
    return null;
  }
  item.lastUsed = Date.now();
  return item;
}

async function closeSession(id) {
  const item = sessions.get(id);
  if (item) {
    clearSnapshotCache(item);
    sessions.delete(id);
    await item.context.close().catch(() => {});
  }
}

function sessionErrorResponse(message) {
  return sessionProtocol.errorResponse(message);
}

async function sessionMetadata(item) {
  const data = await item.page.evaluate(() => {
    const clean = s => (s || '').replace(/\s+/g, ' ').trim().slice(0, 72);
    const nodes = Array.prototype.slice.call(
      document.querySelectorAll('a[href],button,input,textarea,select,[role="button"],[onclick]')
    );
    const elements = [];
    for (let i = 0; i < nodes.length && elements.length < 24; i++) {
      const el = nodes[i];
      const r = el.getBoundingClientRect();
      if (!r.width || !r.height) continue;
      const style = window.getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden') continue;
      const tag = el.tagName.toLowerCase();
      const type = (el.getAttribute('type') || '').toLowerCase();
      let kind = 'button';
      if (tag === 'a') kind = 'link';
      else if (tag === 'input' || tag === 'textarea' || tag === 'select') kind = 'input';
      const label = clean(el.innerText || el.getAttribute('aria-label') ||
        el.getAttribute('placeholder') || el.value || el.title || el.href || tag);
      elements.push({
        kind, x: Math.round(r.left), y: Math.round(r.top),
        w: Math.round(r.width), h: Math.round(r.height),
        label, href: (el.href || '').slice(0, 256), inputType: type.slice(0, 24), focused: document.activeElement === el
      });
    }
    return {
      title: document.title || '',
      url: location.href,
      text: ((document.body && (document.body.innerText || document.body.textContent)) || '').slice(0, 6000),
      elements
    };
  }); 
  return sessionProtocol.metadataResponse(item, data);
}

async function createSession(rawUrl, width, height, dataSaver, loadImages, mobileView, requestId) {
  if (!await isSafePublicHttpUrl(rawUrl)) throw new Error('URL rejected by gateway policy');
  if (sessions.size >= MAX_SESSIONS) {
    const oldest = sessions.keys().next().value;
    if (oldest) await closeSession(oldest);
  }
  let context = null;
  let cancelled = false;
  if (requestId) pendingNavigations.register(requestId, () => {
    cancelled = true;
    if (context) context.close().catch(() => {});
  });
  try {
    context = await browser.newContext({
      viewport: { width, height }, deviceScaleFactor: 1,
      ...(mobileView ? { isMobile: true, hasTouch: true,
        userAgent: 'Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36' } : {}),
      javaScriptEnabled: true, acceptDownloads: false, serviceWorkers: 'block'
    });
    if (cancelled) throw new Error('Navigation cancelled by client');
    const page = await context.newPage();
    page.setDefaultNavigationTimeout(NAV_TIMEOUT);
    await configurePageRoutes(page, dataSaver, loadImages);
    await page.goto(rawUrl, { waitUntil: 'domcontentloaded', timeout: NAV_TIMEOUT });
    if (cancelled) throw new Error('Navigation cancelled by client');
    await page.waitForTimeout(500);
    if (cancelled) throw new Error('Navigation cancelled by client');
    const id = crypto.randomBytes(12).toString('hex');
    const item = { id, context, page, dataSaver: !!dataSaver, loadImages: !!loadImages, mobileView: !!mobileView, lastUsed: Date.now(), snapshotCache: new Map() };
    sessions.set(id, item);
    return item;
  } catch (error) {
    if (context) await context.close().catch(() => {});
    throw error;
  } finally {
    if (requestId) pendingNavigations.finish(requestId);
  }
}


function encode(value) {
  return encodeURIComponent(String(value == null ? '' : value))
    .replace(/%20/g, '%20');
}

function protocolResponse(status, pageUrl, title, text, links, error) {
  const lines = [
    'NTM/1',
    'STATUS\t' + status,
    'URL\t' + encode(pageUrl || ''),
    'TITLE\t' + encode(title || ''),
    'TEXT\t' + encode((text || '').slice(0, MAX_TEXT))
  ];
  if (error) lines.push('ERROR\t' + encode(error));
  (links || []).forEach((link, index) => {
    lines.push('LINK\t' + (index + 1) + '\t' +
      encode(link.text) + '\t' + encode(link.href));
  });
  lines.push('END');
  return lines.join('\n') + '\n';
}

function send(res, code, body) {
  res.writeHead(code, {
    'Content-Type': 'text/plain; charset=utf-8',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff'
  });
  res.end(body);
}

async function renderSnapshot(rawUrl, width, height, scrollY) {
  const context = await browser.newContext({
    viewport: { width, height }, deviceScaleFactor: 1,
    javaScriptEnabled: true, acceptDownloads: false, serviceWorkers: 'block'
  });
  try {
    const page = await context.newPage();
    page.setDefaultNavigationTimeout(NAV_TIMEOUT);
    await page.route('**/*', async route => {
      if (!await isSafePublicHttpUrl(route.request().url())) return route.abort();
      return route.continue();
    });
    await page.goto(rawUrl, { waitUntil: 'domcontentloaded', timeout: NAV_TIMEOUT });
    await page.waitForTimeout(500);
    await page.evaluate((y) => window.scrollTo(0, y), scrollY);
    await page.waitForTimeout(100);
    return await page.screenshot({ type: 'png', fullPage: false, animations: 'disabled' });
  } finally { await context.close(); }
}

async function render(rawUrl) {
  if (!await isSafePublicHttpUrl(rawUrl)) {
    return protocolResponse(400, '', '', '', [], 'URL rejected by gateway policy');
  }

  const context = await browser.newContext({
    viewport: { width: 960, height: 720 },
    javaScriptEnabled: true,
    acceptDownloads: false,
    serviceWorkers: 'block'
  });

  try {
    const page = await context.newPage();
    page.setDefaultNavigationTimeout(NAV_TIMEOUT);
    await page.route('**/*', async route => {
      const requestUrl = route.request().url();
      // Restrict browser subrequests to HTTP(S); block local/private hostnames too.
      if (!await isSafePublicHttpUrl(requestUrl)) return route.abort();
      return route.continue();
    });

    const response = await page.goto(rawUrl, {
      waitUntil: 'domcontentloaded',
      timeout: NAV_TIMEOUT
    });
    await page.waitForTimeout(700);

    const extracted = await page.evaluate((maxLinks) => {
      const title = document.title || '';
      const root = document.body || document.documentElement;
      const text = root ? (root.innerText || root.textContent || '') : '';
      const links = [];
      const anchors = document.querySelectorAll('a[href]');
      for (let i = 0; i < anchors.length && links.length < maxLinks; i++) {
        const a = anchors[i];
        const label = (a.innerText || a.getAttribute('aria-label') || a.title || a.href || '')
          .replace(/\s+/g, ' ').trim();
        if (!label) continue;
        links.push({ text: label.slice(0, 180), href: a.href });
      }
      return { title, text, links };
    }, MAX_LINKS);

    return protocolResponse(
      response ? response.status() : 200,
      page.url(),
      extracted.title,
      extracted.text,
      extracted.links,
      ''
    );
  } catch (error) {
    return protocolResponse(502, rawUrl, 'Gateway error', '', [],
      String(error && error.message ? error.message : error));
  } finally {
    await context.close();
  }
}

const server = http.createServer(async (req, res) => {
  const requestUrl = new URL(req.url, 'http://' + req.headers.host);
  if (req.method === 'GET' && requestUrl.pathname === '/health') {
    return send(res, 200, 'Netormium Gateway 1.0 RC21 OK\n');
  }
  if (req.method === 'GET' && requestUrl.pathname === '/v1/info') {
    return send(res, 200,
      'NTMINFO/1\\n' +
      'GATEWAY\t1.0.0-rc21\n' +
      'CHROMIUM\t' + browser.version() + '\n' +
      'END\\n');
  }
  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/cancel') {
    const requestId = requestUrl.searchParams.get('rid') || '';
    const cancelled = pendingNavigations.cancel(requestId);
    return send(res, cancelled ? 200 : 404, cancelled ? 'CANCELLED\n' : 'NOT_PENDING\n');
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/open') {
    const target = requestUrl.searchParams.get('url') || '';
    const viewport = viewportLimits.viewportDimensions(
      requestUrl.searchParams.get('w'),
      requestUrl.searchParams.get('h'),
      viewportLimits.DEFAULT_SESSION_HEIGHT
    );
    const width = viewport.width;
    const height = viewport.height;
    try {
      if (!target || target.length > MAX_URL_LENGTH) return send(res, 400, 'Invalid URL\n');
      const dataSaver = requestUrl.searchParams.get('ds') === '1';
      const loadImages = requestUrl.searchParams.get('img') !== '0';
      const mobileView = requestUrl.searchParams.get('mv') === '1';
      const requestId = requestUrl.searchParams.get('rid') || '';
      const item = await createSession(target, width, height, dataSaver, loadImages, mobileView, requestId);
      return send(res, 200, await sessionMetadata(item));
    } catch (error) {
      return send(res, 502, sessionErrorResponse(error && error.message ? error.message : error));
    }
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/metadata') {
    const item = getSession(requestUrl.searchParams.get('id') || '');
    if (!item) return send(res, 404, sessionErrorResponse('Session expired; reopen the page.'));
    try {
      return send(res, 200, await sessionMetadata(item));
    } catch (error) {
      return send(res, 502, sessionErrorResponse(error && error.message ? error.message : error));
    }
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/snapshot') {
    const item = getSession(requestUrl.searchParams.get('id') || '');
    if (!item) return send(res, 404, sessionErrorResponse('Session expired; reopen the page.'));
    const y = Math.max(0, Math.min(100000, Number(requestUrl.searchParams.get('y') || 0)));
    try {
      const cacheKey = String(y);
      expireSnapshotCache(item);
      const cached = item.snapshotCache.get(cacheKey);
      if (cached) {
        // Restore the browser scroll position too, so the following metadata
        // refresh reports coordinates matching the cached viewport image.
        await item.page.evaluate(offset => window.scrollTo(0, offset), y);
        res.writeHead(200, { 'Content-Type': 'image/png', 'Cache-Control': 'private, max-age=0',
          'X-Netormium-Cache': 'HIT', 'X-Content-Type-Options': 'nosniff' });
        return res.end(cached.png);
      }
      await item.page.evaluate(offset => window.scrollTo(0, offset), y);
      await item.page.waitForTimeout(80);
      const png = await item.page.screenshot({ type: 'png', fullPage: false, animations: 'disabled' });
      if (!isPngBuffer(png)) return send(res, 502, 'Invalid PNG generated by browser\n');
      cacheSnapshot(item, cacheKey, png);
      res.writeHead(200, { 'Content-Type': 'image/png', 'Cache-Control': 'private, max-age=0',
        'X-Netormium-Cache': 'MISS', 'X-Content-Type-Options': 'nosniff' });
      return res.end(png);
    } catch (error) {
      return send(res, 502, String(error && error.message ? error.message : error));
    }
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/tap') {
    const item = getSession(requestUrl.searchParams.get('id') || '');
    if (!item) return send(res, 404, sessionErrorResponse('Session expired; reopen the page.'));
    const x = Number(requestUrl.searchParams.get('x') || 0);
    const y = Number(requestUrl.searchParams.get('y') || 0);
    if (!Number.isFinite(x) || !Number.isFinite(y)) return send(res, 400, 'Invalid coordinates\n');
    try {
      clearSnapshotCache(item);
      await item.page.mouse.click(x, y);
      await item.page.waitForTimeout(350);
      return send(res, 200, await sessionMetadata(item));
    } catch (error) {
      return send(res, 502, sessionErrorResponse(error && error.message ? error.message : error));
    }
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/type') {
    const item = getSession(requestUrl.searchParams.get('id') || '');
    if (!item) return send(res, 404, sessionErrorResponse('Session expired; reopen the page.'));
    const value = requestUrl.searchParams.get('text') || '';
    if (value.length > 1024) return send(res, 413, 'Text too long\n');
    try {
      clearSnapshotCache(item);
      await item.page.keyboard.insertText(value);
      return send(res, 200, await sessionMetadata(item));
    } catch (error) {
      return send(res, 502, sessionErrorResponse(error && error.message ? error.message : error));
    }
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/key') {
    const item = getSession(requestUrl.searchParams.get('id') || '');
    if (!item) return send(res, 404, sessionErrorResponse('Session expired; reopen the page.'));
    const key = requestUrl.searchParams.get('key') || '';
    const allowed = ['Enter', 'Tab', 'Backspace', 'Escape', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];
    if (allowed.indexOf(key) < 0) return send(res, 400, 'Key not allowed\n');
    try {
      clearSnapshotCache(item);
      await item.page.keyboard.press(key);
      await item.page.waitForTimeout(250);
      return send(res, 200, await sessionMetadata(item));
    } catch (error) {
      return send(res, 502, sessionErrorResponse(error && error.message ? error.message : error));
    }
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/session/close') {
    await closeSession(requestUrl.searchParams.get('id') || '');
    return send(res, 200, 'CLOSED\n');
  }

  if (req.method === 'GET' && requestUrl.pathname === '/v1/snapshot') {
    const target = requestUrl.searchParams.get('url') || '';
    const viewport = viewportLimits.viewportDimensions(
      requestUrl.searchParams.get('w'),
      requestUrl.searchParams.get('h'),
      viewportLimits.DEFAULT_SNAPSHOT_HEIGHT
    );
    const width = viewport.width;
    const height = viewport.height;
    const scrollY = Math.max(0, Math.min(100000, Number(requestUrl.searchParams.get('y') || 0)));
    if (!target || target.length > MAX_URL_LENGTH || !await isSafePublicHttpUrl(target)) {
      return send(res, 400, 'Invalid URL\n');
    }
    try {
      const png = await renderSnapshot(target, width, height, scrollY);
      res.writeHead(200, {
        'Content-Type': 'image/png',
        'Cache-Control': 'no-store',
        'X-Content-Type-Options': 'nosniff'
      });
      return res.end(png);
    } catch (error) {
      return send(res, 502, String(error && error.message ? error.message : error));
    }
  }

  if (req.method !== 'GET' || requestUrl.pathname !== '/v1/page') {
    return send(res, 404, 'Not found\n');
  }
  const target = requestUrl.searchParams.get('url') || '';
  if (!target || target.length > MAX_URL_LENGTH) {
    return send(res, 400, protocolResponse(400, '', '', '', [], 'Missing or oversized URL'));
  }
  try {
    const result = await render(target);
    return send(res, 200, result);
  } catch (error) {
    return send(res, 500, protocolResponse(500, '', '', '', [],
      String(error && error.message ? error.message : error)));
  }
});

(async () => {
  browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-dev-shm-usage']
  });
  server.listen(PORT, HOST, () => {
    process.stdout.write('Netormium Gateway listening on http://' + HOST + ':' + PORT + '\n');
  });
})().catch(error => {
  process.stderr.write(String(error) + '\n');
  process.exit(1);
});

function shutdown() {
  server.close();
  if (browser) browser.close().finally(() => process.exit(0));
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
