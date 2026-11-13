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
import com.netormium.client.util.BrowserStateStore;
import com.netormium.client.util.HistoryStore;
import com.netormium.client.util.BrowserPreferences;

/** Netormium RC21 browser canvas with keypad navigation and remote Chromium snapshots. */
public final class BrowserCanvas extends Canvas implements CommandListener {
    private final NetormiumMIDlet midlet;
    private final BrowserHistory history = new BrowserHistory(100);
    private final BrowserStateStore stateStore = new BrowserStateStore();
    private final HistoryStore historyStore = new HistoryStore();
    private final BrowserPreferences preferences = new BrowserPreferences();
    private final TabManager tabs = new TabManager();
    private final BookmarkStore bookmarks = new BookmarkStore();
    private String[] bookmarkUrls = new String[0];
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
    private final Command homeCommand = new Command("Home", Command.SCREEN, 2);
    private final Command backCommand = new Command("Back", Command.BACK, 1);
    private final Command forwardCommand = new Command("Forward", Command.SCREEN, 3);
    private final Command reloadCommand = new Command("Reload", Command.SCREEN, 4);
    private final Command typeCommand = new Command("Type", Command.SCREEN, 5);
    private final Command enterCommand = new Command("Enter", Command.SCREEN, 6);
    private final Command dataSaverCommand = new Command("Toggle Data Saver", Command.SCREEN, 7);
    private boolean dataSaverEnabled = true;
    private final Command newTabCommand = new Command("New Tab", Command.SCREEN, 8);
    private final Command tabsCommand = new Command("Tabs", Command.SCREEN, 9);
    private final Command nextTabCommand = new Command("Next Tab", Command.SCREEN, 10);
    private final Command addBookmarkCommand = new Command("Add Bookmark", Command.SCREEN, 11);
    private final Command bookmarksCommand = new Command("Bookmarks", Command.SCREEN, 12);
    private final Command historyCommand = new Command("History", Command.SCREEN, 13);
    private final Command clearHistoryCommand = new Command("Clear History", Command.SCREEN, 14);
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
        interactive.configureBaseUrl(gatewayUrl);
        snapshotLoader.configureBaseUrl(gatewayUrl);
        loader.configureBaseUrl(gatewayUrl);
        setFullScreenMode(false);
        addCommand(openCommand);
        addCommand(homeCommand);
        addCommand(backCommand);
        addCommand(forwardCommand);
        addCommand(reloadCommand);
        addCommand(typeCommand);
        addCommand(enterCommand);
        addCommand(dataSaverCommand);
        addCommand(newTabCommand);
        addCommand(tabsCommand);
        addCommand(nextTabCommand);
        addCommand(addBookmarkCommand);
        addCommand(bookmarksCommand);
        addCommand(historyCommand);
        addCommand(stopCommand);
        addCommand(diagnosticsCommand);
        addCommand(exitCommand);
        setCommandListener(this);
        stateStore.load(tabs);
        historyStore.load(history);
        restoreActiveTab();
    }

    public void commandAction(Command command, javax.microedition.lcdui.Displayable displayable) {
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
        if (displayable == inputBox) {
            if (command.getCommandType() == Command.OK) sendTextToPage(inputBox.getString());
            else Display.getDisplay(midlet).setCurrent(this);
            return;
        }
        if (displayable == chooser) {
            if (command == clearHistoryCommand && "History".equals(chooser.getTitle())) {
                history.clear();
                historyStore.save(history);
                status = "History cleared";
                showHistory();
                return;
            }
            if (command == List.SELECT_COMMAND) {
                int selected = chooser.getSelectedIndex();
                if (selected >= 0) {
                    if ("Open Tab".equals(chooser.getTitle())) switchTab(selected);
                    else if ("Bookmarks".equals(chooser.getTitle()) && selected < bookmarkUrls.length)
                        loadUrl(bookmarkUrls[selected], true);
                    else if ("History".equals(chooser.getTitle())) {
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
        else if (command == homeCommand) showHome();
        else if (command == backCommand) navigateHistory(true);
        else if (command == forwardCommand) navigateHistory(false);
        else if (command == reloadCommand) { if (currentUrl.length() > 0) loadUrl(currentUrl, false); }
        else if (command == exitCommand) midlet.exit();
        else if (command == diagnosticsCommand) showDiagnostics();
        else if (command == typeCommand) showInputBox();
        else if (command == enterCommand) sendBrowserKey("Enter");
        else if (command == dataSaverCommand) {
            dataSaverEnabled = !dataSaverEnabled;
            preferences.saveDataSaver(dataSaverEnabled);
            if (currentUrl.length() > 0) loadUrl(currentUrl, false);
            else { status = dataSaverEnabled ? "Data Saver enabled" : "Data Saver disabled"; repaint(); }
        } else if (command == newTabCommand) createTab();
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
        if (currentUrl.length() > 0) loadUrl(currentUrl, false);
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
        chooser.addCommand(new Command("Back", Command.BACK, 1));
        chooser.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(chooser);
    }

    private void showHistory() {
        String[] entries = history.entries();
        chooser = new List("History", List.IMPLICIT);
        for (int i = entries.length - 1; i >= 0; i--) chooser.append(entries[i], null);
        if (entries.length == 0) chooser.append("No history yet", null);
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
                report.append("Netormium 1.0 RC21\n");
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
        pageText = "Welcome to Netormium 1.0 RC21.\n\nChoose Open to enter a website address.\n\nPages are rendered by the remote Chromium Gateway.\n\nConfigure Netormium-Gateway-URL in the JAD before browsing.";
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
        repaint();

        Thread worker = new Thread(new Runnable() {
            public void run() {
                final com.netormium.client.net.InteractiveGateway.Page result =
                    interactive.open(url, viewportWidth, viewportHeight, useDataSaver);
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
        g.setColor(ThemeManager.HEADER_BACKGROUND);
        g.fillRect(0, 0, w, headerH);
        g.setColor(ThemeManager.BORDER);
        g.drawRect(0, 0, w - 1, headerH - 1);
        g.setFont(body);
        g.setColor(ThemeManager.HEADER_FOREGROUND);
        String addressLabel = currentUrl.length() == 0 ? "Netormium - Open" : currentUrl;
        g.drawString(clip(addressLabel, body, w - 10), 5, headerH / 2,
            Graphics.LEFT | Graphics.VCENTER);

        int contentTop = headerH + 3;
        int footerH = layout.getToolbarHeight();
        int contentBottom = h - footerH - 2;
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

        int footerY = h - footerH;
        g.setColor(ThemeManager.HEADER_BACKGROUND);
        g.fillRect(0, footerY, w, footerH);
        g.setColor(ThemeManager.BORDER);
        g.drawLine(0, footerY, w, footerY);
        g.setFont(body);
        g.setColor(ThemeManager.HEADER_FOREGROUND);
        g.drawString("Open", 3, footerY + footerH / 2,
            Graphics.LEFT | Graphics.VCENTER);
        g.drawString(loading ? "Stop" : "Links " + pageLinks.length, w - 3, footerY + footerH / 2,
            Graphics.RIGHT | Graphics.VCENTER);

        g.setColor(ThemeManager.MUTED);
        if (status.length() > 0 && h > 150) {
            g.drawString(status, w / 2, footerY - 1,
                Graphics.HCENTER | Graphics.BOTTOM);
        }
    }

    private void paintStartPage(Graphics g, int width, int top, int bottom,
        Font body, Font title) {
        int centerX = width / 2;
        int logoY = top + 8;
        int logoSize = Math.min(42, Math.max(30, width / 6));
        int logoX = centerX - (logoSize / 2);

        g.setColor(ThemeManager.ACCENT);
        g.fillArc(logoX, logoY, logoSize, logoSize, 0, 360);
        g.setColor(ThemeManager.BACKGROUND);
        g.drawArc(logoX + 5, logoY + 5, logoSize - 10, logoSize - 10, 0, 360);
        g.drawArc(logoX + (logoSize / 3), logoY + 2, logoSize / 3,
            logoSize - 4, 90, 180);
        g.drawArc(logoX + (logoSize / 3), logoY + 2, logoSize / 3,
            logoSize - 4, 270, 180);
        g.drawLine(logoX + 3, logoY + (logoSize / 2),
            logoX + logoSize - 3, logoY + (logoSize / 2));

        g.setColor(ThemeManager.HEADER_FOREGROUND);
        g.setFont(title);
        g.drawString("Netormium", centerX, logoY + logoSize + 3,
            Graphics.HCENTER | Graphics.TOP);
        g.setFont(body);
        g.setColor(ThemeManager.MUTED);
        g.drawString("Explore the web", centerX,
            logoY + logoSize + title.getHeight() + 5,
            Graphics.HCENTER | Graphics.TOP);

        int sectionY = logoY + logoSize + title.getHeight() + body.getHeight() + 15;
        g.setColor(ThemeManager.HEADER_FOREGROUND);
        g.drawString("Speed Dial", 5, sectionY, Graphics.LEFT | Graphics.TOP);
        sectionY += body.getHeight() + 5;

        BookmarkStore.Bookmark[] items = bookmarks.all();
        if (items.length == 0) {
            g.setColor(ThemeManager.MUTED);
            g.drawString("No Speed Dial entries yet.", 7, sectionY + 5,
                Graphics.LEFT | Graphics.TOP);
            g.drawString("Open a site and choose Add Bookmark.", 7,
                sectionY + body.getHeight() + 9, Graphics.LEFT | Graphics.TOP);
            return;
        }

        int columns = width >= 220 ? 2 : 1;
        int gap = 5;
        int tileWidth = (width - 10 - ((columns - 1) * gap)) / columns;
        int tileHeight = Math.max(30, body.getHeight() + 13);
        int visible = Math.min(6, items.length);
        for (int i = 0; i < visible; i++) {
            int column = i % columns;
            int row = i / columns;
            int x = 5 + column * (tileWidth + gap);
            int y = sectionY + row * (tileHeight + gap);
            if (y + tileHeight > bottom) break;
            g.setColor(ThemeManager.HEADER_BACKGROUND);
            g.fillRect(x, y, tileWidth, tileHeight);
            g.setColor(ThemeManager.BORDER);
            g.drawRect(x, y, tileWidth - 1, tileHeight - 1);
            g.setColor(ThemeManager.ACCENT);
            g.drawString(String.valueOf(i + 1), x + 5, y + 3,
                Graphics.LEFT | Graphics.TOP);
            g.setColor(ThemeManager.FOREGROUND);
            g.drawString(clip(items[i].title, body, tileWidth - 25),
                x + 19, y + 3, Graphics.LEFT | Graphics.TOP);
        }
        if (items.length > visible) {
            g.setColor(ThemeManager.MUTED);
            g.drawString("Add Bookmark to grow your Speed Dial.",
                5, Math.min(bottom - body.getHeight(), sectionY +
                3 * (tileHeight + gap)), Graphics.LEFT | Graphics.TOP);
        }
    }

    protected void pointerPressed(int x, int y) {
        ScreenMetrics metrics = new ScreenMetrics(getWidth(), getHeight());
        AdaptiveLayout layout = new AdaptiveLayout(metrics);
        int top = layout.getAddressBarHeight() + 3;
        int bottom = getHeight() - layout.getToolbarHeight() - 2;
        if (y >= top && y < bottom && sessionId.length() > 0) {
            tapPage(x, y - top);
        } else if (y < top) {
            showAddressBox();
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
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
                BookmarkStore.Bookmark[] items = bookmarks.all();
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
