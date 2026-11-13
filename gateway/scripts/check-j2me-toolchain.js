'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

function parseArgs(argv) {
  const result = { propertyFile: 'build.properties' };
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === '--property-file' && argv[i + 1]) result.propertyFile = argv[++i];
  }
  return result;
}
function readProperties(file) {
  if (!fs.existsSync(file)) return {};
  const values = {};
  for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#') || line.startsWith('!')) continue;
    const split = line.search(/(?<!\\)[=:]/);
    if (split < 0) continue;
    values[line.slice(0, split).trim()] = line.slice(split + 1).trim().replace(/\\([:= ])/g, '$1');
  }
  return values;
}
function resolveConfiguredPath(value, propertyFile) {
  if (!value) return '';
  return path.isAbsolute(value) ? value : path.resolve(path.dirname(path.resolve(propertyFile)), value);
}
function isZipArchive(file) {
  try {
    const fd = fs.openSync(file, 'r');
    const header = Buffer.alloc(4);
    const bytes = fs.readSync(fd, header, 0, 4, 0);
    fs.closeSync(fd);
    return bytes === 4 && header[0] === 0x50 && header[1] === 0x4b &&
      ((header[2] === 0x03 && header[3] === 0x04) ||
       (header[2] === 0x05 && header[3] === 0x06) ||
       (header[2] === 0x07 && header[3] === 0x08));
  } catch (_) { return false; }
}
function isExecutableFile(file, platform = process.platform) {
  try {
    const stat = fs.statSync(file);
    return stat.isFile() && (platform === 'win32' || (stat.mode & 0o111) !== 0);
  } catch (_) { return false; }
}
function missingArchiveEntries(output, requiredEntries) {
  const available = new Set(String(output || '').split(/\r?\n/).map(line => line.trim()).filter(Boolean));
  return requiredEntries.filter(entry => !available.has(entry));
}
function inspectApiArchive(file, requiredEntries, jarCommand = process.env.JAR || 'jar', runner = spawnSync) {
  if (!isZipArchive(file)) {
    return { ok: false, detail: file ? `missing or not a ZIP/JAR: ${file}` : 'archive path is not configured' };
  }
  const result = runner(jarCommand, ['tf', file], {
    encoding: 'utf8', timeout: 15000, maxBuffer: 8 * 1024 * 1024
  });
  if (result.error || result.status !== 0) {
    return { ok: false, detail: result.error ? result.error.message : `jar tf exited ${result.status}` };
  }
  const missing = missingArchiveEntries(result.stdout, requiredEntries);
  return missing.length
    ? { ok: false, detail: `missing required classes: ${missing.join(', ')}` }
    : { ok: true, detail: `${file} contains ${requiredEntries.join(', ')}` };
}
function commandVersion(command, args) {
  if (!command) return { ok: false, output: 'not configured' };
  const result = spawnSync(command, args, { encoding: 'utf8', timeout: 10000 });
  if (result.error) return { ok: false, output: result.error.message };
  return { ok: result.status === 0, output: `${result.stdout || ''}${result.stderr || ''}`.trim().split(/\r?\n/)[0] || `exit ${result.status}` };
}
const ECJ_MAIN_CLASS = 'org.eclipse.jdt.internal.compiler.batch.Main';

