package com.netormium.client.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import com.netormium.client.NetormiumMIDlet;
import com.netormium.client.net.GatewayPageLoader;
import com.netormium.client.util.BrowserHistory;
import com.netormium.client.util.UrlUtil;
import com.netormium.client.util.TabManager;
import com.netormium.client.util.BookmarkStore;
import com.netormium.client.util.SpeedDialStore;
import com.netormium.client.util.BrowserStateStore;
import com.netormium.client.util.HistoryStore;
import com.netormium.client.util.BrowserPreferences;
import com.netormium.client.util.GatewaySettingsStore;

/** Netormium RC21 browser canvas with keypad navigation and remote Chromium snapshots. */
public final class BrowserCanvas extends Canvas implements CommandListener {
    private final NetormiumMIDlet midlet;
    private final BrowserHistory history = new BrowserHistory(100);
    private final BrowserStateStore stateStore = new BrowserStateStore();
    private final HistoryStore historyStore = new HistoryStore();
    private final BrowserPreferences preferences = new BrowserPreferences();
    private final GatewaySettingsStore gatewaySettings = new GatewaySettingsStore();
    private TextBox gatewayBox;
    private String jadGatewayUrl = "";
    private final TabManager tabs = new TabManager();
    private final BookmarkStore bookmarks = new BookmarkStore();
    private final SpeedDialStore speedDial = new SpeedDialStore();
    private String[] speedDialUrls = new String[0];
    private int selectedSpeedDial = -1;
    private int startPageSelection = 0;
    private boolean mainMenuOpen = false;
    private int mainMenuSelection = 0;
    private int mainMenuScroll = 0;
    private final Command renameSpeedDialCommand = new Command("Rename", Command.SCREEN, 1);
    private final Command deleteSpeedDialCommand = new Command("Delete", Command.SCREEN, 2);
    private final Command moveSpeedDialUpCommand = new Command("Move Up", Command.SCREEN, 3);
    private final Command moveSpeedDialDownCommand = new Command("Move Down", Command.SCREEN, 4);
    private String[] bookmarkUrls = new String[0];
    private TextBox bookmarkRenameBox;
    private TextBox speedDialRenameBox;
    private int selectedBookmark = -1;
    private final Command renameBookmarkCommand = new Command("Rename", Command.SCREEN, 1);
    private final Command deleteBookmarkCommand = new Command("Delete", Command.SCREEN, 2);
    private List chooser;
    private final GatewayPageLoader loader = new GatewayPageLoader();
    private final com.netormium.client.net.InteractiveGateway interactive =
        new com.netormium.client.net.InteractiveGateway();
    private String sessionId = "";
    private com.netormium.client.net.InteractiveGateway.Element[] interactiveElements =
        new com.netormium.client.net.InteractiveGateway.Element[0];
    private boolean inputFocused = false;
    private boolean interactionBusy = false;
    private int selectedElement = -1;
    private TextBox inputBox;
    private final com.netormium.client.net.SnapshotLoader snapshotLoader = new com.netormium.client.net.SnapshotLoader();
    private Image pageImage;
    private GatewayPageLoader.Link[] pageLinks = new GatewayPageLoader.Link[0];

    private final Command openCommand = new Command("Open", Command.SCREEN, 1);
    private final Command menuCommand = new Command("Menu", Command.SCREEN, 2);
    private final Command homeCommand = new Command("Home", Command.SCREEN, 2);
    private final Command backCommand = new Command("Back", Command.BACK, 1);
    private final Command forwardCommand = new Command("Forward", Command.SCREEN, 3);
    private final Command reloadCommand = new Command("Reload", Command.SCREEN, 4);
    private final Command typeCommand = new Command("Type", Command.SCREEN, 5);
    private final Command enterCommand = new Command("Enter", Command.SCREEN, 6);
    private final Command dataSaverCommand = new Command("Toggle Data Saver", Command.SCREEN, 7);
    private boolean dataSaverEnabled = true;
    private int fontSize = 1;
    private boolean fullScreenEnabled = false;
    private boolean loadImagesEnabled = true;
    private boolean mobileViewEnabled = false;
    private boolean restoreLastPageEnabled = false;
    private final Command newTabCommand = new Command("New Tab", Command.SCREEN, 8);
    private final Command tabsCommand = new Command("Tabs", Command.SCREEN, 9);
    private final Command nextTabCommand = new Command("Next Tab", Command.SCREEN, 10);
    private final Command addBookmarkCommand = new Command("Add Bookmark", Command.SCREEN, 11);
    private final Command bookmarksCommand = new Command("Bookmarks", Command.SCREEN, 12);
    private final Command historyCommand = new Command("History", Command.SCREEN, 13);
    private final Command clearHistoryCommand = new Command("Clear History", Command.SCREEN, 14);
    private final Command deleteHistoryEntryCommand = new Command("Delete Entry", Command.SCREEN, 15);
    private final Command stopCommand = new Command("Stop", Command.STOP, 15);
    private final Command exitCommand = new Command("Exit", Command.EXIT, 16);
    private final Command diagnosticsCommand = new Command("Diagnostics", Command.SCREEN, 17);

    private String currentUrl = "";
    private String pageTitle = "Netormium";
    private String pageText = "Welcome to Netormium.\n\nChoose Open to enter a website address.\n\nWeb pages are rendered by the remote Chromium Gateway. Configure Netormium-Gateway-URL in the JAD to start browsing.";
    private String status = "Ready";
    private int scrollY = 0;
    private boolean loading = false;
    private int requestToken = 0;
    private TextBox addressBox;

    public BrowserCanvas(NetormiumMIDlet midlet) {
        this.midlet = midlet;
        String gatewayUrl = midlet.getAppProperty("Netormium-Gateway-URL");
        jadGatewayUrl = gatewayUrl == null ? "" : gatewayUrl;
        String savedGateway = gatewaySettings.load();
        if (savedGateway.length() > 0) gatewayUrl = savedGateway;
        interactive.configureBaseUrl(gatewayUrl);
        snapshotLoader.configureBaseUrl(gatewayUrl);
        loader.configureBaseUrl(gatewayUrl);
        setFullScreenMode(false);
        dataSaverEnabled = preferences.loadDataSaver(true);
        fontSize = preferences.loadFontSize();
        fullScreenEnabled = preferences.loadFullScreen();
        loadImagesEnabled = preferences.loadImages();
        mobileViewEnabled = preferences.loadMobileView();
        restoreLastPageEnabled = preferences.loadRestoreLastPage();
        ThemeManager.setFontSize(fontSize);
        setFullScreenMode(fullScreenEnabled);
        addCommand(openCommand);
        addCommand(menuCommand);
        setCommandListener(this);
        stateStore.load(tabs);
        historyStore.load(history);
        restoreActiveTab();
    }

