# Changelog

## 1.0.0-rc18
- Pivoted away from repeated automatic compiler downloads to explicit local Java ME SDK installation.
- Added a local toolchain configuration helper that validates CLDC/MIDP API class inventories, preverifier permissions, and ECJ before writing build.properties.
- Added tests proving incomplete or invalid toolchains do not produce partial configuration.
- Updated Gateway and Java ME client release identity to RC18.
- No MIDlet JAR/JAD build is claimed; actual SDK and ECJ are still unavailable in this environment.


## 1.0.0-rc17
- Added an official Eclipse archive mirror fallback for pinned ECJ 4.41 acquisition; both mirrors use the same SHA-512 and runtime legacy-mode probe.
- Improved acquisition diagnostics so DNS, HTTP, archive-integrity and compiler-launch failures are reported per mirror before the final failure.
- Retained strict Java ME SDK/API/preverifier gates; no fabricated toolchain or MIDlet package is accepted.


## 1.0.0-rc7
- Added Ant compile/preverify/package targets for Java ME SDK toolchains, using separate CLDC and MIDP API jars.
- Added a MIDP 2.0 / CLDC 1.1 JAD template and automatic JAR size insertion.
- Added configurable Gateway URL through the MIDlet app property `Netormium-Gateway-URL`.
- Added release consistency checks across Gateway, Java ME client and JAD metadata.
- Documented Gateway environment configuration and deployment limitations.


## 1.0.0-rc6
- Added safe viewport dimension parsing with fallback values and 128–640 × 128–960 bounds.
- Added Gateway-side and client-side PNG IHDR dimension and pixel-count validation before caching or MIDP image decoding.
- Added automated viewport boundary and malformed-input tests.
- Audited Java sources for common post-CLDC language/API constructs; full SDK compilation remains pending.


## 1.0.0-rc5
- Reduced global PNG snapshot cache budget to 8 MiB and limited each session to two cached viewport images.
- Fixed cache accounting when replacing an existing scroll-position entry.
- Added explicit NTM/2 metadata bounds for text, title, URL, interactive element count and element strings.
- Released the previous canvas viewport image reference before decoding the next snapshot.
- Added metadata-bound unit tests; retained PNG as the MIDP 2.0-safe default.


## 1.0.0-rc4
- Added a bounded retry for idempotent health, metadata and snapshot requests after network errors/HTTP 5xx.
- Added expired-session classification and URL-based recovery across startup, snapshot, tap, text-entry and key-action flows without discarding sessions on temporary network failures.
- Added PNG signature/IHDR validation on client and Gateway.
- Added automated PNG response validation tests.
- Updated Java ME client identity to RC4.


## 1.0.0-rc3
- Added request-ID based server-side cancellation for in-flight session creation.
- Stop now requests cancellation of the active Gateway open operation in addition to discarding stale client callbacks.
- Added automated tests for pending-navigation registry behavior.
- Corrected an invalid escaped-newline sequence in the RC2 Java response parser.
- Updated Gateway health identification and client User-Agent to RC3.


## 1.0.0-rc2
- Fixed stale asynchronous responses crossing tab-switch and new-tab boundaries by invalidating the active request token before switching state.
- Added device and gateway diagnostics screen.
- Updated gateway health identification and client User-Agent.
- Added incomplete NTM/2 response detection.


## 1.0.0-rc1
- Persisted Data Saver preference in a versioned RMS record.
- Added Clear History action to the History chooser.
- Added explicit history clearing and persistence.
- Added MIDlet pause/destroy persistence hooks for tabs and preferences.
- Retained the independent per-tab Chromium sessions and six-tab limit.
- Release candidate is prepared for controlled emulator/handset validation; no device test is claimed.

# Changelog

## 1.0.0-rc18
- Pivoted away from repeated automatic compiler downloads to explicit local Java ME SDK installation.
- Added a local toolchain configuration helper that validates CLDC/MIDP API class inventories, preverifier permissions, and ECJ before writing build.properties.
- Added tests proving incomplete or invalid toolchains do not produce partial configuration.
- Updated Gateway and Java ME client release identity to RC18.
- No MIDlet JAR/JAD build is claimed; actual SDK and ECJ are still unavailable in this environment.


## 0.9.0
- Added per-tab Chromium session IDs and session reattachment when switching tabs.
- Added RMS persistence for tab URLs, titles, session IDs, scroll positions and active-tab index.
- Added startup validation of the active saved session with URL-reload fallback for expired sessions.
- Added persistent browsing history and a History chooser.
- Added Stop command to invalidate the current client navigation request and discard stale responses; server-side navigation cancellation remains future work.
- Increased gateway idle session lifetime from 10 to 60 minutes for background tabs.
- Preserved six-tab manager, RMS bookmarks, Data Saver and snapshot caching.

## 0.8.0
- Added a six-tab lightweight tab manager with tab list, new tab and next tab commands.
- Tab entries retain URL, title and scroll offset; switching reloads the saved URL.
- Added persistent bookmarks using MIDP RMS.
- Added bookmark list and open-bookmark workflow.
- Fixed text-entry dialog Send action to forward text to the active Chromium session.
- Preserved Data Saver, viewport cache and keypad focus navigation.

