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
const workerStart = canvas.indexOf('Thread worker = new Thread');
const workerEnd = canvas.indexOf('worker.start();', workerStart);
const loadWorker = workerStart >= 0 && workerEnd > workerStart ? canvas.substring(workerStart, workerEnd) : '';

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
    !javaSource.includes('<>') && !javaSource.includes('try (')],
  ['zero key resets and persists the remote Chromium viewport', canvas.includes('} else if (keyCode == KEY_NUM0) {\n            scrollY = 0;\n            tabs.setCurrentScroll(scrollY);\n            stateStore.save(tabs);\n            if (sessionId.length() > 0) refreshSessionSnapshot(requestToken, scrollY);')],
  ['bookmark duplicate detection completes enumeration cleanup before returning',
    read('src/com/netormium/client/util/BookmarkStore.java').includes('exists = url.equals(in.readUTF());') &&
    read('src/com/netormium/client/util/BookmarkStore.java').includes('if (exists) return true;') &&
    read('src/com/netormium/client/util/BookmarkStore.java').indexOf('if (exists) return true;') >
      read('src/com/netormium/client/util/BookmarkStore.java').indexOf('en.destroy();')],
  ['bookmark manager supports persistent rename and delete',
    read('src/com/netormium/client/util/BookmarkStore.java').includes('public boolean delete(String url)') &&
    read('src/com/netormium/client/util/BookmarkStore.java').includes('public boolean rename(String url, String title)') &&
    canvas.includes('renameBookmarkCommand') && canvas.includes('deleteBookmarkCommand') &&
    canvas.includes('bookmarks.rename(bookmarkUrls[selectedBookmark], title)') &&
    canvas.includes('bookmarks.delete(bookmarkUrls[selected])')],
  ['Speed Dial uses an independent persistent store with a bounded capacity',
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('STORE = "NetormiumSpeedDial"') &&
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('MAX_ITEMS = 12') &&
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('public boolean add(String title, String url)') &&
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('public boolean rename(String url, String title)') &&
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('public boolean delete(String url)')],
  ['Speed Dial manager is connected to home screen, menu, keypad, and touch',
    canvas.includes('private void showSpeedDial()') &&
    canvas.includes('private void addCurrentSpeedDial()') &&
    canvas.includes('else if (selected == 4) showSpeedDial();') &&
    canvas.includes('SpeedDialStore.Entry[] items = speedDial.all();') &&
    canvas.includes('loadUrl(items[selected - 1].url, true)') &&
    canvas.includes('loadUrl(items[dial].url, true)') &&
    canvas.includes('renameSpeedDialCommand') && canvas.includes('deleteSpeedDialCommand')],
  ['preferences persist font size and full-screen mode with backward-compatible Data Saver',
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('public int loadFontSize()') &&
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('public boolean loadFullScreen()') &&
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('private static final int VERSION = 4') &&
    canvas.includes('preferences.saveFontSize(fontSize)') &&
    canvas.includes('preferences.saveFullScreen(fullScreenEnabled)') &&
    canvas.includes('ThemeManager.setFontSize(fontSize)') &&
    canvas.includes('setFullScreenMode(fullScreenEnabled)')],
  ['Settings menu exposes functional text size and full-screen controls',
    canvas.includes('chooser.append("Text size: " + fontSizeLabel(), null)') &&
    canvas.includes('chooser.append(fullScreenEnabled ? "Full screen: On" : "Full screen: Off", null)') &&
    canvas.includes('else if (selected == 1) cycleFontSize();') &&
    canvas.includes('else if (selected == 2) toggleFullScreen();')],
  ['Speed Dial supports persistent adjacent reordering',
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('public boolean move(String url, int offset)') &&
    read('src/com/netormium/client/util/SpeedDialStore.java').includes('int target = index + offset;') &&
    canvas.includes('moveSpeedDialUpCommand') && canvas.includes('moveSpeedDialDownCommand') &&
    canvas.includes('speedDial.move(speedDialUrls[selected], direction)')],
  ['history manager supports deleting a single entry and persists the result',
    read('src/com/netormium/client/util/BrowserHistory.java').includes('public boolean removeAt(int position)') &&
    canvas.includes('deleteHistoryEntryCommand') &&
    canvas.includes('history.removeAt(historyIndex)') &&
    canvas.includes('historyStore.save(history);') &&
    canvas.includes('chooser.addCommand(deleteHistoryEntryCommand)')],
  ['history removal keeps the current index within valid bounds',
    read('src/com/netormium/client/util/BrowserHistory.java').includes('else if (position == index && index >= size) index = size - 1;')],
  ['gateway endpoint override is persisted and validated independently of JAD defaults',
    read('src/com/netormium/client/util/GatewaySettingsStore.java').includes('public boolean save(String value)') &&
    read('src/com/netormium/client/util/GatewaySettingsStore.java').includes("authority.indexOf('@') < 0") &&
    canvas.includes('gatewaySettings.save(value)') &&
    canvas.includes('gatewaySettings.load()') &&
    canvas.includes('gatewaySettings.clear()')],
  ['gateway endpoint changes are applied to all browser transports',
    canvas.includes('private void configureGateway(String value)') &&
    canvas.includes('interactive.configureBaseUrl(value)') &&
    canvas.includes('snapshotLoader.configureBaseUrl(value)') &&
    canvas.includes('loader.configureBaseUrl(value)') &&
    canvas.includes('showGatewaySettings()') &&
    canvas.includes('chooser.append("Gateway address", null)')],
  ['page preferences persist Load Images and Mobile View in settings schema v3',
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('VERSION = 4') &&
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('public boolean loadImages()') &&
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('public boolean loadMobileView()') &&
    canvas.includes('preferences.saveLoadImages(loadImagesEnabled)') &&
    canvas.includes('preferences.saveMobileView(mobileViewEnabled)')],
  ['page rendering preferences are passed through NTM session-open parameters',
    read('src/com/netormium/client/net/InteractiveGateway.java').includes('&img=') &&
    read('src/com/netormium/client/net/InteractiveGateway.java').includes('&mv=') &&
    canvas.includes('interactive.open(url, viewportWidth, viewportHeight, useDataSaver, useImages, useMobileView)')],
  ['startup preference persists restore-last-page behavior with schema v4 migration',
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('VERSION = 4') &&
    read('src/com/netormium/client/util/BrowserPreferences.java').includes('public boolean loadRestoreLastPage()') &&
    canvas.includes('preferences.saveRestoreLastPage(restoreLastPageEnabled)') &&
    canvas.includes('currentUrl.length() > 0 && restoreLastPageEnabled')],
  ['Settings exposes startup behavior and shifts gateway diagnostics actions',
    canvas.includes('Restore Last Page: On') &&
    canvas.includes('toggleRestoreLastPage()') &&
    canvas.includes('else if (selected == 6) showGatewaySettings();') &&
    canvas.includes('else if (selected == 7) showDiagnostics();')],
  ['Start Page renders classic compact address bar and aligns Speed Dial hit testing',
    canvas.includes('Search or enter address') &&
    canvas.includes('g.fillRect(5, searchY, width - 10, searchHeight)') &&
    canvas.includes('if (dial == -2) { showAddressBox(); return; }') &&
    canvas.includes('int gridY = labelY + body.getHeight() + 4;')],
  ['Start Page renders compact Top Sites tiles and keypad selection',
    canvas.includes('g.drawString("TOP SITES"') &&
    canvas.includes('int columns = width >= 220 ? 3 : 2;') &&
    canvas.includes('int tileHeight = width >= 220 ? 51 : 43;') &&
    canvas.includes('private int startPageSelection = 0;') &&
    canvas.includes('if (action == LEFT && startPageSelection > 0)') &&
    canvas.includes('if (i == startPageSelection)')],
  ['interactive session mutations are serialized',
    canvas.includes('private boolean interactionBusy = false;') &&
    (canvas.match(/interactionBusy = true;/g) || []).length === 3 &&
    (canvas.match(/interactionBusy = false;/g) || []).length >= 5],
  ['navigation worker uses viewport captured on UI thread',
    canvas.includes('final int viewportWidth =') && canvas.includes('final int viewportHeight =') &&
    loadWorker.length > 0 && !loadWorker.includes('getWidth()') && !loadWorker.includes('getHeight()')],
  ['interactive transport rejects oversized declared response bodies',
    read('src/com/netormium/client/net/InteractiveGateway.java').includes('long declaredLength = connection.getLength();') &&
    read('src/com/netormium/client/net/InteractiveGateway.java').includes('if (declaredLength > maxBytes)') &&
    read('src/com/netormium/client/net/InteractiveGateway.java').includes('new ByteArrayOutputStream(4096)')],
  ['interactive transport guarantees progress on zero-byte reads',
    read('src/com/netormium/client/net/InteractiveGateway.java').includes('int single = input.read();')],
  ['PNG snapshot transport enforces declared and streamed byte limits',
    read('src/com/netormium/client/net/SnapshotLoader.java').includes('if (declaredLength > MAX_IMAGE_BYTES)') &&
    read('src/com/netormium/client/net/SnapshotLoader.java').includes('if (total > MAX_IMAGE_BYTES)') &&
    read('src/com/netormium/client/net/SnapshotLoader.java').includes('new ByteArrayOutputStream(4096)')],
  ['legacy page transport enforces declared and streamed byte limits',
    read('src/com/netormium/client/net/GatewayPageLoader.java').includes('if (declaredLength > MAX_RESPONSE_BYTES)') &&
    read('src/com/netormium/client/net/GatewayPageLoader.java').includes('if (total > MAX_RESPONSE_BYTES)') &&
    read('src/com/netormium/client/net/GatewayPageLoader.java').includes('new ByteArrayOutputStream(4096)')]
];

let failed = false;
for (const [name, ok] of checks) {
  process.stdout.write((ok ? 'PASS ' : 'FAIL ') + name + '\n');
  if (!ok) failed = true;
}
if (failed) process.exitCode = 1;
else process.stdout.write('MIDlet lifecycle source checks passed.\n');