function ecjProbeArguments(jarPath) {
  return ['-cp', jarPath, ECJ_MAIN_CLASS, '-help'];
}
function supportsEcjLegacyMode(exitCode, output) {
  return exitCode === 0 && /(?:^|\s)-1\.3(?:\s|$)/m.test(output || '');
}
function report(label, ok, detail) {
  console.log(`${ok ? '[PASS]' : '[MISSING]'} ${label}: ${detail}`);
  return ok;
}
function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  const propertyFile = path.resolve(args.propertyFile);
  const properties = readProperties(propertyFile);
  console.log('Netormium Java ME toolchain validation');
  console.log(`Property file: ${propertyFile} (${fs.existsSync(propertyFile) ? 'found' : 'not found; using environment/defaults'})`);
  let passed = 0;
  let required = 0;
  function check(label, ok, detail) {
    required += 1;
    if (report(label, ok, detail)) passed += 1;
  }
  const java = commandVersion(process.env.JAVA || 'java', ['-version']);
  check('Java runtime', java.ok, java.output);
  const javacCommand = process.env.JAVAC || 'javac';
  const javac = commandVersion(javacCommand, ['-version']);
  check('Java compiler', javac.ok, javac.output);
  const ant = commandVersion(process.env.ANT || 'ant', ['-version']);
  check('Apache Ant', ant.ok, ant.output);
  const cldcRaw = process.env.CLDC_API_JAR || properties['cldc.api.jar'] || '';
  const midpRaw = process.env.MIDP_API_JAR || properties['midp.api.jar'] || '';
  const preverifyRaw = process.env.PREVERIFY || properties['preverify.executable'] || '';
  const cldc = resolveConfiguredPath(cldcRaw, propertyFile);
  const midp = resolveConfiguredPath(midpRaw, propertyFile);
  const preverify = resolveConfiguredPath(preverifyRaw, propertyFile);
  const cldcInventory = inspectApiArchive(cldc, [
    'java/lang/Object.class',
    'javax/microedition/io/Connection.class',
    'javax/microedition/io/Connector.class'
  ]);
  const midpInventory = inspectApiArchive(midp, [
    'javax/microedition/midlet/MIDlet.class',
    'javax/microedition/lcdui/Display.class',
    'javax/microedition/lcdui/Canvas.class',
    'javax/microedition/rms/RecordStore.class',
    'javax/microedition/rms/RecordEnumeration.class'
  ]);
  check('CLDC 1.1 API contents', cldcInventory.ok, cldcInventory.detail);
  check('MIDP 2.0 API contents', midpInventory.ok, midpInventory.detail);
  const executable = isExecutableFile(preverify);
  check('Java ME preverifier executable', executable, preverify ? (executable ? preverify : `missing, not a regular file, or not executable: ${preverify}`) : 'set preverify.executable or PREVERIFY');
  const backend = process.env.NETORMIUM_COMPILER || properties['compiler.backend'] || 'javac';
  if (backend === 'ecj') {
    const ecjRaw = process.env.ECJ_JAR || properties['ecj.jar'] || '';
    const ecj = resolveConfiguredPath(ecjRaw, propertyFile);
    const ecjOk = isZipArchive(ecj);
    check('Eclipse Compiler JAR', ecjOk, ecj ? (ecjOk ? ecj : `missing or not a ZIP/JAR: ${ecj}`) : 'set ecj.jar or ECJ_JAR');
    let ecjHelp = { status: 1, stdout: '', stderr: '' };
    if (ecjOk) ecjHelp = spawnSync(process.env.JAVA || 'java', ecjProbeArguments(ecj), { encoding: 'utf8', timeout: 15000 });
    const ecjText = `${ecjHelp.stdout || ''}${ecjHelp.stderr || ''}`;
    check('ECJ legacy mode', ecjOk && supportsEcjLegacyMode(ecjHelp.status, ecjText),
      ecjOk ? (ecjHelp.status === 0 ? (supportsEcjLegacyMode(ecjHelp.status, ecjText) ? 'ECJ advertises -1.3 compatibility mode' : 'ECJ help does not advertise -1.3 mode') : 'ECJ batch compiler could not be launched') : 'blocked until a valid ECJ JAR is supplied');
  } else if (backend === 'javac') {
    const compilerHelp = spawnSync(javacCommand, ['-help'], { encoding: 'utf8', timeout: 10000 });
    const helpText = `${compilerHelp.stdout || ''}${compilerHelp.stderr || ''}`;
    const source = process.env.NETORMIUM_JAVAC_SOURCE || properties['javac.source'] || '1.3';
    const target = process.env.NETORMIUM_JAVAC_TARGET || properties['javac.target'] || '1.1';
    const supportsSource = helpText.includes(source);
    const supportsTarget = helpText.includes(target);
    check('Legacy javac source level', supportsSource, supportsSource ? `compiler advertises source ${source}` : `${javacCommand} does not advertise configured source ${source}`);
    check('Legacy javac target level', supportsTarget, supportsTarget ? `compiler advertises target ${target}` : `${javacCommand} does not advertise configured target ${target}`);
  } else {
    check('Compiler backend', false, `unsupported compiler.backend=${backend}; use javac or ecj`);
  }
  console.log(`\nRequired checks: ${passed}/${required} passed.`);
  if (passed !== required) {
    console.log('Result: NOT READY — no Java ME build should be considered successful until all required checks pass.');
    process.exitCode = 1;
  } else {
    console.log('Result: TOOLCHAIN CANDIDATE — perform a real compile, preverify and device test before release.');
    process.exitCode = 0;
  }
}
if (require.main === module) main();
module.exports = {
  parseArgs, readProperties, resolveConfiguredPath, isZipArchive, isExecutableFile,
  missingArchiveEntries, inspectApiArchive, ECJ_MAIN_CLASS, ecjProbeArguments,
  supportsEcjLegacyMode, main
};
