const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { discoverSdk } = require('../scripts/discover-j2me-sdk');

test('SDK discovery finds CLDC, MIDP and executable preverifier in a WTK-like tree', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-wtk-'));
  try {
    const lib = path.join(root, 'lib');
    const bin = path.join(root, 'bin');
    fs.mkdirSync(lib, { recursive: true });
    fs.mkdirSync(bin, { recursive: true });
    fs.writeFileSync(path.join(lib, 'cldcapi11.jar'), 'placeholder');
    fs.writeFileSync(path.join(lib, 'midpapi20.jar'), 'placeholder');
    const preverify = path.join(bin, 'preverify');
    fs.writeFileSync(preverify, '#!/bin/sh\nexit 0\n');
    fs.chmodSync(preverify, 0o755);
    const found = discoverSdk([root]);
    assert.equal(found.ready, true);
    assert.match(found.cldcApi, /cldcapi11\.jar$/);
    assert.match(found.midpApi, /midpapi20\.jar$/);
    assert.match(found.preverify, /preverify$/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('SDK discovery refuses an incomplete SDK tree', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-wtk-incomplete-'));
  try {
    const found = discoverSdk([root]);
    assert.equal(found.ready, false);
    assert.equal(found.cldcApi, '');
    assert.equal(found.midpApi, '');
    assert.equal(found.preverify, '');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
