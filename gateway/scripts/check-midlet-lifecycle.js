'use strict';

const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

const canvas = read('src/com/netormium/client/ui/BrowserCanvas.java');
const state = read('src/com/netormium/client/util/BrowserStateStore.java');
const midlet = read('src/com/netormium/client/NetormiumMIDlet.java');
const javaFiles = [];
function collectJava(directory) {
  fs.readdirSync(path.join(root, directory), { withFileTypes: true }).forEach(entry => {
    const relative = path.join(directory, entry.name);
    if (entry.isDirectory()) collectJava(relative);
    else if (entry.isFile() && entry.name.endsWith('.java')) javaFiles.push(read(relative));
  });
}
collectJava('src');
const javaSource = javaFiles.join('\\n');

const checks = [
  ['MIDP UI callbacks use Display.callSerially', /private void postUi\([\s\S]*?Display\.getDisplay\(midlet\)\.callSerially/.test(canvas)],
  ['tab restoration discards transient session IDs', /private void restoreActiveTab\(\)[\s\S]*?sessionId = "";[\s\S]*?tabs\.setCurrentSessionId\(""\);[\s\S]*?loadUrl\(currentUrl, false\)/.test(canvas)],
  ['tab switching closes previous remote session', /private void switchTab\(int index\)[\s\S]*?closeSession\(\);/.test(canvas)],
  ['new tab closes previous remote session', /private void createTab\(\)[\s\S]*?closeSession\(\);/.test(canvas)],
  ['MIDlet shutdown closes browser session', /public void shutdown\(\)[\s\S]*?closeSession\(\);/.test(canvas) && /destroyApp\(boolean unconditional\)[\s\S]*?browser\.shutdown\(\)/.test(midlet)],
  ['RMS v2 no longer writes server session IDs', /VERSION = 2/.test(state) && !/writeUTF\(limit\(tab\.sessionId/.test(state)],
  ['RMS v1 migration consumes and discards old session IDs', /version == 1\\) in\\.readUTF\\(\\)/.test(state)],
  ['worker results captured by UI callbacks are final', /final com\\.netormium\\.client\\.net\\.InteractiveGateway\\.Page result = interactive\\.type/.test(canvas) && /final com\\.netormium\\.client\\.net\\.InteractiveGateway\\.Page result = interactive\\.tap/.test(canvas) && /final com\\.netormium\\.client\\.net\\.InteractiveGateway\\.Page result = interactive\\.key/.test(canvas)],
  ['snapshot callback payloads are final', /final byte\\[\\] png = interactive\\.snapshot/.test(canvas) && /final com\\.netormium\\.client\\.net\\.InteractiveGateway\\.Page fresh = interactive\\.inspect/.test(canvas)],
  ['Java ME sources avoid post-Java-1.3 syntax', !/new\\s+[A-Za-z0-9_.<>]+\\s*<>\\s*\\(|\\bStringBuilder\\b|try\\s*\\([^)]*\\)|->/.test(javaSource) && !/for\\s*\\(\\s*[^;:]+:\\s*[^)]+\\)/.test(javaSource)]
];

let failed = false;
for (const [name, ok] of checks) {
  process.stdout.write((ok ? 'PASS ' : 'FAIL ') + name + '\n');
  if (!ok) failed = true;
}
if (failed) process.exitCode = 1;
else process.stdout.write('MIDlet lifecycle source checks passed.\n');
