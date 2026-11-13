const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { parseAttributes, validatePackage } = require('../scripts/check-midlet-package');

function makeFixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-midlet-package-'));
  const classes = path.join(root, 'classes', 'com', 'netormium', 'client');
  fs.mkdirSync(classes, { recursive: true });
  fs.writeFileSync(path.join(classes, 'NetormiumMIDlet.class'), Buffer.from([0xca, 0xfe, 0xba, 0xbe]));
  const manifest = path.join(root, 'MANIFEST.MF');
  fs.writeFileSync(manifest, [
    'Manifest-Version: 1.0',
    'MIDlet-Name: Netormium',
    'MIDlet-Version: 1.0.0',
    'MIDlet-Vendor: Netormium Project',
    'MIDlet-1: Netormium,,com.netormium.client.NetormiumMIDlet',
    'MIDlet-Permissions: javax.microedition.io.Connector.http, javax.microedition.io.Connector.https',
    'MicroEdition-Profile: MIDP-2.0',
    'MicroEdition-Configuration: CLDC-1.1',
    ''
  ].join('\r\n'));
  const jar = path.join(root, 'Netormium.jar');
  const created = spawnSync('jar', ['cfm', jar, manifest, '-C', path.join(root, 'classes'), '.'], { encoding: 'utf8' });
  if (created.status !== 0) throw new Error(created.stderr || 'jar fixture creation failed');
  const size = fs.statSync(jar).size;
  const jad = path.join(root, 'Netormium.jad');
  fs.writeFileSync(jad, [
    'MIDlet-Name: Netormium',
    'MIDlet-Version: 1.0.0',
    'MIDlet-Vendor: Netormium Project',
    'MIDlet-1: Netormium,,com.netormium.client.NetormiumMIDlet',
    'MIDlet-Permissions: javax.microedition.io.Connector.http, javax.microedition.io.Connector.https',
    'MIDlet-Jar-URL: Netormium.jar',
    `MIDlet-Jar-Size: ${size}`,
    'MicroEdition-Profile: MIDP-2.0',
    'MicroEdition-Configuration: CLDC-1.1',
    ''
  ].join('\r\n'));
  return { root, jar, jad };
}

test('manifest and JAD attribute parser supports continuation lines', () => {
  const attrs = parseAttributes('MIDlet-Name: Netormium\nLong-Value: part one\n part two\n');
  assert.equal(attrs['MIDlet-Name'], 'Netormium');
  assert.equal(attrs['Long-Value'], 'part onepart two');
});

test('validates JAR entry class, manifest, JAD and exact JAR byte size', () => {
  const fixture = makeFixture();
  try {
    const result = validatePackage(fixture.jar, fixture.jad);
    assert.equal(result.ok, true, result.errors.join('; '));
    assert.equal(result.jarBytes, fs.statSync(fixture.jar).size);
    assert.ok(result.entryCount > 0);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects JAD byte-size mismatch and metadata inconsistency', () => {
  const fixture = makeFixture();
  try {
    fs.appendFileSync(fixture.jad, '');
    let jad = fs.readFileSync(fixture.jad, 'utf8').replace(/MIDlet-Jar-Size: \d+/, 'MIDlet-Jar-Size: 1');
    jad = jad.replace('MIDlet-Version: 1.0.0', 'MIDlet-Version: 9.9.9');
    fs.writeFileSync(fixture.jad, jad);
    const result = validatePackage(fixture.jar, fixture.jad);
    assert.equal(result.ok, false);
    assert.ok(result.errors.some(error => error.includes('MIDlet-Jar-Size mismatch')));
    assert.ok(result.errors.some(error => error.includes('MIDlet-Version mismatch')));
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects missing MIDlet JAR or JAD paths', () => {
  const result = validatePackage('/missing/Netormium.jar', '/missing/Netormium.jad');
  assert.equal(result.ok, false);
  assert.match(result.errors[0], /JAR not found/);
});


test('requires the same MIDlet-1 suite entry in JAD and manifest', () => {
  const fixture = makeFixture();
  try {
    let jad = fs.readFileSync(fixture.jad, 'utf8');
    jad = jad.replace('MIDlet-1: Netormium,,com.netormium.client.NetormiumMIDlet',
                      'MIDlet-1: Netormium,,com.example.WrongMIDlet');
    fs.writeFileSync(fixture.jad, jad);
    const result = validatePackage(fixture.jar, fixture.jad);
    assert.equal(result.ok, false);
    assert.ok(result.errors.some(error => error.includes('MIDlet-1 mismatch')));
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});
