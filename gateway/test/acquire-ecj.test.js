const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {
  RELEASE, BUILD, FILE, URL, URLS, SHA512, MAIN_CLASS, isJar, sha512, probeEcj
} = require('../scripts/acquire-ecj');

test('ECJ acquisition is pinned to the stable Eclipse 4.41 GA artifact', () => {
  assert.equal(RELEASE, '4.41');
  assert.equal(BUILD, 'R-4.41-202608281142');
  assert.equal(FILE, 'ecj-4.41.jar');
  assert.equal(MAIN_CLASS, 'org.eclipse.jdt.internal.compiler.batch.Main');
  assert.match(URL, /^https:\/\/download\.eclipse\.org\/eclipse\/downloads\/drops4\/R-4\.41-/);
  assert.equal(URLS.length, 2);
  assert.match(URLS[1], /^https:\/\/archive\.eclipse\.org\/eclipse\/downloads\/drops4\/R-4\.41-/);
  assert.equal(SHA512, '080bb403adb3b88b5b5f382388922425de5274f7729f0a94702f0e2a516c7d13c6740faab1cb20786b1dda0c5da933aa9b842f7c64867414ff75fa668ea06b09');
});

test('JAR validator rejects non-archives and accepts ZIP local headers', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-ecj-'));
  const file = path.join(dir, 'sample.jar');
  try {
    fs.writeFileSync(file, 'not a jar');
    assert.equal(isJar(file), false);
    fs.writeFileSync(file, Buffer.from([0x50, 0x4b, 0x03, 0x04, 0, 0]));
    assert.equal(isJar(file), true);
    assert.equal(sha512(file).length, 128);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});

test('ECJ probe requires both successful launch and legacy mode support', () => {
  const goodRunner = (_command, args) => ({
    status: 0, stdout: `Batch compiler help: ${args.join(' ')} -1.3`, stderr: ''
  });
  const badRunner = () => ({ status: 1, stdout: 'error', stderr: '' });
  assert.equal(probeEcj('/fake/ecj.jar', 'java', goodRunner).ok, true);
  assert.equal(probeEcj('/fake/ecj.jar', 'java', badRunner).ok, false);
});