    public void commandAction(Command command, javax.microedition.lcdui.Displayable displayable) {
        if (displayable == gatewayBox) {
            if (command.getCommandType() == Command.OK) {
                String value = gatewayBox.getString().trim();
                if (value.length() == 0) {
                    gatewaySettings.clear();
                    configureGateway(jadGatewayUrl);
                    status = "Using default JAD gateway";
                } else if (gatewaySettings.save(value)) {
                    configureGateway(value);
                    status = "Gateway address saved";
                } else status = "Invalid gateway URL";
                repaint();
                showSettings();
            } else Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (displayable == addressBox) {
            if (command.getCommandType() == Command.OK) {
                String url = UrlUtil.normalize(addressBox.getString());
                if (url == null) {
                    status = "Only HTTP/HTTPS URLs supported";
                    repaint();
                    Display.getDisplay(midlet).setCurrent(this);
                } else loadUrl(url, true);
            } else Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (displayable == speedDialRenameBox) {
            if (command.getCommandType() == Command.OK && selectedSpeedDial >= 0 &&
                selectedSpeedDial < speedDialUrls.length) {
                status = speedDial.rename(speedDialUrls[selectedSpeedDial], speedDialRenameBox.getString())
                    ? "Speed Dial renamed" : "Rename failed";
                showSpeedDial();
            } else Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (displayable == bookmarkRenameBox) {
            if (command.getCommandType() == Command.OK && selectedBookmark >= 0 &&
                selectedBookmark < bookmarkUrls.length) {
                String title = bookmarkRenameBox.getString();
                status = bookmarks.rename(bookmarkUrls[selectedBookmark], title)
                    ? "Bookmark renamed" : "Rename failed";
                showBookmarks();
            } else Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (displayable == inputBox) {
            if (command.getCommandType() == Command.OK) sendTextToPage(inputBox.getString());
            else Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (displayable == chooser) {
            if (command == renameSpeedDialCommand && "Speed Dial".equals(chooser.getTitle())) {
                selectedSpeedDial = chooser.getSelectedIndex();
                if (selectedSpeedDial >= 0 && selectedSpeedDial < speedDialUrls.length) {
                    SpeedDialStore.Entry[] entries = speedDial.all();
                    speedDialRenameBox = new TextBox("Rename Speed Dial", entries[selectedSpeedDial].title, 80, TextField.ANY);
                    speedDialRenameBox.addCommand(new Command("Save", Command.OK, 1));
                    speedDialRenameBox.addCommand(new Command("Cancel", Command.CANCEL, 2));
                    speedDialRenameBox.setCommandListener(this);
                    Display.getDisplay(midlet).setCurrent(speedDialRenameBox);
                }
                return;
            }
            if (command == deleteSpeedDialCommand && "Speed Dial".equals(chooser.getTitle())) {
                int selected = chooser.getSelectedIndex();
                if (selected >= 0 && selected < speedDialUrls.length) {
                    status = speedDial.delete(speedDialUrls[selected])
                        ? "Speed Dial entry deleted" : "Delete failed";
                    showSpeedDial();
                }
                return;
            }
            if ((command == moveSpeedDialUpCommand || command == moveSpeedDialDownCommand) &&
                "Speed Dial".equals(chooser.getTitle())) {
                int selected = chooser.getSelectedIndex();
                if (selected >= 0 && selected < speedDialUrls.length) {
                    int direction = command == moveSpeedDialUpCommand ? -1 : 1;
                    status = speedDial.move(speedDialUrls[selected], direction)
                        ? "Speed Dial order updated" : "Already at list boundary";
                    showSpeedDial();
                }
                return;
            }
            if (command == renameBookmarkCommand && "Bookmarks".equals(chooser.getTitle())) {
                selectedBookmark = chooser.getSelectedIndex();
                if (selectedBookmark >= 0 && selectedBookmark < bookmarkUrls.length) {
                    BookmarkStore.Bookmark[] items = bookmarks.all();
                    String title = selectedBookmark < items.length ? items[selectedBookmark].title : "";
                    bookmarkRenameBox = new TextBox("Rename bookmark", title, 80, TextField.ANY);
                    bookmarkRenameBox.addCommand(new Command("Save", Command.OK, 1));
                    bookmarkRenameBox.addCommand(new Command("Cancel", Command.CANCEL, 2));
                    bookmarkRenameBox.setCommandListener(this);
                    Display.getDisplay(midlet).setCurrent(bookmarkRenameBox);
                }
                return;
            }
            if (command == deleteBookmarkCommand && "Bookmarks".equals(chooser.getTitle())) {
                int selected = chooser.getSelectedIndex();
                if (selected >= 0 && selected < bookmarkUrls.length) {
                    status = bookmarks.delete(bookmarkUrls[selected])
                        ? "Bookmark deleted" : "Delete failed";
                    showBookmarks();
                }
                return;
            }
            if (command == clearHistoryCommand && "History".equals(chooser.getTitle())) {
                history.clear();
                historyStore.save(history);
                status = "History cleared";
                showHistory();
                return;
            }
            if (command == deleteHistoryEntryCommand && "History".equals(chooser.getTitle())) {
                int selected = chooser.getSelectedIndex();
                String[] entries = history.entries();
                int historyIndex = entries.length - 1 - selected;
                if (historyIndex >= 0 && historyIndex < entries.length) {
                    status = history.removeAt(historyIndex) ? "History entry deleted" : "Delete failed";
                    historyStore.save(history);
                    showHistory();
                }
                return;
            }
            if (command == List.SELECT_COMMAND) {
                int selected = chooser.getSelectedIndex();
                if (selected >= 0) {
                    String title = chooser.getTitle();
                    if ("Netormium Menu".equals(title)) {
                        handleMainMenuSelection(selected);
                        return;
                    } else if ("Settings".equals(title)) {
                        if (selected == 0) toggleDataSaver();
                        else if (selected == 1) cycleFontSize();
                        else if (selected == 2) toggleFullScreen();
                        else if (selected == 3) toggleLoadImages();
                        else if (selected == 4) toggleMobileView();
                        else if (selected == 5) toggleRestoreLastPage();
                        else if (selected == 6) showGatewaySettings();
                        else if (selected == 7) showDiagnostics();
                        else showAbout();
                        return;
                    } else if ("Speed Dial".equals(title) && selected < speedDialUrls.length)
                        loadUrl(speedDialUrls[selected], true);
                    else if ("Open Tab".equals(title)) switchTab(selected);
                    else if ("Bookmarks".equals(title) && selected < bookmarkUrls.length)
                        loadUrl(bookmarkUrls[selected], true);
                    else if ("History".equals(title)) {
                        String[] entries = history.entries();
                        int hi = entries.length - 1 - selected;
                        if (hi >= 0 && hi < entries.length) loadUrl(entries[hi], false);
                    }
                }
            }
            Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (command == openCommand) showAddressBox();
        else if (command == menuCommand) showMainMenu();
        else if (command == homeCommand) showHome();
        else if (command == backCommand) navigateHistory(true);
        else if (command == forwardCommand) navigateHistory(false);
        else if (command == reloadCommand) { if (currentUrl.length() > 0) loadUrl(currentUrl, false); }
        else if (command == exitCommand) midlet.exit();
        else if (command == diagnosticsCommand) showDiagnostics();
        else if (command == typeCommand) showInputBox();
        else if (command == enterCommand) sendBrowserKey("Enter");
        else if (command == dataSaverCommand) toggleDataSaver();
        else if (command == newTabCommand) createTab();
        else if (command == tabsCommand) showTabs();
        else if (command == nextTabCommand) switchTab((tabs.activeIndex() + 1) % tabs.size());
        else if (command == addBookmarkCommand) addCurrentBookmark();
        else if (command == bookmarksCommand) showBookmarks();
        else if (command == historyCommand) showHistory();
        else if (command == stopCommand && loading) {
            stopLoading();
            status = "Loading stopped";
            pageText = "Navigation cancelled by user.";
            repaint();
        }
    }

    private String[] mainMenuItems() {
        String[] items = new String[17];
        items[0] = "Open address";
        items[1] = "Start page";
        items[2] = "Tabs";
        items[3] = "New tab";
        items[4] = "Speed Dial";
        items[5] = "Add to Speed Dial";
        items[6] = "Bookmarks";
        items[7] = "Add bookmark";
        items[8] = "History";
        items[9] = "Reload page";
        items[10] = "Forward";
        items[11] = dataSaverEnabled ? "Data Saver: On" : "Data Saver: Off";
        items[12] = "Settings";
        items[13] = "Page information";
        items[14] = "Diagnostics";
        items[15] = "About Netormium";
        items[16] = "Exit";
        return items;
    }

    private void showMainMenu() {
        mainMenuOpen = true;
        mainMenuSelection = 0;
        mainMenuScroll = 0;
        Display.getDisplay(midlet).setCurrent(this);
        repaint();
    }

    private void closeMainMenu() {
        mainMenuOpen = false;
        repaint();
    }

    private void paintMainMenu(Graphics g, int width, int height, Font body) {
        int headerHeight = Math.max(22, body.getHeight() + 10);
        int footerHeight = Math.max(18, body.getHeight() + 7);
        int x = 3;
        int panelWidth = width - 6;
        int top = 2;
        int bottom = height - footerHeight - 2;
        g.setColor(0xFFFFFF);
        g.fillRect(x, top, panelWidth, bottom - top);
        g.setColor(0x526F89);
        g.drawRect(x, top, panelWidth - 1, bottom - top - 1);
        g.setColor(0x244D70);
        g.fillRect(x + 1, top + 1, panelWidth - 2, headerHeight);
        g.setFont(body);
        g.setColor(0xFFFFFF);
        g.drawString("Netormium", x + 6, top + headerHeight / 2,
            Graphics.LEFT | Graphics.VCENTER);
        String[] items = mainMenuItems();
        int rowHeight = Math.max(19, body.getHeight() + 5);
        int listTop = top + headerHeight + 2;
        int listBottom = bottom - 2;
        int visibleRows = Math.max(1, (listBottom - listTop) / rowHeight);
        if (mainMenuSelection < mainMenuScroll) mainMenuScroll = mainMenuSelection;
        if (mainMenuSelection >= mainMenuScroll + visibleRows) {
            mainMenuScroll = mainMenuSelection - visibleRows + 1;
        }
        for (int row = 0; row < visibleRows; row++) {
            int index = mainMenuScroll + row;
            if (index >= items.length) break;
            int y = listTop + row * rowHeight;
            if (index == mainMenuSelection) {
                g.setColor(0x2879B9);
                g.fillRect(x + 2, y, panelWidth - 4, rowHeight);
                g.setColor(0xFFFFFF);
            } else {
                g.setColor(0x263B4D);
            }
            g.drawString(items[index], x + 8, y + rowHeight / 2,
                Graphics.LEFT | Graphics.VCENTER);
            if (index == 11) {
                g.drawString(dataSaverEnabled ? "ON" : "OFF",
                    width - 10, y + rowHeight / 2, Graphics.RIGHT | Graphics.VCENTER);
            }
        }
        g.setColor(0x244D70);
        g.fillRect(0, height - footerHeight, width, footerHeight);
        g.setColor(0xFFFFFF);
        g.drawString("Select", 5, height - footerHeight / 2,
            Graphics.LEFT | Graphics.VCENTER);
        g.drawString("Back", width - 5, height - footerHeight / 2,
            Graphics.RIGHT | Graphics.VCENTER);
    }

    private void handleMainMenuSelection(int selected) {
        if (selected == 0) showAddressBox();
        else if (selected == 1) showHome();
        else if (selected == 2) showTabs();
        else if (selected == 3) createTab();
        else if (selected == 4) showSpeedDial();
        else if (selected == 5) addCurrentSpeedDial();
        else if (selected == 6) showBookmarks();
        else if (selected == 7) addCurrentBookmark();
        else if (selected == 8) showHistory();
        else if (selected == 9) {
            if (currentUrl.length() > 0) loadUrl(currentUrl, false);
        } else if (selected == 10) navigateHistory(false);
        else if (selected == 11) toggleDataSaver();
        else if (selected == 12) showSettings();
        else if (selected == 13) showPageInformation();
        else if (selected == 14) showDiagnostics();
        else if (selected == 15) showAbout();
        else if (selected == 16) midlet.exit();
        else Display.getDisplay(midlet).setCurrent(this);
    }

    private void showSettings() {
        chooser = new List("Settings", List.IMPLICIT);
        chooser.append(dataSaverEnabled ? "Data Saver: On" : "Data Saver: Off", null);
        chooser.append("Text size: " + fontSizeLabel(), null);
        chooser.append(fullScreenEnabled ? "Full screen: On" : "Full screen: Off", null);
        chooser.append(loadImagesEnabled ? "Load Images: On" : "Load Images: Off", null);
        chooser.append(mobileViewEnabled ? "Mobile View: On" : "Mobile View: Off", null);
        chooser.append(restoreLastPageEnabled ? "Restore Last Page: On" : "Restore Last Page: Off", null);
        chooser.append("Gateway address", null);
        chooser.append("Gateway diagnostics", null);
        chooser.append("About Netormium", null);
        chooser.addCommand(new Command("Back", Command.BACK, 1));
        chooser.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(chooser);
    }

    private void configureGateway(String value) {
        interactive.configureBaseUrl(value);
        snapshotLoader.configureBaseUrl(value);
        loader.configureBaseUrl(value);
    }

    private void showGatewaySettings() {
        String current = gatewaySettings.load();
        if (current.length() == 0) current = jadGatewayUrl;
        gatewayBox = new TextBox("Gateway URL", current, 180, TextField.URL);
        gatewayBox.addCommand(new Command("Save", Command.OK, 1));
        gatewayBox.addCommand(new Command("Cancel", Command.CANCEL, 2));
        gatewayBox.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(gatewayBox);
    }

    private void toggleRestoreLastPage() {
        restoreLastPageEnabled = !restoreLastPageEnabled;
        preferences.saveRestoreLastPage(restoreLastPageEnabled);
        status = restoreLastPageEnabled ? "Last page will restore at startup" : "Start Page will open at startup";
        showSettings();
    }

    private void toggleLoadImages() {
        loadImagesEnabled = !loadImagesEnabled;
        preferences.saveLoadImages(loadImagesEnabled);
        status = loadImagesEnabled ? "Images enabled for next page" : "Images disabled for next page";
        showSettings();
    }

    private void toggleMobileView() {
        mobileViewEnabled = !mobileViewEnabled;
        preferences.saveMobileView(mobileViewEnabled);
        status = mobileViewEnabled ? "Mobile View enabled for next page" : "Mobile View disabled for next page";
        showSettings();
    }

    private String fontSizeLabel() { if (fontSize == 0) return "Small"; if (fontSize == 2) return "Large"; return "Normal"; }
    private void cycleFontSize() { fontSize = (fontSize + 1) % 3; preferences.saveFontSize(fontSize); ThemeManager.setFontSize(fontSize); status = "Text size: " + fontSizeLabel(); repaint(); showSettings(); }
    private void toggleFullScreen() { fullScreenEnabled = !fullScreenEnabled; preferences.saveFullScreen(fullScreenEnabled); setFullScreenMode(fullScreenEnabled); status = fullScreenEnabled ? "Full screen enabled" : "Full screen disabled"; repaint(); showSettings(); }
    private void toggleDataSaver() {
        dataSaverEnabled = !dataSaverEnabled;
        preferences.saveDataSaver(dataSaverEnabled);
        if (currentUrl.length() > 0) loadUrl(currentUrl, false);
        else {
            status = dataSaverEnabled ? "Data Saver enabled" : "Data Saver disabled";
            repaint();
        }
    }

    private void showAbout() {
        final Display display = Display.getDisplay(midlet);
        final Alert alert = new Alert("About Netormium",
            "Netormium\nBrowser version: 1.0.0-rc21\nPlatform: MIDP-2.0 / CLDC-1.1\nGateway version: Checking...\nChromium version: Checking...\nConnection: Checking...\n\nRemote Chromium rendering for Java ME phones.",
            null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        display.setCurrent(alert, this);
        new Thread(new Runnable() {
            public void run() {
                final String runtimeInfo = interactive.getRuntimeInfo();
                display.callSerially(new Runnable() {
                    public void run() {
                        alert.setString("Netormium\nBrowser version: 1.0.0-rc21\n" +
                            "Platform: MIDP-2.0 / CLDC-1.1\n" + runtimeInfo +
                            "\n\nRemote Chromium rendering for Java ME phones.");
                    }
                });
            }
        }).start();
    }

    private void showPageInformation() {
        StringBuffer info = new StringBuffer();
        info.append("Title: ").append(pageTitle == null ? "" : pageTitle).append("\n");
        info.append("Address: ").append(currentUrl.length() == 0 ? "Start page" : currentUrl).append("\n");
        info.append("Protocol: ");
        if (currentUrl.startsWith("https://")) info.append("HTTPS");
        else if (currentUrl.startsWith("http://")) info.append("HTTP");
        else info.append("Local");
        info.append("\nTabs: ").append(tabs.size());
        info.append("\nData Saver: ").append(dataSaverEnabled ? "On" : "Off");
        Alert alert = new Alert("Page Information", info.toString(), null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        Display.getDisplay(midlet).setCurrent(alert, this);
    }

    private void postUi(final int token, final String expectedSessionId, final Runnable action) {
        Display.getDisplay(midlet).callSerially(new Runnable() {
            public void run() {
                if (token != requestToken) return;
                if (expectedSessionId != null && !expectedSessionId.equals(sessionId)) return;
                action.run();
            }
        });
    }

    private void saveCurrentTab() {
        tabs.updateCurrent(currentUrl, pageTitle, scrollY);
        tabs.setCurrentSessionId(sessionId);
        stateStore.save(tabs);
    }

    private void persistState() {
        tabs.updateCurrent(currentUrl, pageTitle, scrollY);
        tabs.setCurrentSessionId(sessionId);
        stateStore.save(tabs);
        historyStore.save(history);
    }

    private void restoreActiveTab() {
        TabManager.Tab tab = tabs.current();
        currentUrl = tab.url == null ? "" : tab.url;
        pageTitle = tab.title == null ? "Netormium" : tab.title;
        scrollY = tab.scrollY;
        // Gateway sessions are in-memory and may disappear after server restart or TTL expiry.
        // Restore the URL and create a fresh session instead of trusting a persisted session ID.
        sessionId = "";
        tabs.setCurrentSessionId("");
        if (currentUrl.length() > 0 && restoreLastPageEnabled) loadUrl(currentUrl, false);
        else showHome();
    }

    private void createTab() {
        if (tabs.size() >= TabManager.MAX_TABS) {
            status = "Maximum 6 tabs";
            repaint();
            return;
        }
        stopLoading();
        saveCurrentTab();
        closeSession();
        int index = tabs.add("", "New Tab");
        if (index < 0) { status = "Maximum 6 tabs"; repaint(); return; }
        showHome();
        persistState();
        status = "New tab " + (index + 1);
        repaint();
    }

    private void switchTab(int index) {
        if (index == tabs.activeIndex()) return;
        stopLoading();
        saveCurrentTab();
        closeSession();
        if (!tabs.select(index)) return;
        TabManager.Tab tab = tabs.current();
        sessionId = "";
        currentUrl = tab.url == null ? "" : tab.url;
        pageTitle = tab.title == null ? "New Tab" : tab.title;
        scrollY = tab.scrollY;
        // Recreate remote Chromium state from the URL; tab session IDs are transient.
        tabs.setCurrentSessionId("");
        if (tab.url == null || tab.url.length() == 0) showHome();
        else loadUrl(tab.url, false);
        persistState();
        status = "Tab " + (index + 1) + "/" + tabs.size();
        repaint();
    }

    private void showTabs() {
        chooser = new List("Open Tab", List.IMPLICIT);
        String[] labels = tabs.labels();
        for (int i = 0; i < labels.length; i++) chooser.append(labels[i], null);
        chooser.addCommand(new Command("Back", Command.BACK, 1));
        chooser.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(chooser);
    }

    private void addCurrentSpeedDial() {
        if (currentUrl == null || currentUrl.length() == 0) status = "No page to add";
        else status = speedDial.add(pageTitle, currentUrl)
            ? "Added to Speed Dial" : "Speed Dial is full (12 entries)";
        repaint();
    }

    private void showSpeedDial() {
        SpeedDialStore.Entry[] entries = speedDial.all();
        chooser = new List("Speed Dial", List.IMPLICIT);
        speedDialUrls = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            speedDialUrls[i] = entries[i].url;
            chooser.append(entries[i].title, null);
        }
        if (entries.length == 0) chooser.append("No Speed Dial entries", null);
        chooser.addCommand(renameSpeedDialCommand);
        chooser.addCommand(deleteSpeedDialCommand);
        chooser.addCommand(moveSpeedDialUpCommand);
        chooser.addCommand(moveSpeedDialDownCommand);
        chooser.addCommand(new Command("Back", Command.BACK, 3));
        chooser.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(chooser);
    }

    private void addCurrentBookmark() {
        if (currentUrl == null || currentUrl.length() == 0) status = "No page to bookmark";
        else status = bookmarks.add(pageTitle, currentUrl) ? "Bookmark saved" : "Bookmark save failed";
        repaint();
    }

    private void showBookmarks() {
        BookmarkStore.Bookmark[] items = bookmarks.all();
        chooser = new List("Bookmarks", List.IMPLICIT);
        bookmarkUrls = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            bookmarkUrls[i] = items[i].url;
            chooser.append(items[i].title, null);
        }
        if (items.length == 0) chooser.append("No bookmarks saved", null);
        chooser.addCommand(renameBookmarkCommand);
        chooser.addCommand(deleteBookmarkCommand);
        chooser.addCommand(new Command("Back", Command.BACK, 3));
        chooser.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(chooser);
    }

    private void showHistory() {
        String[] entries = history.entries();
        chooser = new List("History", List.IMPLICIT);
        for (int i = entries.length - 1; i >= 0; i--) chooser.append(entries[i], null);
        if (entries.length == 0) chooser.append("No history yet", null);
        chooser.addCommand(deleteHistoryEntryCommand);
        chooser.addCommand(clearHistoryCommand);
        chooser.addCommand(new Command("Back", Command.BACK, 1));
        chooser.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(chooser);
    }

    private void showDiagnostics() {
        final int width = getWidth();
        final int height = getHeight();
        final String platform = System.getProperty("microedition.platform");
        final String config = System.getProperty("microedition.configuration");
        final String profile = System.getProperty("microedition.profiles");
        status = "Checking gateway...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                String gateway = interactive.healthCheck();
                StringBuffer report = new StringBuffer();
                report.append("Netormium 1.0.0-rc21\n");
                report.append("Screen: ").append(width).append(" x ").append(height).append("\n");
                report.append("Platform: ").append(platform == null ? "Unknown" : platform).append("\n");
                report.append("Configuration: ").append(config == null ? "Unknown" : config).append("\n");
                report.append("Profiles: ").append(profile == null ? "Unknown" : profile).append("\n");
                report.append("Gateway: ").append(gateway).append("\n");
                final String diagnosticText = report.toString();
                final boolean healthy = gateway.indexOf("OK") >= 0;
                Display.getDisplay(midlet).callSerially(new Runnable() {
                    public void run() {
                        Alert alert = new Alert("Diagnostics", diagnosticText, null,
                            healthy ? AlertType.INFO : AlertType.WARNING);
                        alert.setTimeout(Alert.FOREVER);
                        Display.getDisplay(midlet).setCurrent(alert, BrowserCanvas.this);
                        status = "Diagnostics complete";
                        repaint();
                    }
                });
            }
        }).start();
    }

    public void persistOnPause() {
        persistState();
        preferences.saveDataSaver(dataSaverEnabled);
    }

    public void shutdown() {
        stopLoading();
        closeSession();
        persistState();
        preferences.saveDataSaver(dataSaverEnabled);
    }

    private void sendTextToPage(final String value) {
        if (sessionId == null || sessionId.length() == 0) {
            status = "No active page session";
            Display.getDisplay(midlet).setCurrent(this);
            repaint();
            return;
        }
        if (interactionBusy) {
            status = "Please wait for the current page action";
            repaint();
            return;
        }
        interactionBusy = true;
        final String id = sessionId;
        final int token = requestToken;
        Display.getDisplay(midlet).setCurrent(this);
        status = "Sending text...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                final com.netormium.client.net.InteractiveGateway.Page result = interactive.type(id, value);
                if (token != requestToken || !id.equals(sessionId)) return;
                postUi(token, id, new Runnable() {
                    public void run() {
                        interactionBusy = false;
                        if (result.error != null && result.error.length() > 0) {
                            if (!recoverExpiredSession(id, token, result.error)) status = result.error;
                        } else {
                            pageTitle = result.title;
                            pageText = result.text;
                            interactiveElements = result.elements;
                            inputFocused = hasFocusedInput();
                            status = "Text sent";
                            refreshSessionSnapshot(token, scrollY);
                        }
                        repaint();
                    }
                });
            }
        }).start();
    }

