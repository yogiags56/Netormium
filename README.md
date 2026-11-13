# Netormium 1.0 RC18 — Local Toolchain Configuration

This release candidate builds on the interactive Chromium session architecture. It is intended for controlled emulator and handset testing, not public production deployment.

## Architecture
J2ME MIDlet -> NTM/2 session API -> Node.js / Playwright -> persistent Chromium page/context.

## Features
- Persistent page session with cookies and JavaScript state kept in the same Playwright context.
- Data Saver blocks image, media and font requests while retaining HTML, CSS and JavaScript.
- Data Saver can be toggled from the J2ME command menu; changing the setting reopens the current page.
- Per-session viewport PNG cache with a five-second freshness window; interaction actions invalidate cached snapshots.
- Global snapshot-cache memory budget capped at 8 MiB with oldest-entry eviction.
- Chromium viewport PNG snapshots.
- DOM hit-target metadata for links, buttons, inputs, textareas, selects and role buttons.
- Touch/pointer tap forwarded to Chromium mouse coordinates.
- Text input forwarded to the currently focused element.
- Key forwarding for Enter, Tab, Backspace, Escape, ArrowUp and ArrowDown.
- Session expiry after 60 minutes idle; maximum 30 sessions, oldest session evicted at capacity.
- MIDP 2.0 client uses `pointerPressed`; keypad and command navigation remain available.
- Diagnostics reports device screen/platform properties and checks the gateway `/health` endpoint.
- Tab changes invalidate pending client callbacks so stale responses cannot repaint the newly active tab.

## Gateway setup
Requirements: Node.js 18+.

```sh
cd gateway
npm install
npx playwright install chromium
HOST=0.0.0.0 PORT=8787 npm start
```

For the phone, set the JAD application property `Netormium-Gateway-URL` to the gateway address reachable from the handset. The client reads this property at startup; the fallback is `http://127.0.0.1:8787`. Do not use `127.0.0.1` on a phone unless the Gateway runs on that same device.

### Java ME client build
The source tree includes an Ant build file. A Java ME SDK or Wireless Toolkit installation is required because ordinary Java SE libraries do not provide MIDP/CLDC APIs or the `preverify` tool.

First copy `build.properties.example` to `build.properties` and edit the SDK paths. Then run the toolchain preflight:

```sh
ant doctor
```

The same check can be run from `gateway/` with `npm run check:j2me-toolchain`. It checks Java, javac, Ant, the CLDC/MIDP API JARs, the preverifier executable and whether the compiler advertises the configured legacy source level. The preflight is diagnostic only; passing it does not replace an actual compile or device test.

For a modern JDK where `javac` no longer supports the project's legacy levels, use an ECJ release that still supports Java 1.3 / classfile 1.1:

```properties
compiler.backend=ecj
ecj.jar=/absolute/path/to/ecj.jar
```

The ECJ backend still needs the real CLDC and MIDP API JARs and the Java ME `preverify` utility.

