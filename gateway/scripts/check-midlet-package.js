#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

function parseAttributes(text) {
  const result = {};
  let current = '';
  for (const line of String(text || '').replace(/\r\n/g, '\n').split('\n')) {
    if (line.startsWith(' ') && current) {
      result[current] += line.slice(1);
      continue;
    }
    const split = line.indexOf(':');
    if (split <= 0) {
      current = '';
      continue;
    }
    current = line.slice(0, split).trim();
    result[current] = line.slice(split + 1).trim();
  }
  return result;
}

function listJar(jarFile, jarCommand = process.env.JAR || 'jar', runner = spawnSync) {
  const result = runner(jarCommand, ['tf', jarFile], {
    encoding: 'utf8', timeout: 15000, maxBuffer: 8 * 1024 * 1024
  });
  if (result.error) throw new Error(`Unable to execute jar tool: ${result.error.message}`);
  if (result.status !== 0) throw new Error(`jar tf failed with exit ${result.status}`);
  return new Set(String(result.stdout || '').split(/\r?\n/).map(v => v.trim()).filter(Boolean));
}

function readManifest(jarFile, jarCommand = process.env.JAR || 'jar', runner = spawnSync) {
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'netormium-manifest-'));
  try {
    const result = runner(jarCommand, ['xf', path.resolve(jarFile), 'META-INF/MANIFEST.MF'], {
      cwd: temp, encoding: 'utf8', timeout: 15000, maxBuffer: 2 * 1024 * 1024
    });
    if (result.error) throw new Error(`Unable to extract JAR manifest: ${result.error.message}`);
    if (result.status !== 0) throw new Error(`Unable to extract JAR manifest (exit ${result.status})`);
    const manifest = path.join(temp, 'META-INF', 'MANIFEST.MF');
    if (!fs.existsSync(manifest)) throw new Error('JAR is missing META-INF/MANIFEST.MF');
    return parseAttributes(fs.readFileSync(manifest, 'utf8'));
  } finally {
    fs.rmSync(temp, { recursive: true, force: true });
  }
}

function parseJad(file) {
  return parseAttributes(fs.readFileSync(file, 'utf8'));
}

function validatePackage(jarFile, jadFile, options = {}) {
  const errors = [];
  const jarTool = options.jarCommand || process.env.JAR || 'jar';
  if (!fs.existsSync(jarFile)) return { ok: false, errors: [`JAR not found: ${jarFile}`] };
  if (!fs.existsSync(jadFile)) return { ok: false, errors: [`JAD not found: ${jadFile}`] };

  let entries;
  let manifest;
  let jad;
  try {
    entries = listJar(jarFile, jarTool, options.runner || spawnSync);
    manifest = readManifest(jarFile, jarTool, options.runner || spawnSync);
    jad = parseJad(jadFile);
  } catch (error) {
    return { ok: false, errors: [error.message] };
  }

  const expectedClass = 'com/netormium/client/NetormiumMIDlet.class';
  if (!entries.has(expectedClass)) errors.push(`JAR missing MIDlet entry class ${expectedClass}`);
  if (Array.from(entries).some(entry => entry.startsWith('javax/microedition/'))) {
    errors.push('JAR must not bundle Java ME platform API classes');
  }

  const requiredManifest = [
    'MIDlet-Name', 'MIDlet-Version', 'MIDlet-Vendor',
    'MIDlet-1', 'MIDlet-Permissions', 'MicroEdition-Profile', 'MicroEdition-Configuration'
  ];
  for (const key of requiredManifest) {
    if (!manifest[key]) errors.push(`Manifest missing ${key}`);
  }
  if (!manifest['MIDlet-Permissions'] || manifest['MIDlet-Permissions'].indexOf('javax.microedition.io.Connector.http') < 0) errors.push('Manifest must declare HTTP Connector permission');
  if (!jad['MIDlet-Permissions'] || jad['MIDlet-Permissions'].indexOf('javax.microedition.io.Connector.http') < 0) errors.push('JAD must declare HTTP Connector permission');
  if (manifest['MicroEdition-Profile'] !== 'MIDP-2.0') errors.push('Manifest profile must be MIDP-2.0');
  if (manifest['MicroEdition-Configuration'] !== 'CLDC-1.1') errors.push('Manifest configuration must be CLDC-1.1');

  const requiredJad = [
    'MIDlet-Name', 'MIDlet-Version', 'MIDlet-Vendor',
    'MIDlet-1', 'MIDlet-Permissions', 'MIDlet-Jar-URL', 'MIDlet-Jar-Size'
  ];
  for (const key of requiredJad) {
    if (!jad[key]) errors.push(`JAD missing ${key}`);
  }
  const size = fs.statSync(jarFile).size;
  if (Number(jad['MIDlet-Jar-Size']) !== size) {
    errors.push(`MIDlet-Jar-Size mismatch: JAD=${jad['MIDlet-Jar-Size']} actual=${size}`);
  }
  if (jad['MIDlet-Jar-URL'] && /[\\/\s]/.test(jad['MIDlet-Jar-URL'])) {
    errors.push('MIDlet-Jar-URL must be a simple relative filename for this package');
  }
  for (const key of ['MIDlet-Name', 'MIDlet-Version', 'MIDlet-Vendor', 'MIDlet-1']) {
    if (manifest[key] && jad[key] && manifest[key] !== jad[key]) {
      errors.push(`${key} mismatch between JAR manifest and JAD`);
    }
  }
  if (jad['MicroEdition-Profile'] && jad['MicroEdition-Profile'] !== 'MIDP-2.0') {
    errors.push('JAD profile must be MIDP-2.0');
  }
  if (jad['MicroEdition-Configuration'] && jad['MicroEdition-Configuration'] !== 'CLDC-1.1') {
    errors.push('JAD configuration must be CLDC-1.1');
  }
  return { ok: errors.length === 0, errors, jarBytes: size, entryCount: entries.size };
}

function main(argv = process.argv.slice(2)) {
  const jarFile = argv[0];
  const jadFile = argv[1];
  if (!jarFile || !jadFile || argv.includes('--help')) {
    console.log('Usage: node scripts/check-midlet-package.js PATH_TO_JAR PATH_TO_JAD');
    process.exitCode = argv.includes('--help') ? 0 : 2;
    return;
  }
  const result = validatePackage(path.resolve(jarFile), path.resolve(jadFile));
  for (const error of result.errors) console.error(`[FAIL] ${error}`);
  if (!result.ok) {
    console.error('MIDlet package validation: FAILED');
    process.exitCode = 1;
    return;
  }
  console.log(`[PASS] JAR entries: ${result.entryCount}`);
  console.log(`[PASS] MIDlet JAR size: ${result.jarBytes} bytes`);
  console.log('MIDlet package validation: PASS');
}

if (require.main === module) main();
module.exports = { parseAttributes, listJar, readManifest, parseJad, validatePackage, main };