    private void showAddressBox() {
        addressBox = new TextBox("Open address", currentUrl, 512, TextField.URL);
        addressBox.addCommand(new Command("Go", Command.OK, 1));
        addressBox.addCommand(new Command("Cancel", Command.CANCEL, 2));
        addressBox.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(addressBox);
    }

    private void showHome() {
        stopLoading();
        closeSession();
        pageLinks = new GatewayPageLoader.Link[0];
        pageImage = null;
        currentUrl = "";
        tabs.updateCurrent("", "New Tab", 0);
        pageTitle = "Netormium";
        pageText = "Welcome to Netormium 1.0.0-rc21.\n\nChoose Open to enter a website address.\n\nPages are rendered by the remote Chromium Gateway.\n\nConfigure Netormium-Gateway-URL in the JAD before browsing.";
        status = "Home";
        scrollY = 0;
        repaint();
    }

    private void navigateHistory(boolean back) {
        String url = back ? history.back() : history.forward();
        historyStore.save(history);
        if (url != null) loadUrl(url, false);
    }

    private void loadUrl(final String url, boolean addHistory) {
        stopLoading();
        closeSession();
        if (addHistory) { history.visit(url); historyStore.save(history); }
        currentUrl = url;
        tabs.setCurrentSessionId("");
        tabs.updateCurrent(url, "Loading...", 0);
        persistState();
        pageTitle = "Loading...";
        pageLinks = new GatewayPageLoader.Link[0];
        pageImage = null;
        pageText = "Connecting to " + url;
        status = "Loading";
        scrollY = 0;
        loading = true;
        final int token = ++requestToken;
        final int viewportWidth = Math.max(128, Math.min(640, getWidth()));
        final ScreenMetrics metrics = new ScreenMetrics(getWidth(), getHeight());
        final AdaptiveLayout layout = new AdaptiveLayout(metrics);
        final int viewportHeight = Math.max(128, Math.min(960, getHeight() -
            layout.getAddressBarHeight() - layout.getToolbarHeight() - 6));
        final boolean useDataSaver = dataSaverEnabled;
        final boolean useImages = loadImagesEnabled;
        final boolean useMobileView = mobileViewEnabled;
        repaint();

        Thread worker = new Thread(new Runnable() {
            public void run() {
                final com.netormium.client.net.InteractiveGateway.Page result =
                    interactive.open(url, viewportWidth, viewportHeight, useDataSaver, useImages, useMobileView);
                if (token != requestToken) {
                    if (result.sessionId.length() > 0) interactive.close(result.sessionId);
                    return;
                }
                postUi(token, null, new Runnable() {
                    public void run() {
                        loading = false;
                        if (result.error != null && result.error.length() > 0) {
                            pageTitle = "Unable to load";
                            pageText = result.error;
                            status = "Connection error - Open to retry";
                            pageImage = null;
                        } else {
                            sessionId = result.sessionId;
                            tabs.setCurrentSessionId(sessionId);
                            currentUrl = result.url;
                            pageTitle = result.title.length() == 0 ? currentUrl : result.title;
                            tabs.updateCurrent(currentUrl, pageTitle, 0);
                            persistState();
                            pageText = result.text;
                            interactiveElements = result.elements;
                            status = useDataSaver ? "Interactive session · Data Saver" : "Interactive session";
                            refreshSessionSnapshot(token, scrollY);
                        }
                        scrollY = 0;
                        repaint();
                    }
                });
            }
        });
        worker.start();
    }

