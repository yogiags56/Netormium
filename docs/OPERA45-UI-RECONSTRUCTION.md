# Opera Mini 4.5 UI Reconstruction Blueprint

## Purpose and boundaries

Opera Mini 4.5 is the behavioral and visual reference for Netormium's compact J2ME browser experience. Netormium must be an independent implementation: do not copy Opera source code, proprietary protocols, binary assets, trademarks, or extracted artwork. Recreate common browser workflows and interaction patterns using original Netormium code and assets.

The uploaded reference archive was inspected locally. Its manifest identifies MIDlet version 4.5, MIDP-2.0, CLDC-1.0, entry point `Browser`, and icon resource `/i.png`. The archive contains obfuscated class files and a compact resource named `v`. Its embedded UI strings include Start page, Speed Dial, Bookmarks, History, Feeds, Settings, Shortcuts, Load images, Page Information, Security, Protocol, Change window size, Go to top/bottom/text start, Transmitted (session), Transmitted (total), and Help. These strings are evidence of UI vocabulary, not proof that every item is available in every build or context.

## Reference inventory

### Confirmed from current Netormium source

- Address entry and URL normalization
- Home navigation
- Back and forward history
- Reload and stop
- Multiple tabs (maximum six in current implementation)
- Bookmarks and history lists
- Data Saver toggle
- Keypad scrolling, element selection, and activation
- Remote Chromium viewport snapshots and interactive page actions
- RMS persistence for tab and history state

### Reference vocabulary to investigate and reconstruct

| Area | Reference strings / behavior to investigate | Netormium direction |
|---|---|---|
| Start page | Start page, Speed Dial | Branded home screen with editable Speed Dial tiles |
| Main menu | Menu, Shortcuts, Tools, Help | Compact menu grouped by browsing, organize, and settings |
| Navigation | Back, Forward, Reload, Stop, Enter address | Preserve keypad-first navigation and predictable softkeys |
| Organization | Bookmarks, Add Bookmark, Manage, History, Delete, Edit, Clear | Functional bookmark/history managers with safe empty states |
| Tabs/windows | Change window size, Previous addresses | Tab switcher and viewport controls; do not assume this string means tab support |
| Page controls | Load images, Go to top/bottom/text start | Per-page image loading preference and fast scroll shortcuts |
| Information | Page Information, Protocol, Address, Title, Size, Security, Secure | Lightweight page information panel based on gateway metadata |
| Data usage | Transmitted (session), Transmitted (total) | Show gateway-reported transfer totals; distinguish estimates from measured bytes |
| Settings | General, 24-hour clock, Network | Dedicated settings list with persisted preferences |
| Extras | Feeds, On-line Help, Bug report | Later phase; feeds and bug reporting require explicit gateway support |

## Proposed screen model

1. **Start Page** — Netormium globe mark, address field, Speed Dial grid/list, and compact bottom softkeys.
2. **Page View** — narrow address/title strip, remote page viewport, selection highlight, and two softkey labels.
3. **Main Menu** — vertically navigable list; first items prioritize Open, Start Page, Tabs, Bookmarks, and History.
4. **Tabs** — numbered tab list with page title/host and clear active-tab indicator.
5. **Bookmarks / History** — list screens with Open, Edit/Delete where supported, and Back.
6. **Settings** — General, Page, Network, and Data Saver sections.
7. **Page Information** — title, URL, protocol, security state, viewport, and transfer statistics when available.

## Keypad interaction principles

- Directional Up/Down scroll the page or move within a menu.
- Directional Left/Right move between actionable page elements where the current page model supports it.
- Center/Fire activates the selected item.
- Numeric shortcuts may activate numbered links/elements when displayed as available; avoid hidden, undocumented key conflicts.
- Softkey labels must reflect the current screen and action.
- Escape/back behavior must be consistent and must not silently discard typed input.
- Touch/pointer input is an enhancement, not a prerequisite.

## Implementation sequence

1. Build the home screen and branded Speed Dial presentation.
2. Replace the flat command collection with a context-aware menu and softkey model.
3. Add missing managers and edit/delete flows for bookmarks and history.
4. Add a settings screen and persisted preferences.
5. Add Page Information and transfer statistics only where gateway data is authoritative.
6. Validate layouts at small feature-phone resolutions (128x160, 176x220, 240x320) and landscape variants.
7. Test on a Java ME emulator and real devices; source-level checks alone do not establish MIDlet compatibility.

## Current implementation gap

The current `BrowserCanvas` still draws a simple address strip, page body/snapshot, status line, and Open/Links footer. Several commands are attached directly to the Canvas as MIDP Commands. The next UI pass should introduce a dedicated home/menu presentation without disrupting the working remote Chromium session lifecycle, tab persistence, or keypad interaction.
