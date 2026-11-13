#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const CLDC_NAMES = ['cldcapi11.jar', 'cldcapi.jar'];
const MIDP_NAMES = ['midpapi20.jar', 'midpapi.jar'];
const PREVERIFY_NAMES = process.platform === 'win32'
  ? ['preverify.exe', 'preverify.bat']
  : ['preverify', 'preverify.sh'];
const DEFAULT_ROOTS = [
  process.env.JAVA_ME_HOME,
  process.env.WTK_HOME,
  process.env.JAVAME_HOME,
  '/opt/WTK',
  '/opt/java-me-sdk',
  '/usr/local/WTK',
  '/usr/local/java-me-sdk',
  '/usr/share/java',
  'C:\\WTK',
  'C:\\Java_ME_platform_SDK',
  '/Applications/Java_ME_SDK'
].filter(Boolean);

function walk(root, maxDepth = 5) {
  const found = [];
  const visited = new Set();
  function visit(dir, depth) {
    let real;
    try { real = fs.realpathSync(dir); } catch (_) { return; }
    if (visited.has(real) || depth > maxDepth) return;
    visited.add(real);
    let entries;
    try { entries = fs.readdirSync(real, { withFileTypes: true }); } catch (_) { return; }
    for (const entry of entries) {
      const full = path.join(real, entry.name);
      if (entry.isFile()) found.push(full);
      else if (entry.isDirectory() && !entry.isSymbolicLink() &&
        !['.git', 'node_modules', 'proc', 'sys', 'dev'].includes(entry.name)) {
        visit(full, depth + 1);
      }
    }
  }
  visit(root, 0);
  return found;
}

function discoverSdk(roots = DEFAULT_ROOTS) {
  const files = [];
  for (const root of roots) {
    if (!fs.existsSync(root)) continue;
    files.push(...walk(root));
  }
  const pick = names => files.find(file => names.includes(path.basename(file).toLowerCase())) || '';
  const cldc = pick(CLDC_NAMES);
  const midp = pick(MIDP_NAMES);
  const preverify = pick(PREVERIFY_NAMES);
  const exists = file => !!file && fs.existsSync(file) && fs.statSync(file).isFile();
  const executable = exists(preverify) &&
    (process.platform === 'win32' || (fs.statSync(preverify).mode & 0o111) !== 0);
  return {
    cldcApi: exists(cldc) ? cldc : '',
    midpApi: exists(midp) ? midp : '',
    preverify: executable ? preverify : '',
    roots: roots.filter(root => fs.existsSync(root)),
    ready: exists(cldc) && exists(midp) && executable
  };
}

function main(argv = process.argv.slice(2)) {
  const args = argv;
  if (args.includes('--help')) {
    console.log('Usage: node scripts/discover-j2me-sdk.js [--root PATH] [--json]');
    console.log('Searches local SDK/WTK directories; does not download or install licensed SDKs.');
    return;
  }
  const roots = DEFAULT_ROOTS.slice();
  for (let i = 0; i < args.length; i += 1) {
    if (args[i] === '--root' && args[i + 1]) roots.push(path.resolve(args[++i]));
  }
  const result = discoverSdk(Array.from(new Set(roots)));
  if (args.includes('--json')) {
    console.log(JSON.stringify(result, null, 2));
    return;
  }
  console.log('Netormium Java ME SDK discovery');
  console.log(`Roots checked: ${result.roots.length ? result.roots.join(', ') : '(none found)'}`);
  console.log(`${result.cldcApi ? '[FOUND]' : '[MISSING]'} CLDC API: ${result.cldcApi || 'cldcapi11.jar'}`);
  console.log(`${result.midpApi ? '[FOUND]' : '[MISSING]'} MIDP API: ${result.midpApi || 'midpapi20.jar'}`);
  console.log(`${result.preverify ? '[FOUND]' : '[MISSING]'} Preverifier: ${result.preverify || 'preverify'}`);
  console.log(`Result: ${result.ready ? 'SDK CANDIDATE FOUND' : 'SDK NOT FOUND / INCOMPLETE'}`);
  if (!result.ready) process.exitCode = 1;
}

if (require.main === module) main();
module.exports = { CLDC_NAMES, MIDP_NAMES, PREVERIFY_NAMES, walk, discoverSdk, main };
