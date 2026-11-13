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
const javaSource = javaFiles.join('\n');

const checks = [
  ['MIDP UI callbacks use Display.callSerially', canvas.includes('Display.getDisplay(midlet).callSerially')],
  ['tab restoration discards transient session IDs', canvas.includes('sessionId = "";') && canvas.includes('tabs.setCurrentSessionId("");') && canvas.includes('loadUrl(currentUrl, false)')],
  ['tab switching closes previous remote session', canvas.includes('private void switchTab(int index)') && canvas.includes('closeSession();')],
  ['new tab closes previous remote session', canvas.includes('private void createTab()') && canvas.includes('closeSession();')],
  ['MIDlet shutdown closes browser session', canvas.includes('public void shutdown()') && midlet.includes('browser.shutdown();')],
  ['RMS v2 no longer writes server session IDs', state.includes('VERSION = 2') && !state.includes('writeUTF(limit(tab.sessionId')],
  ['RMS v1 migration consumes and discards old session IDs', state.includes('if (version == 1) in.readUTF();')],
  ['worker results captured by UI callbacks are final',
    canvas.includes('final com.netormium.client.net.InteractiveGateway.Page result = interactive.type') &&
    canvas.includes('final com.netormium.client.net.InteractiveGateway.Page result = interactive.tap') &&
    canvas.includes('final com.netormium.client.net.InteractiveGateway.Page result = interactive.key')],
  ['snapshot callback payloads are final',
    canvas.includes('final byte[] png = interactive.snapshot') &&
    canvas.includes('final com.netormium.client.net.InteractiveGateway.Page fresh = interactive.inspect')],
  ['Java ME sources avoid post-Java-1.3 syntax',
    !javaSource.includes('StringBuilder') && !javaSource.includes('->') &&
    !javaSource.includes('<>') && !javaSource.includes('try (')]
];

let failed = false;
for (const [name, ok] of checks) {
  process.stdout.write((ok ? 'PASS ' : 'FAIL ') + name + '\n');
  if (!ok) failed = true;
}
if (failed) process.exitCode = 1;
else process.stdout.write('MIDlet lifecycle source checks passed.\n');
