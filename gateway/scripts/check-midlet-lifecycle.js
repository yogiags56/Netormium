'use strict';

const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

const canvas = read('src/com/netormium/client/ui/BrowserCanvas.java');
const state = read('src/com/netormium/client/util/BrowserStateStore.java');
const midlet = read('src/com/netormium/client/NetormiumMIDlet.java');

const checks = [
  ['MIDP UI callbacks use Display.callSerially', /private void postUi\([\s\S]*?Display\.getDisplay\(midlet\)\.callSerially/.test(canvas)],
  ['tab restoration discards transient session IDs', /private void restoreActiveTab\(\)[\s\S]*?sessionId = "";[\s\S]*?tabs\.setCurrentSessionId\(""\);[\s\S]*?loadUrl\(currentUrl, false\)/.test(canvas)],
  ['tab switching closes previous remote session', /private void switchTab\(int index\)[\s\S]*?closeSession\(\);/.test(canvas)],
  ['new tab closes previous remote session', /private void createTab\(\)[\s\S]*?closeSession\(\);/.test(canvas)],
  ['MIDlet shutdown closes browser session', /public void shutdown\(\)[\s\S]*?closeSession\(\);/.test(canvas) && /destroyApp\(boolean unconditional\)[\s\S]*?browser\.shutdown\(\)/.test(midlet)],
  ['RMS v2 no longer writes server session IDs', /VERSION = 2/.test(state) && !/writeUTF\(limit\(tab\.sessionId/.test(state)],
  ['RMS v1 migration consumes and discards old session IDs', /version == 1\) in\.readUTF\(\)/.test(state)]
];

let failed = false;
for (const [name, ok] of checks) {
  process.stdout.write((ok ? 'PASS ' : 'FAIL ') + name + '\n');
  if (!ok) failed = true;
}
if (failed) process.exitCode = 1;
else process.stdout.write('MIDlet lifecycle source checks passed.\n');
