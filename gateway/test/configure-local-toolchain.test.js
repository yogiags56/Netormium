'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { configure, parseArgs, propertyPath } = require('../scripts/configure-local-toolchain');

function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-local-sdk-'));
  const lib = path.join(root, 'lib');
  const bin = path.join(root, 'bin');
  fs.mkdirSync(lib, { recursive: true });
  fs.mkdirSync(bin, { recursive: true });
  const cldc = path.join(lib, 'cldcapi11.jar');
  const midp = path.join(lib, 'midpapi20.jar');
  fs.writeFileSync(cldc, Buffer.from([0x50, 0x4b, 0x03, 0x04]));
  fs.writeFileSync(midp, Buffer.from([0x50, 0x4b, 0x03, 0x04]));
  const preverify = path.join(bin, 'preverify');
  fs.writeFileSync(preverify, '#!/bin/sh\nexit 0\n');
  fs.chmodSync(preverify, 0o755);
  const ecj = path.join(root, 'ecj.jar');
  fs.writeFileSync(ecj, Buffer.from([0x50, 0x4b, 0x03, 0x04]));
  return { root, cldc, midp, preverify, ecj };
}

test('parses explicit local SDK, ECJ, and output paths', () => {
  const parsed = parseArgs(['--root', './wtk', '--ecj', './ecj.jar', '--output', './build.properties']);
  assert.equal(parsed.root, path.resolve('./wtk'));
  assert.equal(parsed.ecj, path.resolve('./ecj.jar'));
  assert.equal(parsed.output, path.resolve('./build.properties'));
});

test('writes build.properties only after all local components validate', () => {
  const f = fixture();
  try {
    const output = path.join(f.root, 'generated.properties');
    const entries = {
      [f.cldc]: ['java/lang/Object.class', 'javax/microedition/io/Connection.class', 'javax/microedition/io/Connector.class'],
      [f.midp]: ['javax/microedition/midlet/MIDlet.class', 'javax/microedition/lcdui/Display.class', 'javax/microedition/lcdui/Canvas.class', 'javax/microedition/rms/RecordStore.class', 'javax/microedition/rms/RecordEnumeration.class']
    };
    const runner = (_command, args) => ({ status: 0, stdout: (entries[args[1]] || []).join('\n'), stderr: '' });
    const result = configure({ root: f.root, ecj: f.ecj, output, runner, ecjVerifier: () => ({ sha512: 'verified' }) });
    assert.equal(fs.existsSync(output), true);
    const text = fs.readFileSync(output, 'utf8');
    assert.match(text, /compiler\.backend=ecj/);
    assert.ok(text.includes(`cldc.api.jar=${propertyPath(f.cldc)}`));
    assert.ok(text.includes(`midp.api.jar=${propertyPath(f.midp)}`));
    assert.ok(text.includes(`preverify.executable=${propertyPath(f.preverify)}`));
    assert.equal(result.output, output);
  } finally { fs.rmSync(f.root, { recursive: true, force: true }); }
});

test('refuses invalid toolchain and does not leave partial properties', () => {
  const f = fixture();
  try {
    const output = path.join(f.root, 'should-not-exist.properties');
    assert.throws(() => configure({ root: f.root, ecj: f.ecj, output,
      runner: () => ({ status: 0, stdout: 'wrong/Class.class', stderr: '' }),
      ecjVerifier: () => ({ sha512: 'verified' }) }), /Local toolchain was not configured/);
    assert.equal(fs.existsSync(output), false);
  } finally { fs.rmSync(f.root, { recursive: true, force: true }); }
});

test('rejects missing ECJ rather than silently selecting desktop javac', () => {
  const f = fixture();
  try {
    assert.throws(() => configure({ root: f.root, output: path.join(f.root, 'x.properties'),
      runner: () => ({ status: 0, stdout: '', stderr: '' }) }), /--ecj must point/);
  } finally { fs.rmSync(f.root, { recursive: true, force: true }); }
});
