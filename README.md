# Netormium 1.0.0-rc21

Netormium is a J2ME browser inspired by Opera Mini 4.5. It uses a lightweight
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
from the handset. Do not use `127.0.0.1` or the sample private-LAN address on a
phone unless the gateway is actually running at that address on the same LAN.

**Important: the repository does not host a live gateway.** Building the JAR/JAD
alone does not provide internet access. You must deploy the Node.js gateway on
a reachable server, install Playwright Chromium there, allow inbound TCP on the
chosen port, and configure the exact public/LAN URL in the JAD or Settings.
For a phone on mobile data, a private address such as `192.168.x.x` will not
work; use a public HTTPS endpoint or a properly secured reachable server.

The gateway binds to `0.0.0.0` by default so a phone on the same network can
reach it. This is a network-listening default, **not** a recommendation to expose
the prototype openly. Before any public exposure, add TLS, authentication,
per-user limits, process isolation and outbound network controls as described
in [Gateway security](docs/GATEWAY-SECURITY.md).

Quick connection check from the phone's browser:
`http(s)://YOUR_GATEWAY_HOST:8787/health`
A working service returns `Netormium Gateway 1.0.0-rc21 OK`.

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
