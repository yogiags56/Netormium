'use strict';

const dns = require('node:dns').promises;
const net = require('node:net');

function ipv4IsPublic(address) {
  if (net.isIP(address) !== 4) return false;
  const p = address.split('.').map(Number);
  const a = p[0], b = p[1], c = p[2];
  if (a === 0 || a === 10 || a === 127 || a >= 224) return false;
  if (a === 100 && b >= 64 && b <= 127) return false;
  if (a === 169 && b === 254) return false;
  if (a === 172 && b >= 16 && b <= 31) return false;
  if (a === 192 && (b === 168 || (b === 0 && c === 0) ||
      (b === 0 && c === 2) || (b === 88 && c === 99))) return false;
  if (a === 198 && (b === 18 || b === 19 || (b === 51 && c === 100))) return false;
  if (a === 203 && b === 0 && c === 113) return false;
  if (a === 255) return false;
  return true;
}

function expandIpv6(address) {
  let value = address.toLowerCase().split('%')[0];
  if (value.includes('.')) {
    const lastColon = value.lastIndexOf(':');
    const v4 = value.slice(lastColon + 1);
    if (net.isIP(v4) !== 4) return null;
    const p = v4.split('.').map(Number);
    value = value.slice(0, lastColon) + ':' +
      (((p[0] << 8) | p[1]).toString(16)) + ':' +
      (((p[2] << 8) | p[3]).toString(16));
  }
  const halves = value.split('::');
  if (halves.length > 2) return null;
  const left = halves[0] ? halves[0].split(':') : [];
  const right = halves.length === 2 && halves[1] ? halves[1].split(':') : [];
  const missing = 8 - left.length - right.length;
  if ((halves.length === 1 && missing !== 0) || (halves.length === 2 && missing < 1)) return null;
  const words = left.concat(new Array(missing).fill('0'), right);
  if (words.length !== 8 || words.some(x => !/^[0-9a-f]{1,4}$/.test(x))) return null;
  return words.map(x => parseInt(x, 16));
}

function ipv6IsPublic(address) {
  if (net.isIP(address) !== 6) return false;
  const w = expandIpv6(address);
  if (!w) return false;

  // IPv4-mapped IPv6: apply the IPv4 policy to the embedded address.
  if (w.slice(0, 5).every(x => x === 0) && w[5] === 0xffff) {
    const v4 = [
      (w[6] >> 8) & 255, w[6] & 255, (w[7] >> 8) & 255, w[7] & 255
    ].join('.');
    return ipv4IsPublic(v4);
  }

  // Only global-unicast 2000::/3 is accepted; exclude special-use blocks.
  if ((w[0] & 0xe000) !== 0x2000) return false;
  if (w[0] === 0x2001 && w[1] === 0x0db8) return false; // documentation
  if (w[0] === 0x2001 && w[1] === 0x0002) return false; // benchmarking
  if (w[0] === 0x2001 && (w[1] & 0xfff0) === 0x0010) return false; // ORCHID
  if (w[0] === 0x2002) return false; // 6to4 transition
  return true;
}

function isPublicIp(address) {
  const family = net.isIP(address);
  if (family === 4) return ipv4IsPublic(address);
  if (family === 6) return ipv6IsPublic(address);
  return false;
}

function parsePublicHttpUrl(raw) {
  let url;
  try { url = new URL(raw); } catch (_) { return null; }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') return null;
  if (!url.hostname || url.username || url.password) return null;
  const hostname = url.hostname.replace(/^\[|\]$/g, '').replace(/\.$/, '').toLowerCase();
  if (!hostname || hostname === 'localhost' || hostname.endsWith('.localhost') ||
      hostname.endsWith('.local') || hostname.endsWith('.internal')) return null;
  return { url, hostname };
}

async function isSafePublicHttpUrl(raw) {
  const parsed = parsePublicHttpUrl(raw);
  if (!parsed) return false;
  if (net.isIP(parsed.hostname)) return isPublicIp(parsed.hostname);
  try {
    const records = await dns.lookup(parsed.hostname, { all: true, verbatim: true });
    return records.length > 0 && records.every(record => isPublicIp(record.address));
  } catch (_) {
    return false;
  }
}

module.exports = { isPublicIp, parsePublicHttpUrl, isSafePublicHttpUrl };
