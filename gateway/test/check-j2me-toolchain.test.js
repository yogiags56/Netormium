'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {
  parseArgs, readProperties, resolveConfiguredPath, isZipArchive, isExecutableFile,
  ECJ_MAIN_CLASS, ecjProbeArguments, supportsEcjLegacyMode
} = require('../scripts/check-j2me-toolchain');

test('parses the explicit property-file argument', () => {
  assert.equal(parseArgs(['--property-file', 'sdk.properties']).propertyFile, 'sdk.properties');
});
test('reads Java properties and ignores comments', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-props-'));
  const file = path.join(dir, 'sdk.properties');
  fs.writeFileSync(file, '# comment\ncldc.api.jar=lib/cldc.jar\nmidp.api.jar:lib/midp.jar\n');
  assert.deepEqual(readProperties(file), { 'cldc.api.jar': 'lib/cldc.jar', 'midp.api.jar': 'lib/midp.jar' });
  fs.rmSync(dir, { recursive: true, force: true });
});
test('resolves relative SDK paths against the property file directory', () => {
  assert.equal(resolveConfiguredPath('sdk/cldc.jar', '/opt/netormium/build.properties'), '/opt/netormium/sdk/cldc.jar');
  assert.equal(resolveConfiguredPath('/sdk/cldc.jar', '/opt/netormium/build.properties'), '/sdk/cldc.jar');
});
test('accepts ZIP/JAR signatures and rejects ordinary files', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-jar-'));
  const jar = path.join(dir, 'api.jar');
  const txt = path.join(dir, 'not-a-jar.jar');
  fs.writeFileSync(jar, Buffer.from([0x50, 0x4b, 0x03, 0x04, 0, 0]));
  fs.writeFileSync(txt, 'not a jar');
  assert.equal(isZipArchive(jar), true);
  assert.equal(isZipArchive(txt), false);
  assert.equal(isZipArchive(path.join(dir, 'missing.jar')), false);
  fs.rmSync(dir, { recursive: true, force: true });
});
test('checks preverifier regular-file and executable permission', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-exe-'));
  const file = path.join(dir, 'preverify');
  fs.writeFileSync(file, '#!/bin/sh\nexit 0\n');
  fs.chmodSync(file, 0o644);
  assert.equal(isExecutableFile(file, 'linux'), false);
  fs.chmodSync(file, 0o755);
  assert.equal(isExecutableFile(file, 'linux'), true);
  assert.equal(isExecutableFile(file, 'win32'), true);
  fs.rmSync(dir, { recursive: true, force: true });
});

test('uses the documented ECJ batch compiler entry point', () => {
  assert.equal(ECJ_MAIN_CLASS, 'org.eclipse.jdt.compiler.batch.BatchCompiler');
  assert.deepEqual(ecjProbeArguments('/tools/ecj.jar'), [
    '-cp', '/tools/ecj.jar', 'org.eclipse.jdt.compiler.batch.BatchCompiler', '-help'
  ]);
});
test('requires a successful ECJ help command advertising legacy 1.3 mode', () => {
  assert.equal(supportsEcjLegacyMode(0, 'Options: -1.3 -1.4 -1.5'), true);
  assert.equal(supportsEcjLegacyMode(1, 'Options: -1.3'), false);
  assert.equal(supportsEcjLegacyMode(0, 'Options: -1.4 -1.5'), false);
});


test('API inventory requires the expected CLDC and MIDP class entries', () => {
  const { missingArchiveEntries } = require('../scripts/check-j2me-toolchain');
  const midpListing = [
    'javax/microedition/midlet/MIDlet.class',
    'javax/microedition/lcdui/Display.class'
  ].join('\n');
  assert.deepEqual(missingArchiveEntries(midpListing, [
    'javax/microedition/midlet/MIDlet.class',
    'javax/microedition/lcdui/Display.class'
  ]), []);
  assert.deepEqual(missingArchiveEntries(midpListing, [
    'javax/microedition/io/Connector.class'
  ]), ['javax/microedition/io/Connector.class']);
});

test('API archive inspection rejects a non-archive before invoking jar', () => {
  const { inspectApiArchive } = require('../scripts/check-j2me-toolchain');
  assert.equal(inspectApiArchive('/definitely/missing/cldc.jar', [
    'java/lang/Object.class'
  ]).ok, false);
});


test('API archive inspection accepts a valid CLDC class inventory', () => {
  const { inspectApiArchive } = require('../scripts/check-j2me-toolchain');
  const fs = require('node:fs');
  const os = require('node:os');
  const path = require('node:path');
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-api-'));
  const archive = path.join(dir, 'cldcapi11.jar');
  try {
    fs.writeFileSync(archive, Buffer.from([0x50, 0x4b, 0x03, 0x04, 0, 0]));
    const listing = [
      'java/lang/Object.class',
      'javax/microedition/io/Connection.class',
      'javax/microedition/io/Connector.class'
    ].join('\n');
    const runner = (_command, args) => ({
      status: args[0] === 'tf' ? 0 : 1,
      stdout: listing,
      stderr: ''
    });
    const result = inspectApiArchive(archive, [
      'java/lang/Object.class',
      'javax/microedition/io/Connection.class',
      'javax/microedition/io/Connector.class'
    ], 'jar', runner);
    assert.equal(result.ok, true);
    assert.match(result.detail, /contains/);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});


test('MIDP API inventory includes RMS persistence classes used by the client', () => {
  const { missingArchiveEntries } = require('../scripts/check-j2me-toolchain');
  const required = [
    'javax/microedition/rms/RecordStore.class',
    'javax/microedition/rms/RecordEnumeration.class'
  ];
  assert.deepEqual(missingArchiveEntries(required.join('\n'), required), []);
});
