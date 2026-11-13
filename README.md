# Netormium 1.0.0-rc21

Netormium is a J2ME browser inspired by Opera Mini 4.3. It uses a lightweight
Java ME MIDlet on feature phones and a remote Chromium/Playwright gateway to
process modern web pages.

## Architecture

`J2ME MIDlet -> NTM/2 session API -> Node.js / Playwright -> Chromium`

The gateway renders the page remotely and returns PNG viewport snapshots plus
compact interaction metadata. The MIDlet forwards keypad, tap, and text actions.
Chromium does not run on the feature phone.

## Browser-only development

The project is designed to be maintained from an Android phone using a browser:
- GitHub website for repository management and small source edits.
- GitHub Actions for cloud validation when hosted runners are available.
- GitHub Codespaces for browser-based VS Code and remote terminal work, if
  available to the account.

**Termux and a local PC are not required.** Follow
[`docs/BROWSER-ONLY-WORKFLOW.md`](docs/BROWSER-ONLY-WORKFLOW.md).

## Gateway setup

Requirements: Node.js 18+ and Playwright Chromium.

```sh
cd gateway
npm install
npx playwright install chromium
HOST=0.0.0.0 PORT=8787 npm start
```

Set the JAD property `Netormium-Gateway-URL` to the gateway address reachable
from the handset. Do not use `127.0.0.1` on a phone unless the gateway runs on
that same device.

## Source validation

```sh
cd gateway
npm test
npm run check:release
cd ..
ant -projecthelp
```

The cloud validation workflow checks the Node.js gateway and Ant project
definition. It does not prove that a Java ME MIDlet has been compiled.

## Genuine Java ME release requirements

An installable MIDlet still requires a genuine CLDC 1.1 API, MIDP 2.0 API,
compatible ECJ compiler, and Java ME preverifier. Once a legally obtained
toolchain is configured, run:

```sh
ant doctor
ant clean release
```

Until compilation, preverification, JAR/JAD validation, and emulator or handset
testing all pass, Netormium does not claim to provide an installable release.

## Security warning

The gateway is a development prototype, not a public production proxy. Do not
expose it to the public internet or use it for banking or sensitive accounts
without authentication, TLS, robust SSRF protections, process isolation,
rate limits, quotas, and appropriate data handling.

## Baseline contents

This repository contains the 68 source and documentation files from the
Netormium 1.0 RC20 Browser-Only Development archive. The original archive is
retained under `release/` for provenance.