    private boolean recoverExpiredSession(String id, int token, String error) {
        if (token != requestToken || id == null || !id.equals(sessionId) ||
            !interactive.isSessionExpired(error)) return false;
        tabs.setCurrentSessionId("");
        sessionId = "";
        stateStore.save(tabs);
        if (currentUrl != null && currentUrl.length() > 0) loadUrl(currentUrl, false);
        else showHome();
        return true;
    }

    private void closeSession() {
        interactionBusy = false;
        if (sessionId != null && sessionId.length() > 0) {
            String oldSession = sessionId;
            tabs.setCurrentSessionId("");
            interactive.close(oldSession);
            sessionId = "";
        }
        interactiveElements = new com.netormium.client.net.InteractiveGateway.Element[0];
        inputFocused = false;
        pageImage = null;
    }

    private void refreshSessionSnapshot(final int token, final int offset) {
        if (sessionId == null || sessionId.length() == 0) return;
        final String id = sessionId;
        final int width = Math.max(128, Math.min(640, getWidth()));
        final int height = Math.max(128, Math.min(960,
            getHeight() - new AdaptiveLayout(new ScreenMetrics(getWidth(), getHeight())).getAddressBarHeight()
            - new AdaptiveLayout(new ScreenMetrics(getWidth(), getHeight())).getToolbarHeight() - 6));
        new Thread(new Runnable() {
            public void run() {
                final byte[] png = interactive.snapshot(id, offset);
                if (token != requestToken || !id.equals(sessionId)) return;
                if (png == null) {
                    final String error = interactive.getLastTransportError();
                    postUi(token, id, new Runnable() {
                        public void run() {
                            if (!recoverExpiredSession(id, token, error)) {
                                status = error.length() == 0 ? "Snapshot failed - check gateway" : error;
                                repaint();
                            }
                        }
                    });
                    return;
                }
                final com.netormium.client.net.InteractiveGateway.Page fresh = interactive.inspect(id);
                if (token != requestToken || !id.equals(sessionId)) return;
                postUi(token, id, new Runnable() {
                    public void run() {
                        if (fresh != null && fresh.error.length() > 0 &&
                            recoverExpiredSession(id, token, fresh.error)) return;
                        if (fresh != null && fresh.error.length() == 0) {
                            interactiveElements = fresh.elements;
                            syncSelectedElement();
                            inputFocused = hasFocusedInput();
                        }
                        try {
                            pageImage = null;
                            pageImage = Image.createImage(png, 0, png.length);
                            status = selectedElement >= 0 ? "Element " + (selectedElement + 1) : "Interactive page";
                        } catch (Exception e) {
                            status = "Image decode failed";
                        }
                        repaint();
                    }
                });
            }
        }).start();
    }

