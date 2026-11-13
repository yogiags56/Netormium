# Netormium Chromium Component Porting Plan

## Objective

Netormium must integrate at least one genuine component distributed in
Chromium's source tree. Merely copying Chromium's design ideas or writing an
unrelated HTML renderer is not sufficient to satisfy this project objective.

## Platform boundary

- The browser shell remains a MIDP 2.0 / CLDC 1.1 MIDlet.
- J2ME does not provide JNI. A native Android library cannot be loaded by the
  MIDlet JAR on a normal feature phone.
- Native Chromium components must initially live in a separate Android
  Companion process/application.
- The Companion may expose a loopback/local HTTP or socket API to a MIDlet
  emulator when the emulator supports that connectivity.
- A pure J2ME device without a companion remains a separate compatibility
  target; it must not be represented as running native Chromium.

## Component selection

### Milestone 1: Brotli decoder

Brotli is included under Chromium's `third_party/brotli` and is independently
buildable. Its upstream implementation is C and is distributed under the MIT
License. Netormium's `native/CMakeLists.txt` pins Google Brotli v1.1.0 and
exposes the decoder as `netormium_brotli_decoder`.

This is a genuine component integration, but Brotli only decompresses data.
It does not parse HTML, execute JavaScript, calculate CSS layout, or render
pages. Its value is establishing the native build and licensing workflow.

### Later candidates

- Skia: investigate a small, isolated rendering use case and dependencies.
- Blink: investigate architecture and minimum dependency closure before any
  port attempt. A direct CLDC port is not assumed feasible.
- V8: treat as a separate high-risk project; do not promise it can run on J2ME.
- Android System WebView: possible Companion rendering backend, but it is
  Chromium-based Android infrastructure, not a Blink/V8 port to MIDP.

## Acceptance gates

1. Android NDK builds the pinned Brotli decoder for at least arm64-v8a.
2. A native test verifies known Brotli compressed fixtures and rejects
   oversized output safely.
3. An Android Companion loads the library and exposes a bounded local API.
4. The MIDlet emulator can reach that local API without an internet-hosted
   gateway.
5. A separate feasibility report determines whether a rendering-related
   Chromium component can be integrated at useful size and memory cost.

Do not mark the project complete merely because CMake configures. Record
actual CI build results and runtime tests.

## Project decision

The current work is an experiment, not a guarantee that Chromium's full
rendering stack can be ported to J2ME. If the acceptance gates fail, stop and
reassess honestly rather than silently replacing the goal with an unrelated
custom browser engine.
