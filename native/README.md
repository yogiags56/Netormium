# Netormium Native Component Lab

This directory is the first native-integration experiment for Netormium. It
builds the **upstream Google Brotli decoder** as a native library dependency.

Brotli is present in Chromium's `third_party/brotli` tree. Netormium pins the
upstream `v1.1.0` source and links only the decoder target for this milestone.
Brotli is a compression component, **not** an HTML renderer or a browser engine.
This experiment proves a real Chromium-distributed component can be built for
Android; it does not prove Blink or V8 can run on Java ME.

## Build for Android with the NDK

Requirements:
- CMake 3.22 or newer
- Android NDK with its CMake toolchain
- Git access to fetch the pinned Brotli source

From the repository root:

```sh
cmake -S native -B build/native-arm64 \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-23 \
  -DBUILD_SHARED_LIBS=OFF
cmake --build build/native-arm64 --target netormium_brotli_decoder
```

Other useful ABIs to test later are `armeabi-v7a`, `x86`, and `x86_64`.
The current target is a static-library integration point; no JNI bridge or
Android application is wired yet.

## Why this is separate from the MIDlet

CLDC 1.1 / MIDP 2.0 does not provide JNI or a standard way to load Android
`.so` libraries. A native library therefore cannot be loaded directly by the
J2ME JAR. A future Android Companion would own the native library and expose a
local bridge to the MIDlet. Feature phones without such a companion continue
to require a pure-Java path.

## Milestone status

- [x] Pin a real Chromium-distributed component and its upstream version.
- [x] Define a native CMake target for the Brotli decoder only.
- [ ] Build it in Android NDK CI for supported ABIs.
- [ ] Add decoder/JNI wrapper and bounded input/output tests.
- [ ] Connect an Android Companion to the MIDlet over a documented local API.
- [ ] Evaluate a rendering-related Chromium component separately.

## Go/no-go criteria

Do not claim Chromium rendering is available based on this milestone alone.
The next decision requires a reproducible Android NDK build, successful
Brotli decoding tests, and a working local companion-to-MIDlet bridge.
If the project cannot demonstrate a real Chromium component and a useful
integration path without a hosted server, reassess the project before
investing in a large custom rendering engine.

## Licensing

Brotli is distributed under the MIT License. Preserve its upstream
copyright and license notices in any redistribution. Review the exact
license of every additional Chromium component independently before adopting
it. See https://github.com/google/brotli/tree/v1.1.0 .
