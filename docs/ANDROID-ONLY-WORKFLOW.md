# Netormium development using only an Android phone

This workflow assumes no personal computer is available. It separates Android-side
source work from the genuine Java ME release build.

## Recommended division of work

1. **Android + Termux:** inspect/edit source, run Node.js unit tests, check release
   identity, manage Git, and collect diagnostics. Do not expect Termux to run the
   old WTK Linux preverifier: many legacy SDK tools are native Linux/glibc binaries,
   while Android uses Bionic.
2. **Browser-based cloud environment:** use GitHub Codespaces or GitHub Actions for
   reproducible Linux builds and validation. Codespaces can be opened in a browser
   and provides a cloud development environment. Availability, account limits and
   billing depend on the user's GitHub plan/settings.
3. **Java ME SDK payload:** the real CLDC 1.1 API, MIDP 2.0 API, ECJ and compatible
   preverifier must still be supplied from a legally obtained SDK/compiler package.
   Do not commit proprietary SDK binaries to a public repository. Upload them only
   to a private Codespace workspace or other private storage permitted by their
   license. Never put account passwords or access tokens in source files.

## A. Install Termux

- Install Termux from the official F-Droid listing or official Termux GitHub releases.
- Do not mix APKs/plugins from different signing sources; their signatures differ.
- Grant shared-storage permission only if you need to copy project ZIPs from Downloads.
- Android may stop long-running background processes. Keep Termux in foreground during
  package installation and tests, and avoid battery optimization for long sessions.

Official links:
- https://f-droid.org/packages/com.termux/
- https://github.com/termux/termux-app/releases

## B. Install Android-side tools

Open Termux and run:

```sh
pkg update
pkg upgrade -y
pkg install -y git nodejs-lts openjdk-21 ant unzip zip
```

Extract the Netormium project ZIP into your Termux home directory. If the ZIP is in
Android Downloads, first run `termux-setup-storage`, then use the path under
`~/storage/downloads/`.

From the extracted project root:

```sh
bash android/termux/doctor.sh
bash android/termux/test.sh
```

The test command runs the Node built-in test suite and release identity checks. It
does not require installing Playwright or downloading Chromium.

## C. Cloud development from the phone browser

1. Sign in to GitHub in the Android browser.
2. Create a **private** repository for Netormium.
3. Upload the extracted project files from the phone, or use GitHub's web upload flow.
4. Open the repository's **Code → Codespaces → Create codespace on main** flow.
5. Wait for `.devcontainer/devcontainer.json` to install Node.js, Java 21 and Ant.
6. In the Codespaces terminal run:

```sh
cd gateway
node --test test/*.test.js
node scripts/check-release.js
cd ..
ant -projecthelp
```

GitHub's browser-based Codespaces editor supports editing, terminal commands, Git,
and running project tasks. Stop the Codespace when finished to avoid unnecessary
usage. Check current quota/billing information in GitHub settings before creating it.

## D. Genuine Java ME build gate

The following inputs are still mandatory before `ant release` can produce a real
installable MIDlet:

- ECJ batch compiler JAR that passes the pinned SHA-512 and legacy `-1.3` probe;
- genuine CLDC 1.1 API JAR;
- genuine MIDP 2.0 API JAR;
- a preverifier executable compatible with the cloud Linux environment.

Place the legally obtained SDK/compiler files in a private, untracked directory in
the Codespace (for example `toolchain/local/`). Then configure the existing helper:

```sh
node gateway/scripts/configure-local-toolchain.js --help
```

Use its documented arguments to create `build.properties`, then run:

```sh
ant doctor
ant clean release
```

If the WTK installer or preverifier is Windows-only, do not try to run it in Linux
or Termux. We must find a compatible Linux distribution or a legally usable
alternative toolchain. A passing source test is not a Java ME MIDlet build.

## What this workflow does not claim

- Termux is not a full desktop Java ME SDK.
- OpenJDK 21 does not by itself support Java ME's legacy source/bytecode target.
- The cloud workflow does not contain Oracle's proprietary SDK or ECJ automatically.
- No JAR/JAD is installable until real API compilation, preverification and package
  validation all pass.
