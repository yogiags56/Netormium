'use strict';

const fs = require('fs');
const path = require('path');

const gatewayDir = path.resolve(__dirname, '..');
const projectDir = path.resolve(gatewayDir, '..');
const pkg = JSON.parse(fs.readFileSync(path.join(gatewayDir, 'package.json'), 'utf8'));
const server = fs.readFileSync(path.join(gatewayDir, 'server.js'), 'utf8');
const build = fs.readFileSync(path.join(projectDir, 'build.xml'), 'utf8');
const client = fs.readFileSync(path.join(projectDir, 'src/com/netormium/client/net/InteractiveGateway.java'), 'utf8');
const snapshotClient = fs.readFileSync(path.join(projectDir, 'src/com/netormium/client/net/SnapshotLoader.java'), 'utf8');
const legacyClient = fs.readFileSync(path.join(projectDir, 'src/com/netormium/client/net/GatewayPageLoader.java'), 'utf8');
const jad = fs.readFileSync(path.join(projectDir, 'resources/Netormium.jad.example'), 'utf8');

const release = pkg.version;
const clientVersion = release.replace(/^1\.0\.0/, '1.0');
const checks = [
  ['Gateway banner', server.includes('Netormium Chromium Gateway ' + release)],
  ['Gateway runtime version derives from package metadata', server.includes("require('./package.json').version") && server.includes("'GATEWAY\\t' + GATEWAY_VERSION")],
  ['Gateway health response uses the package version', server.includes("'Netormium Gateway ' + GATEWAY_VERSION + ' OK\\n'")],
  ['Java ME User-Agent', client.includes('Netormium/' + clientVersion + ' (Java ME)')],
  ['JAD MIDlet version', /^MIDlet-Version:\s*1\.0\.0\s*$/m.test(jad)],
  ['JAD MIDP/CLDC profiles', jad.includes('MicroEdition-Profile: MIDP-2.0') &&
    jad.includes('MicroEdition-Configuration: CLDC-1.1')],
  ['Configurable Gateway URL property', client.includes('configureBaseUrl') &&
    snapshotClient.includes('configureBaseUrl') && legacyClient.includes('configureBaseUrl') &&
    jad.includes('Netormium-Gateway-URL:')],
  ['All Java ME clients use the exact release identity', client.includes('Netormium/' + clientVersion) &&
    snapshotClient.includes('Netormium/' + clientVersion) &&
    legacyClient.includes('Netormium/' + clientVersion)]
];

let failed = false;
checks.forEach(([name, ok]) => {
  process.stdout.write((ok ? 'PASS ' : 'FAIL ') + name + '\n');
  if (!ok) failed = true;
});
if (failed) process.exitCode = 1;
else process.stdout.write('Release consistency checks passed for ' + release + '.\n');