## 0.7.0
- Added keypad left/right focus cycling through interactive DOM elements.
- Added selected-element highlight overlay and FIRE/keypad-5 activation.
- Added NTM/2 ERROR protocol responses for session-open and interactive action failures.
- Added support for ArrowLeft, ArrowRight, Home and End gateway key events.\n- Added a metadata refresh endpoint so DOM hit targets stay aligned after scrolling, including cache-hit scroll restoration.
- Improved client-side HTTP error diagnostics and gateway retry messaging.
- Preserved Data Saver mode and five-second snapshot cache from 0.6.

## 0.6.0
- Added optional Data Saver mode to block image, media and font requests at the gateway.
- Added a J2ME command-menu toggle; changing the mode reopens the current page.
- Added a five-second per-session PNG snapshot cache with interaction invalidation.
- Added a 12 MiB global cache-memory ceiling and oldest-entry eviction.
- Added the NTM/2 DATASAVER metadata flag.

## 0.5.0
- Added persistent Chromium sessions and session expiry.
- Added DOM interactive-element extraction with viewport coordinates.
- Added touch tap forwarding, focused text input and selected key forwarding.
- Updated J2ME BrowserCanvas to use session-based rendering and pointer input.
- Added NTM/2 protocol and deployment/security documentation.

## 0.4.0
- Added PNG viewport snapshot endpoint.
- Added J2ME PNG snapshot client and visual page rendering.

## 1.0 RC8 — Java ME Toolchain Preflight
- Added `check-j2me-toolchain.js` to report Java, javac, Ant, CLDC/MIDP API JAR, preverifier and legacy source-level readiness.
- Added `ant doctor` and `npm run check:j2me-toolchain` entry points.
- Clarified the order of SDK configuration, preflight and actual packaging; diagnostic success is not treated as proof of a valid MIDlet binary.

## 1.0 RC9 — SDK Validation & Build Gate
- Strengthened Java ME SDK preflight to validate ZIP/JAR signatures, preverifier executable permissions, and javac source and target support independently.
- Relative SDK paths now resolve relative to the configured properties file.
- Added five unit tests for the toolchain validation helpers.
- Updated Gateway and Java ME client runtime identity to RC9.

## 1.0 RC10 — Legacy Compiler Backend
- Added optional Eclipse Compiler for Java (ECJ) Ant backend for legacy Java 1.3 syntax and classfile 1.1 output on modern host JDKs.
- Added selected-backend checks to the Java ME toolchain preflight, including ECJ archive validation and a batch compiler capability probe.
- Kept javac as the default backend; real MIDP/CLDC compilation and preverification remain gated on the SDK toolchain.

## 1.0 RC11 — ECJ Invocation & SDK Acquisition
- Corrected the ECJ batch compiler main class to `org.eclipse.jdt.compiler.batch.BatchCompiler`.
- Changed ECJ compilation input to the source directory to avoid fragile space-separated source file arguments.
- Added tests for the documented ECJ entry point and legacy-mode capability gate.
- Documented the official Oracle WTK 2.5.2_01 archive and its legacy host requirements.

## 1.0.0-rc12
- Added a pinned ECJ acquisition helper with SHA-512 and ZIP/JAR validation.
- Added Java ME SDK acquisition/provenance documentation for CLDC 1.1, MIDP 2.0, and preverifier setup.
- Added an explicit real-compile/preverification/release gate; no MIDlet binary is claimed without genuine SDK inputs.
- Updated Gateway package version to 1.0.0-rc12.

## 1.0.0-rc13
- Replaced the weekly ECJ build pin with the stable Eclipse 4.41 GA compiler artifact.
- Added an ECJ runtime probe requiring successful batch compiler startup and the `-1.3` legacy mode before installation.
- Added CLDC/MIDP API archive inventory checks for expected framework classes.
- Expanded toolchain tests for the API class inventory and acquisition probe.
- Updated all Gateway and Java ME client release identities to 1.0.0-rc13.

## 1.0.0-rc14
- Added local Java ME SDK/WTK discovery for common installation roots and explicit `--root` paths.
- Added `npm run discover:j2me-sdk` without automatic download or license acceptance of Oracle binaries.
- Expanded MIDP API inventory checks to include RMS `RecordStore` and `RecordEnumeration` used by the client.
- Updated Gateway and Java ME client identities to 1.0.0-rc14.

## 1.0.0-rc15
- Changed the ECJ Ant target to pass the complete Java source file set and sourcepath.
- Clears stale preverification output before each preverifier run.
- Added JAR/JAD structural validation and Ant `verify-package` / `release` targets.
- Validates MIDlet entry class, required manifest/JAD attributes, JAR size, metadata consistency, MIDP/CLDC declarations, and excludes bundled platform API classes.
- Updated Gateway and Java ME client release identities to 1.0.0-rc15.


## 1.0.0-rc16

- Updated Gateway, Java ME User-Agent, and release consistency checks to RC16.
- Re-ran genuine ECJ and Java ME SDK acquisition/build readiness checks.
- Preserved strict release gates: no synthetic or placeholder MIDlet package is represented as installable.

## 1.0.0-rc19 — Android-first development workflow

- Added Termux setup, diagnostics and source-test scripts for Android-only development.
- Added browser-based GitHub Codespaces development configuration.
- Added GitHub Actions source-validation workflow.
- Documented the separation between Android source work and genuine Java ME release build.
- Clarified that legacy Linux preverifier binaries may not execute on Android/Bionic.
- Updated active Gateway and Java ME User-Agent release identities to RC19.