    private void tapPage(final int x, final int y) {
        if (sessionId == null || sessionId.length() == 0) return;
        if (interactionBusy) {
            status = "Please wait for the current page action";
            repaint();
            return;
        }
        interactionBusy = true;
        final String id = sessionId;
        final int token = requestToken;
        status = "Activating...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                final com.netormium.client.net.InteractiveGateway.Page result = interactive.tap(id, x, y);
                if (token != requestToken || !id.equals(sessionId)) return;
                postUi(token, id, new Runnable() {
                    public void run() {
                        interactionBusy = false;
                        if (result.error != null && result.error.length() > 0) {
                            if (!recoverExpiredSession(id, token, result.error)) status = result.error;
                        } else {
                            currentUrl = result.url;
                            pageTitle = result.title;
                            tabs.updateCurrent(currentUrl, pageTitle, scrollY);
                            tabs.setCurrentSessionId(sessionId);
                            persistState();
                            pageText = result.text;
                            interactiveElements = result.elements;
                            inputFocused = hasFocusedInput();
                            if (currentUrl != null && currentUrl.length() > 0) {
                                history.visit(currentUrl);
                                historyStore.save(history);
                            }
                            refreshSessionSnapshot(token, scrollY);
                        }
                        repaint();
                    }
                });
            }
        }).start();
    }

    private void syncSelectedElement() {
        int focused = -1;
        for (int i = 0; i < interactiveElements.length; i++) {
            if (interactiveElements[i].focused) { focused = i; break; }
        }
        if (focused >= 0) selectedElement = focused;
        else if (selectedElement >= interactiveElements.length) selectedElement = interactiveElements.length - 1;
        if (interactiveElements.length == 0) selectedElement = -1;
    }

    private void moveElementSelection(int direction) {
        if (interactiveElements.length == 0) {
            status = "No interactive elements";
            repaint();
            return;
        }
        if (selectedElement < 0) selectedElement = direction > 0 ? 0 : interactiveElements.length - 1;
        else {
            selectedElement += direction;
            if (selectedElement < 0) selectedElement = interactiveElements.length - 1;
            if (selectedElement >= interactiveElements.length) selectedElement = 0;
        }
        com.netormium.client.net.InteractiveGateway.Element e = interactiveElements[selectedElement];
        status = (selectedElement + 1) + "/" + interactiveElements.length + " " + e.label;
        repaint();
    }

    private void activateSelectedElement() {
        if (selectedElement >= 0 && selectedElement < interactiveElements.length) {
            com.netormium.client.net.InteractiveGateway.Element e = interactiveElements[selectedElement];
            tapPage(e.x + Math.max(1, e.width / 2), e.y + Math.max(1, e.height / 2));
        } else {
            showAddressBox();
        }
    }

    private boolean hasFocusedInput() {
        for (int i = 0; i < interactiveElements.length; i++) {
            if ("input".equals(interactiveElements[i].kind) && interactiveElements[i].focused) return true;
        }
        return false;
    }

    private void sendBrowserKey(final String key) {
        if (sessionId == null || sessionId.length() == 0) return;
        if (interactionBusy) {
            status = "Please wait for the current page action";
            repaint();
            return;
        }
        interactionBusy = true;
        final String id = sessionId;
        final int token = requestToken;
        new Thread(new Runnable() {
            public void run() {
                final com.netormium.client.net.InteractiveGateway.Page result = interactive.key(id, key);
                if (token != requestToken || !id.equals(sessionId)) return;
                postUi(token, id, new Runnable() {
                    public void run() {
                        interactionBusy = false;
                        if (result.error != null && result.error.length() > 0) {
                            if (!recoverExpiredSession(id, token, result.error)) status = result.error;
                            repaint();
                            return;
                        }
                        currentUrl = result.url;
                        pageTitle = result.title;
                        tabs.updateCurrent(currentUrl, pageTitle, scrollY);
                        tabs.setCurrentSessionId(sessionId);
                        persistState();
                        pageText = result.text;
                        interactiveElements = result.elements;
                        status = "Key: " + key;
                        refreshSessionSnapshot(token, scrollY);
                        repaint();
                    }
                });
            }
        }).start();
    }

    private void showInputBox() {
        if (!inputFocused || sessionId == null || sessionId.length() == 0) {
            status = "Tap a text field first";
            repaint();
            return;
        }
        inputBox = new TextBox("Enter text", "", 512, TextField.ANY);
        inputBox.addCommand(new Command("Send", Command.OK, 1));
        inputBox.addCommand(new Command("Cancel", Command.CANCEL, 2));
        inputBox.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(inputBox);
    }

    public void stopLoading() {
        interactionBusy = false;
        requestToken++;
        loading = false;
        interactive.cancelCurrentOpen();
        persistState();
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        ScreenMetrics metrics = new ScreenMetrics(w, h);
        AdaptiveLayout layout = new AdaptiveLayout(metrics);
        Font body = ThemeManager.getBodyFont(metrics);
        Font title = ThemeManager.getTitleFont();

        g.setColor(ThemeManager.BACKGROUND);
        g.fillRect(0, 0, w, h);

        int headerH = layout.getAddressBarHeight();
        g.setColor(0xDCE6F0);
        g.fillRect(0, 0, w, headerH);
        g.setColor(0x8298AE);
        g.drawLine(0, headerH - 1, w, headerH - 1);
        int globe = Math.min(12, headerH - 5);
        int globeY = (headerH - globe) / 2;
        g.setColor(ThemeManager.ACCENT);
        g.fillArc(4, globeY, globe, globe, 0, 360);
        g.setColor(0xFFFFFF);
        if (globe > 6) {
            g.drawArc(6, globeY + 2, globe - 4, globe - 4, 0, 360);
            g.drawLine(5, globeY + globe / 2, 3 + globe, globeY + globe / 2);
        }
        g.setFont(body);
        g.setColor(0x18334D);
        String addressLabel = currentUrl.length() == 0 ? "Search or enter address" : currentUrl;
        g.drawString(clip(addressLabel, body, w - globe - 15), globe + 9, headerH / 2,
            Graphics.LEFT | Graphics.VCENTER);
        g.setColor(0xFFFFFF);
        g.fillRect(w - 17, 2, 15, headerH - 5);
        g.setColor(0x71869A);
        g.drawRect(w - 17, 2, 14, headerH - 6);
        g.drawString("Go", w - 10, headerH / 2, Graphics.HCENTER | Graphics.VCENTER);

        int contentTop = headerH + 3;
        int footerH = layout.getToolbarHeight();
        int footerY = h - footerH;
        int contentBottom = footerY - 2;
        int available = contentBottom - contentTop;

        g.setClip(0, contentTop, w, available);
        if (pageImage != null) {
            g.drawImage(pageImage, 0, contentTop, Graphics.LEFT | Graphics.TOP);
            if (selectedElement >= 0 && selectedElement < interactiveElements.length) {
                com.netormium.client.net.InteractiveGateway.Element selected = interactiveElements[selectedElement];
                g.setColor(0x1687FF);
                g.drawRect(selected.x, contentTop + selected.y,
                    Math.max(2, selected.width), Math.max(2, selected.height));
                if (selected.width > 4 && selected.height > 4) {
                    g.drawRect(selected.x + 1, contentTop + selected.y + 1,
                        selected.width - 2, selected.height - 2);
                }
            }
        } else if (currentUrl.length() == 0) {
            paintStartPage(g, w, contentTop, contentBottom, body, title);
        } else {
            g.setColor(ThemeManager.FOREGROUND);
            g.setFont(title);
            g.drawString(clip(pageTitle, title, w - 10), 5, contentTop + 2 - scrollY,
                Graphics.LEFT | Graphics.TOP);
            g.setFont(body);
            int lineHeight = body.getHeight() + 2;
            int y = contentTop + title.getHeight() + 7 - scrollY;
            String[] lines = wrap(pageText, body, w - 10);
            for (int i = 0; i < lines.length; i++) {
                int lineY = y + (i * lineHeight);
                if (lineY + lineHeight >= contentTop && lineY < contentBottom) {
                    g.drawString(lines[i], 5, lineY, Graphics.LEFT | Graphics.TOP);
                }
            }
        }
        g.setClip(0, 0, w, h);

        if (mainMenuOpen) {
            paintMainMenu(g, w, h, body);
            return;
        }

        g.setColor(0x244D70);
        g.fillRect(0, footerY, w, footerH);
        g.setColor(0x102F4B);
        g.drawLine(0, footerY, w, footerY);
        g.setFont(body);
        g.setColor(0xFFFFFF);
        g.drawString("Menu", 5, footerY + footerH / 2,
            Graphics.LEFT | Graphics.VCENTER);
        String rightSoftkey = currentUrl.length() == 0 ? "Open" : "Select";
        g.drawString(rightSoftkey, w - 5, footerY + footerH / 2,
            Graphics.RIGHT | Graphics.VCENTER);

        if (status.length() > 0 && h > 150) {
            g.setColor(ThemeManager.MUTED);
            g.drawString(clip(status, body, w - 8), w / 2, footerY - 1,
                Graphics.HCENTER | Graphics.BOTTOM);
        }
    }

    private void paintStartPage(Graphics g, int width, int top, int bottom,
        Font body, Font title) {
        int logoSize = 15;
        int logoX = 7;
        int logoY = top + 3;
        g.setColor(ThemeManager.ACCENT);
        g.fillArc(logoX, logoY, logoSize, logoSize, 0, 360);
        g.setColor(0xFFFFFF);
        g.drawArc(logoX + 3, logoY + 3, logoSize - 6, logoSize - 6, 0, 360);
        g.drawLine(logoX + 7, logoY + 1, logoX + 7, logoY + logoSize - 1);
        g.setColor(0x18334D);
        g.setFont(title);
        g.drawString("Netormium", logoX + logoSize + 5, logoY,
            Graphics.LEFT | Graphics.TOP);
        g.setFont(body);
        g.setColor(ThemeManager.MUTED);
        g.drawString("Speed Dial", width - 6, logoY + 2,
            Graphics.RIGHT | Graphics.TOP);

        int searchY = top + 23;
        int searchHeight = Math.max(20, body.getHeight() + 8);
        g.setColor(0xFFFFFF);
        g.fillRect(5, searchY, width - 10, searchHeight);
        g.setColor(0x879BAE);
        g.drawRect(5, searchY, width - 11, searchHeight - 1);
        g.setColor(0x687D91);
        g.drawString("Search or enter address", 10, searchY + searchHeight / 2,
            Graphics.LEFT | Graphics.VCENTER);

        int labelY = searchY + searchHeight + 7;
        g.setColor(0x315A7C);
        g.setFont(body);
        g.drawString("TOP SITES", 6, labelY, Graphics.LEFT | Graphics.TOP);
        int gridY = labelY + body.getHeight() + 4;

        SpeedDialStore.Entry[] items = speedDial.all();
        if (items.length == 0) {
            g.setColor(0x718397);
            g.drawString("Your favorite sites appear here.", 7, gridY + 5,
                Graphics.LEFT | Graphics.TOP);
            g.drawString("Open a page and choose Add to Speed Dial.",
                7, gridY + body.getHeight() + 8, Graphics.LEFT | Graphics.TOP);
            return;
        }

        int columns = width >= 220 ? 3 : 2;
        int gap = width >= 220 ? 5 : 4;
        int tileWidth = (width - 12 - ((columns - 1) * gap)) / columns;
        int tileHeight = width >= 220 ? 51 : 43;
        int thumbHeight = width >= 220 ? 28 : 22;
        int visible = Math.min(9, items.length);
        for (int i = 0; i < visible; i++) {
            int column = i % columns;
            int row = i / columns;
            int x = 6 + column * (tileWidth + gap);
            int y = gridY + row * (tileHeight + gap);
            if (y + tileHeight > bottom) break;
            int shade = i % 3;
            int thumbColor = shade == 0 ? 0xD7E5EF : (shade == 1 ? 0xE3E8D9 : 0xE8DFD5);
            g.setColor(thumbColor);
            g.fillRect(x, y, tileWidth, thumbHeight);
            g.setColor(0x9AAABA);
            g.drawRect(x, y, tileWidth - 1, tileHeight - 1);
            g.setColor(0xFFFFFF);
            g.fillRect(x + 3, y + 3, Math.max(5, tileWidth - 6), Math.max(5, thumbHeight - 6));
            g.setColor(ThemeManager.ACCENT);
            g.fillRect(x + 3, y + 3, Math.max(5, tileWidth - 6), 3);
            g.setColor(0x4B6277);
            g.drawString(String.valueOf(i + 1), x + 5, y + 7,
                Graphics.LEFT | Graphics.TOP);
            g.setColor(0x24394D);
            g.drawString(clip(items[i].title, body, tileWidth - 6),
                x + 3, y + thumbHeight + 2, Graphics.LEFT | Graphics.TOP);
            if (i == startPageSelection) {
                g.setColor(0x0878C9);
                g.drawRect(x - 1, y - 1, tileWidth + 1, tileHeight + 1);
                g.drawRect(x, y, tileWidth - 1, tileHeight - 1);
            }
        }
        if (items.length > visible) {
            g.setColor(ThemeManager.MUTED);
            g.drawString("More sites in Menu > Speed Dial",
                6, Math.min(bottom - body.getHeight(), gridY + 3 * (tileHeight + gap)),
                Graphics.LEFT | Graphics.TOP);
        }
    }

    private int speedDialIndexAt(int x, int y, int top, int bottom) {
        ScreenMetrics metrics = new ScreenMetrics(getWidth(), getHeight());
        Font body = ThemeManager.getBodyFont(metrics);
        int searchY = top + 23;
        int searchHeight = Math.max(20, body.getHeight() + 8);
        if (y >= searchY && y < searchY + searchHeight &&
            x >= 5 && x < getWidth() - 5) return -2;
        int labelY = searchY + searchHeight + 7;
        int gridY = labelY + body.getHeight() + 4;
        int columns = getWidth() >= 220 ? 3 : 2;
        int gap = getWidth() >= 220 ? 5 : 4;
        int tileWidth = (getWidth() - 12 - ((columns - 1) * gap)) / columns;
        int tileHeight = getWidth() >= 220 ? 51 : 43;
        if (y < gridY) return -1;
        int row = (y - gridY) / (tileHeight + gap);
        int column = (x - 6) / (tileWidth + gap);
        if (column < 0 || column >= columns || row < 0) return -1;
        int tileX = 6 + column * (tileWidth + gap);
        int tileY = gridY + row * (tileHeight + gap);
        if (x < tileX || x >= tileX + tileWidth ||
            y < tileY || y >= tileY + tileHeight || tileY + tileHeight > bottom) return -1;
        return row * columns + column;
    }

    protected void pointerPressed(int x, int y) {
        if (mainMenuOpen) {
            int footerHeight = Math.max(18, ThemeManager.getBodyFont(
                new ScreenMetrics(getWidth(), getHeight())).getHeight() + 7);
            if (y >= getHeight() - footerHeight && x >= getWidth() / 2) {
                closeMainMenu();
                return;
            }
            Font body = ThemeManager.getBodyFont(new ScreenMetrics(getWidth(), getHeight()));
            int headerHeight = Math.max(22, body.getHeight() + 10);
            int rowHeight = Math.max(19, body.getHeight() + 5);
            int listTop = 2 + headerHeight + 2;
            int index = mainMenuScroll + ((y - listTop) / rowHeight);
            String[] items = mainMenuItems();
            if (y >= listTop && index >= 0 && index < items.length) {
                mainMenuSelection = index;
                mainMenuOpen = false;
                handleMainMenuSelection(index);
            }
            return;
        }
        ScreenMetrics metrics = new ScreenMetrics(getWidth(), getHeight());
        AdaptiveLayout layout = new AdaptiveLayout(metrics);
        int top = layout.getAddressBarHeight() + 3;
        int bottom = getHeight() - layout.getToolbarHeight() - 2;
        if (y >= top && y < bottom && currentUrl.length() == 0) {
            int dial = speedDialIndexAt(x, y, top, bottom);
            if (dial == -2) { showAddressBox(); return; }
            SpeedDialStore.Entry[] items = speedDial.all();
            if (dial >= 0 && dial < items.length) {
                startPageSelection = dial;
                loadUrl(items[dial].url, true);
            }
        } else if (y >= top && y < bottom && sessionId.length() > 0) {
            tapPage(x, y - top);
        } else if (y >= getHeight() - layout.getToolbarHeight()) {
            if (x < getWidth() / 2) showMainMenu();
            else showAddressBox();
        } else if (y < top) {
            showAddressBox();
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (mainMenuOpen) {
            String[] items = mainMenuItems();
            if (action == UP) {
                if (mainMenuSelection > 0) mainMenuSelection--;
                repaint();
            } else if (action == DOWN) {
                if (mainMenuSelection + 1 < items.length) mainMenuSelection++;
                repaint();
            } else if (action == FIRE || keyCode == KEY_NUM5) {
                int selected = mainMenuSelection;
                mainMenuOpen = false;
                handleMainMenuSelection(selected);
            } else if (keyCode == KEY_NUM0 || keyCode == '*') {
                closeMainMenu();
            }
            return;
        }
        if (currentUrl.length() == 0 && pageImage == null) {
            SpeedDialStore.Entry[] homeItems = speedDial.all();
            int columns = getWidth() >= 220 ? 3 : 2;
            if (action == LEFT && startPageSelection > 0) {
                startPageSelection--;
                repaint();
                return;
            } else if (action == RIGHT && startPageSelection + 1 < homeItems.length) {
                startPageSelection++;
                repaint();
                return;
            } else if (action == UP && startPageSelection - columns >= 0) {
                startPageSelection -= columns;
                repaint();
                return;
            } else if (action == DOWN && startPageSelection + columns < homeItems.length) {
                startPageSelection += columns;
                repaint();
                return;
            } else if (action == FIRE || keyCode == KEY_NUM5) {
                if (startPageSelection >= 0 && startPageSelection < homeItems.length) {
                    loadUrl(homeItems[startPageSelection].url, true);
                } else showAddressBox();
                return;
            }
        }
        if (action == UP) {
            scrollY -= 120;
            if (scrollY < 0) scrollY = 0;
            tabs.setCurrentScroll(scrollY);
            stateStore.save(tabs);
            if (sessionId.length() > 0) refreshSessionSnapshot(requestToken, scrollY);
            else repaint();
        } else if (action == DOWN) {
            scrollY += 120;
            tabs.setCurrentScroll(scrollY);
            stateStore.save(tabs);
            if (sessionId.length() > 0) refreshSessionSnapshot(requestToken, scrollY);
            else repaint();
        } else if (action == LEFT) {
            moveElementSelection(-1);
        } else if (action == RIGHT) {
            moveElementSelection(1);
        } else if (action == FIRE) {
            activateSelectedElement();
        } else if (keyCode == KEY_NUM0) {
            scrollY = 0;
            tabs.setCurrentScroll(scrollY);
            stateStore.save(tabs);
            if (sessionId.length() > 0) refreshSessionSnapshot(requestToken, scrollY);
            else repaint();
        } else if (keyCode == KEY_NUM5) {
            activateSelectedElement();
        } else if (keyCode >= KEY_NUM1 && keyCode <= KEY_NUM9) {
            int selected = keyCode - KEY_NUM0;
            if (currentUrl.length() == 0) {
                SpeedDialStore.Entry[] items = speedDial.all();
                if (selected > 0 && selected <= items.length && selected <= 6) {
                    loadUrl(items[selected - 1].url, true);
                }
            } else if (sessionId.length() > 0 && selected > 0 && selected <= interactiveElements.length) {
                com.netormium.client.net.InteractiveGateway.Element element =
                    interactiveElements[selected - 1];
                tapPage(element.x + (element.width / 2), element.y + (element.height / 2));
            } else if (selected > 0 && selected <= pageLinks.length) {
                loadUrl(pageLinks[selected - 1].href, true);
            }
        }
    }

    private void reloadSnapshot() {
        final int token = requestToken;
        final String url = currentUrl;
        final int width = Math.max(128, Math.min(640, getWidth()));
        final int height = Math.max(128, Math.min(960,
            getHeight() - new AdaptiveLayout(new ScreenMetrics(getWidth(), getHeight())).getAddressBarHeight()
            - new AdaptiveLayout(new ScreenMetrics(getWidth(), getHeight())).getToolbarHeight() - 6));
        final int offset = scrollY;
        status = "Rendering viewport";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                final byte[] png = snapshotLoader.load(url, width, height, offset);
                if (token != requestToken) return;
                postUi(token, null, new Runnable() {
                    public void run() {
                        if (png == null) {
                            status = "Snapshot failed - check gateway";
                        } else {
                            try {
                                pageImage = null;
                                pageImage = Image.createImage(png, 0, png.length);
                                status = "Visual page";
                            } catch (Exception e) {
                                status = "Snapshot failed";
                            }
                        }
                        repaint();
                    }
                });
            }
        }).start();
    }

    private int getFontHeight() {
        return ThemeManager.getBodyFont(new ScreenMetrics(getWidth(), getHeight())).getHeight() + 2;
    }

    private String[] wrap(String text, Font font, int maxWidth) {
        java.util.Vector lines = new java.util.Vector();
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) end = text.length();
            String paragraph = text.substring(start, end);
            while (paragraph.length() > 0) {
                int cut = paragraph.length();
                while (cut > 1 && font.stringWidth(paragraph.substring(0, cut)) > maxWidth) {
                    cut--;
                }
                if (cut < paragraph.length()) {
                    int space = paragraph.lastIndexOf(' ', cut);
                    if (space > 0) cut = space;
                }
                lines.addElement(paragraph.substring(0, cut));
                paragraph = paragraph.substring(cut);
                while (paragraph.startsWith(" ")) paragraph = paragraph.substring(1);
            }
            if (end == text.length()) break;
            lines.addElement("");
            start = end + 1;
        }
        String[] result = new String[lines.size()];
        lines.copyInto(result);
        return result;
    }

    private String clip(String value, Font font, int maxWidth) {
        if (value == null) return "";
        if (font.stringWidth(value) <= maxWidth) return value;
        String suffix = "...";
        int end = value.length();
        while (end > 0 && font.stringWidth(value.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end > 0 ? value.substring(0, end) + suffix : suffix;
    }
}