Reference toolchains:
- [Oracle Java ME downloads and reference implementations](https://www.oracle.com/java/technologies/mobile-devices-downloads.html)
- [Oracle Java archive — Sun Java Wireless Toolkit 2.5.2_01 and MIDP 2.0 reference packages](https://www.oracle.com/java/technologies/java-archive-downloads-javame-downloads.html)
- [Eclipse batch compiler command-line options](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/tasks/task-using_batch_compiler.htm)

The Oracle archive contains legacy installers and source/reference packages; check the applicable license and host OS requirements before using them. The Eclipse batch compiler documentation describes the standalone entry point `org.eclipse.jdt.compiler.batch.BatchCompiler` and the legacy `-1.3` mode, which implies source 1.3 and classfile target 1.1. The toolchain checker verifies that ECJ starts and advertises `-1.3`, but only a real build proves that the selected ECJ version works with the project's source and APIs.

When all dependencies are installed, build with:

```sh
ant -propertyfile build.properties clean package
```

The package target creates `dist/Netormium.jar` and `dist/Netormium.jad`, including the calculated JAR size. The JAD template contains a sample LAN Gateway URL; change it before distributing the build. Use a Java ME-compatible JDK/toolchain that supports the configured legacy source and target levels. Modern JDKs may no longer support the Java 1.3 source level used by this legacy client.

### Gateway release checks
```sh
cd gateway
npm install
npm test
npm run check:release
```

The Gateway reads `HOST` and `PORT` from process environment variables. `.env.example` is documentation only; Node.js does not load it automatically. On a trusted LAN, bind to the server's LAN interface only when needed. Do not expose this prototype directly to the public internet.


## RC3 cancellation endpoint
- `GET /v1/session/cancel?rid=...` cancels an in-flight `/v1/session/open` request using the request ID supplied by the client. A `200` response means the pending request was found and cancellation was requested; `404` means it was no longer pending.
- Cancellation closes the temporary Playwright context. The open request then fails with a cancellation error and does not register a persistent session.

## RC4 network recovery behavior
- Health, metadata and snapshot reads receive at most one retry for network failures and HTTP 5xx responses, with a short delay.
- Session creation and interactive actions are not automatically retried because a lost response could otherwise create duplicate sessions or repeat a click/text action.
- HTTP 404 is treated as an expired or unknown session. The MIDlet reopens the saved URL only after this response; transient network errors do not erase the saved session ID.
- PNG viewport data must include the PNG signature and IHDR marker before the client accepts it or the Gateway caches it.


## RC5 performance and memory controls
- Gateway snapshot cache budget reduced from 12 MiB to 8 MiB and limited to two cached viewport images per session.
- Replacing a cached scroll-position snapshot now subtracts the old buffer from global cache accounting before storing the new one.
- NTM/2 metadata is bounded: page text 6,000 characters, title 100, URL 1,024, up to 24 interactive elements, labels 72 and element URLs 256 characters.
- J2ME canvas drops its reference to the previous decoded viewport image before decoding the next snapshot, reducing overlapping full-image retention.
- PNG remains the only default snapshot format; no optional JPEG dependency is introduced.

## RC11 ECJ invocation and SDK acquisition
- Corrected the ECJ batch compiler main class to `org.eclipse.jdt.compiler.batch.BatchCompiler`, matching the current Eclipse JDT batch compiler documentation.
- Changed the Ant ECJ source argument to the source directory. This avoids space-splitting source file paths and lets ECJ recursively compile the source tree.
- Added focused tests for the documented ECJ entry point and the legacy `-1.3` capability check.
- Oracle's archived Sun Java Wireless Toolkit 2.5.2_01 documents CLDC 1.1 and MIDP 2.0 support. Its listed host requirements are Windows XP or Linux x86 and Java SE 1.5 for development, so installation on a modern 64-bit environment may require a legacy VM or a separate compatible build host.

## RC10 legacy Java compiler backend
- Added an optional Eclipse Compiler for Java (ECJ) backend to the Ant build. Configure `compiler.backend=ecj` and `ecj.jar=/path/to/ecj.jar` in `build.properties` to compile legacy Java ME sources using ECJ's `-1.3` mode, which targets classfile 1.1.
- The default `javac` backend remains available for legacy JDK installations.
- `ant doctor` now checks the selected compiler backend and, for ECJ, validates the ECJ archive and probes its batch compiler help output for legacy mode support.
- ECJ is a compiler only; CLDC/MIDP APIs and the Java ME preverifier are still required.

## RC9 Java ME SDK validation and build gate
- Strengthened toolchain preflight: CLDC/MIDP inputs must have ZIP/JAR signatures; preverifier must be a regular executable file on POSIX systems; configured javac source and target levels are checked separately.
- Relative SDK paths are resolved against the selected `build.properties` file, so `ant doctor` and npm invocation use consistent path semantics.
- Added automated tests for property parsing, relative path resolution, archive detection and executable permissions.
- A successful preflight is only a toolchain candidate; actual compile, preverification and device validation remain mandatory.

## RC8 Java ME toolchain preflight
- Added a dependency-free Node.js preflight that checks Java, javac, Ant, CLDC/MIDP API JAR paths, the Java ME preverifier and configured legacy compiler support.
- Added the `ant doctor` target and `npm run check:j2me-toolchain` command. These checks report readiness but do not claim a successful Java ME build.

## RC7 build and deployment preparation
- Added an Ant build file with compile, preverify and package targets for a Java ME SDK/WTK toolchain.
- Added a JAD template for MIDP 2.0 / CLDC 1.1 and a build target that writes the generated JAR size.
- Gateway base URL can now be supplied through the MIDlet JAD property `Netormium-Gateway-URL`; the localhost value is only a development fallback.
- Added a release consistency script checking the Gateway package version, banner, health response, Java ME User-Agent and JAD profile settings.
- Documented that `.env.example` is not auto-loaded by Node.js and that environment variables must be exported or supplied by a service manager.

## RC6 compatibility and viewport validation
- Client source audit confirms no Java generics, enhanced-for loops, StringBuilder, java.nio, java.util.concurrent or try-with-resources constructs are used.
- Gateway viewport parsing now falls back safely for malformed dimensions and clamps width to 128–640 and height to 128–960 pixels.
- Gateway and J2ME PNG validation now check IHDR width and height, reject zero/oversized dimensions, and enforce a 614,400-pixel ceiling before caching or image decoding.
- Java ME source remains written in a CLDC-friendly style; a full MIDP/CLDC SDK compilation is still required before device release.

## NTM/2 endpoints
- `GET /v1/session/open?url=...&w=240&h=250&ds=1` creates a browser session; `ds=1` enables Data Saver. Returns NTM/2 metadata.
- `GET /v1/session/snapshot?id=...&y=0` returns a PNG viewport for the existing session.
- `GET /v1/session/tap?id=...&x=...&y=...` clicks at viewport coordinates and returns refreshed metadata.
- `GET /v1/session/type?id=...&text=...` inserts text into the currently focused browser element.
- `GET /v1/session/key?id=...&key=Enter` forwards a limited keyboard key.
- `GET /v1/session/close?id=...` closes a session.

NTM/2 records:
```text
NTM/2
SESSION<TAB>session-token\nDATASAVER<TAB>1-or-0
URL<TAB>percent-encoded-url
TITLE<TAB>percent-encoded-title
TEXT<TAB>percent-encoded-page-text
ELEMENT<TAB>index<TAB>kind<TAB>x<TAB>y<TAB>w<TAB>h<TAB>label<TAB>href<TAB>input-type<TAB>focused-0-or-1
END
```

## 1.0 RC4 — Network Resilience & Session Recovery
- Added one bounded retry for idempotent health, metadata and snapshot requests after network errors or HTTP 5xx responses. Interactive actions and session creation are not retried to avoid duplicate side effects.
- Added explicit expired-session classification for HTTP 404 responses.
- Startup session restoration now preserves the saved session ID when the Gateway is temporarily unreachable; only expired sessions are cleared and reopened.
- Snapshot, metadata, tap, text-entry and key-action failures trigger URL-based recovery only when the Gateway confirms the session is expired.
- Added PNG signature/IHDR validation in the J2ME client and Gateway before accepting or caching viewport data.
- Added Node.js unit tests for Gateway PNG response validation.
- Updated client identity to RC4.

## 1.0 RC3 — Runtime Integration & Reliability
- Added request IDs to session-open calls and a `/v1/session/cancel?rid=...` endpoint.
- The Gateway tracks in-flight session creation; cancellation closes its temporary Chromium context and prevents a cancelled navigation from becoming a persistent session.
- The MIDlet Stop action now asks the Gateway to cancel the currently opening session as well as invalidating the client callback token.
- Added automated Node.js tests for pending-navigation registration, cancellation, completion and unknown IDs.
- Corrected the RC2 Java parser line-ending defect in the incomplete-response validation block.
- Updated the health endpoint and client identity to RC3.

## 1.0 RC2 — Compatibility & Stability
- Tab creation and tab switching now invalidate outstanding client requests before changing active-tab state, preventing late responses from the previous tab from overwriting the newly selected tab.
- Added a Diagnostics command reporting screen dimensions, Java ME platform/configuration/profile properties, and a live gateway health check.
- Updated gateway health response and Java ME User-Agent to identify RC2.
- Improved detection of incomplete NTM/2 metadata responses.

## 1.0 RC1 — First Release Candidate
- Data Saver preference is now persisted in RMS and restored on MIDlet startup.
- MIDlet pause and destroy lifecycle hooks persist tab state and browser preferences.
- History chooser includes Clear History; history is cleared locally and the empty state is saved.
- Added a compact versioned preferences record to RMS.
- Existing per-tab Chromium sessions, six-tab limit, bookmark storage, history persistence, Data Saver and PNG viewport rendering are retained.

## 0.9 session persistence and history
- Each tab retains its own Chromium session ID; switching tabs reattaches to its existing browser context.
- Tab descriptors (URL, title, scroll offset, session ID and active tab) are persisted in MIDP RMS.
- On startup, the active saved session is validated through the gateway; expired sessions fall back to reopening the saved URL.
- Browsing history is persisted in MIDP RMS and can be opened from the History command.
- Stop command invalidates the current client navigation request; stale gateway responses are discarded and newly-created sessions are closed when the gateway responds. It does not yet interrupt an in-progress Chromium navigation at the server.
- Data Saver preference is retained across MIDlet restarts.
- Pause/destroy lifecycle callbacks persist tab and preference state.
- Clear History removes locally stored browsing history.
- Gateway idle session lifetime increased to 60 minutes to support background tabs.

## 0.8 browser UX and tab management
- Up to six lightweight tabs with tab switcher and next-tab command.
- Tabs retain their URL, title and scroll offset; switching reloads the saved URL.
- Bookmarks persist locally using MIDP Record Management System (RMS).
- Bookmark list can reopen saved pages.
- Text-entry dialog Send action forwards text to the focused Chromium element.

## 0.7 navigation and stability
- Left/right keypad actions cycle through extracted interactive page elements.
- FIRE / center-select / keypad 5 activates the selected element; if no element is selected, it opens the address field.
- A double outline indicates the currently selected interactive element over the PNG viewport. The client refreshes DOM element coordinates after viewport scrolling so keypad activation stays aligned.
- Gateway session errors use the NTM/2 `ERROR` field so the MIDlet can show a useful message rather than a protocol parsing failure.
- Additional browser keys are allowed through the gateway: ArrowLeft, ArrowRight, Home and End.
- HTTP transport failures now report status codes when available.

## Data Saver behavior
Data Saver blocks Chromium requests of resource types `image`, `media` and `font`. It does not rewrite HTML or CSS and does not guarantee a fixed percentage of data reduction. Some sites may appear incomplete when images or web fonts are blocked. PNG viewport snapshots remain enabled for MIDP 2.0 compatibility.

Snapshot caching is short-lived and per session. Tap, text-entry and key actions invalidate it. This is a performance optimization, not persistent offline storage.

## RC2 validation status
- Gateway JavaScript syntax checked with Node.js.
- Java source changes reviewed for MIDP 2.0 / CLDC 1.1 API usage; a full MIDP SDK build and handset/emulator test have not yet been run.
- Live Playwright/Chromium session testing has not yet been run.

## Security and deployment warning
This is a development prototype, not a public production proxy. It has no user authentication or TLS configuration. Session tokens are bearer capabilities. Do not expose it to the public internet or use it for banking, private accounts, or sensitive sessions. The hostname filter is only a basic guard and is not a complete SSRF defense: production must resolve and reject private/reserved IPs for every navigation, redirect and subresource, isolate browser processes, add authentication, TLS, rate limits, quotas, and redact query strings from logs.

## Known limitations
- BrowserCanvas pointer hit-testing works for touch-capable MIDP devices. Keypad-only devices can activate the first nine extracted interactive elements with keys 1–9; a richer focus-navigation overlay is planned.
- Text entry is a basic input dialog; the Enter command forwards Enter to the active browser element. Rich text editing, file inputs, native select menus and virtual keyboard UX are not complete.
- The client currently displays a rasterized page; clickable regions are handled by sending touch coordinates to Chromium. The J2ME client does not implement a local DOM or CSS layout engine.
- The session API uses GET for simplicity in this prototype. Production should use POST for text/action payloads and protect tokens.
- Full-page screenshot streaming, downloads and advanced accessibility are not implemented. Tabs retain independent Playwright browser contexts while their gateway sessions remain alive. Session IDs are stored locally; if the gateway restarts or a session expires, the client falls back to reloading the saved URL. Background sessions expire after 60 minutes idle.


## RC12 — Java ME toolchain acquisition

See [`docs/J2ME-TOOLCHAIN-ACQUISITION.md`](docs/J2ME-TOOLCHAIN-ACQUISITION.md) for the pinned ECJ acquisition helper, SDK provenance, and real-build release gates. ECJ download is verified against Eclipse's published SHA-512. CLDC/MIDP APIs and the preverifier must still come from a genuine licensed Java ME SDK installation.


## RC15 — Stable ECJ and Java ME API verification

RC15 pins the standalone compiler to Eclipse 4.41 GA, validates its SHA-512 and performs a launch probe for `-1.3` compatibility. `ant doctor` / `check:j2me-toolchain` also inspect expected CLDC and MIDP API classes, not just the archive signatures. See [`docs/J2ME-TOOLCHAIN-ACQUISITION.md`](docs/J2ME-TOOLCHAIN-ACQUISITION.md).


## RC15 — Java ME SDK discovery and build readiness

RC15 adds `npm run discover:j2me-sdk` to locate an existing local WTK/Java ME SDK without downloading licensed vendor binaries. The MIDP archive check now includes RMS persistence classes used by the browser. Run discovery first, configure `build.properties`, then run `npm run check:j2me-toolchain` and `ant doctor`.


## RC15 — MIDlet compilation and package verification

RC15 makes the ECJ build pass the complete Java source file set (rather than a source-directory token), clears stale preverification output, and adds `verify-package` / `release` targets. The package validator checks the MIDlet entry class, manifest/JAD required attributes, profile/configuration, JAR size, metadata consistency, and excludes bundled Java ME platform classes. Run `ant -propertyfile build.properties clean release` after the genuine toolchain is installed.


## RC18 — Local toolchain configuration


## RC18 — Local Java ME toolchain configuration

RC18 changes the acquisition strategy: vendor SDKs are installed locally by the developer after reviewing their license. Netormium does not attempt to download or redistribute the legacy Oracle SDK. The new `configure:j2me-toolchain` helper validates the extracted SDK's CLDC and MIDP class inventories, executable preverifier, and a locally supplied ECJ JAR before writing `build.properties`.

Example (after installing/extracting a compatible WTK/Java ME SDK and obtaining ECJ):

```sh
cd gateway
node scripts/configure-local-toolchain.js --root /absolute/path/to/WTK --ecj /absolute/path/to/ecj.jar --output ../build.properties
cd ..
ant -propertyfile build.properties clean release
```

The helper never writes a partial configuration. It does not download vendor binaries, bypass license acceptance, or claim a successful MIDlet build. Oracle's archive provides Java ME SDK 3.0 and WTK 2.5.2_01; Oracle documents WTK's CLDC 1.1 and MIDP 2.0 support.

## Developing from an Android phone

See [`docs/ANDROID-ONLY-WORKFLOW.md`](docs/ANDROID-ONLY-WORKFLOW.md) for the
Termux setup, Android-side source tests, GitHub Codespaces workflow, and the
remaining genuine Java ME toolchain requirements.
