# Netormium

A J2ME browser inspired by Opera Mini 4.3.

Netormium uses a Java ME client for feature phones and a remote Chromium/Playwright gateway for modern web rendering. The phone receives PNG viewport snapshots and DOM interaction metadata; Chromium does not run on the handset.

## Development model

This repository is intended to be developed from an Android phone using a browser only:
- GitHub web interface for repository management and small edits.
- GitHub Actions for automated source validation on cloud runners.
- GitHub Codespaces (when available to the account) for browser-based editing and terminal work.

No Termux or local computer is required.

## Java ME release requirements

A genuine MIDlet release requires:
- CLDC 1.1 API
- MIDP 2.0 API
- ECJ batch compiler configured for the required legacy target
- CLDC-aware preverifier
- Validated JAR/JAD packaging

A source validation workflow is not a substitute for these components. Until all are available and the actual build succeeds, no installable MIDlet is claimed.

## Baseline

The current prepared source baseline is Netormium 1.0 RC20 (Browser-Only Development Workflow). The complete source archive is maintained separately pending transfer into this repository.

## Architecture

`J2ME MIDlet -> NTM/2 session API -> Node.js / Playwright -> persistent Chromium page/context`

The gateway renders web pages remotely and returns PNG viewport snapshots to the MIDlet. The client forwards key, tap, and text input to the gateway.

## Security status

The gateway remains a development prototype, not a public production proxy. Do not expose it to the public internet or use it for banking or other sensitive accounts without implementing authentication, TLS, robust SSRF protections, isolation, rate limits, quotas, and appropriate data handling.
