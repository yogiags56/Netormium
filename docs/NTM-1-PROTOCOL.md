# NTM/2 session protocol

NTM/2 is the interactive protocol used by the Netormium MIDlet and the Chromium
Gateway. The gateway keeps one Playwright page per browser tab; the MIDlet sends
navigation and interaction commands and receives compact metadata plus PNG
viewport snapshots.

## Transport conventions

- HTTP endpoints currently use GET requests and UTF-8 query parameters.
- Text fields are URI-component percent-encoded by the gateway.
- Metadata responses use `text/plain; charset=utf-8`, LF-delimited records,
  and a final `END` record.
- The first record must be `NTM/2`. Unknown records should be ignored.
- A failed session operation can return `ERROR<TAB>message` followed by `END`.
- Session identifiers are temporary server-side handles. Do not persist them
  across MIDlet restarts; persist the page URL and reopen a new session instead.

## Metadata response

A successful open, metadata refresh, tap, text entry, or key action returns:

```text
NTM/2
SESSION<TAB>opaque-session-id
DATASAVER<TAB>0
URL<TAB>https%3A%2F%2Fexample.org%2F
TITLE<TAB>Example%20Domain
TEXT<TAB>Visible%20page%20text
ELEMENT<TAB>1<TAB>link<TAB>12<TAB>80<TAB>120<TAB>24<TAB>Example<TAB>https%3A%2F%2Fexample.org%2Fnext<TAB><TAB>0
END
```

Each `ELEMENT` record contains, in order: index, kind, viewport x/y,
width/height, encoded label, encoded href, encoded input type, and focused flag.
The gateway currently reports at most 24 visible interactive elements.

## Endpoints

All endpoints are relative to the configured Gateway base URL.

| Method and path | Query parameters | Result |
|---|---|---|
| `GET /health` | none | Gateway identity and health text |
| `GET /v1/session/open` | `url,w,h,ds,rid` | Creates a Chromium session and returns metadata |
| `GET /v1/session/metadata` | `id` | Refreshes page metadata |
| `GET /v1/session/snapshot` | `id,y` | PNG viewport snapshot |
| `GET /v1/session/tap` | `id,x,y` | Clicks at viewport coordinates and returns metadata |
| `GET /v1/session/type` | `id,text` | Inserts text into the focused element and returns metadata |
| `GET /v1/session/key` | `id,key` | Sends an allowed keyboard key and returns metadata |
| `GET /v1/session/close` | `id` | Closes a session |
| `GET /v1/session/cancel` | `rid` | Cancels a pending session-open request |
| `GET /v1/snapshot` | `url,w,h,y` | Stateless PNG snapshot compatibility endpoint |
| `GET /v1/page` | `url` | Legacy NTM/1 text-and-link response |

Allowed key values are `Enter`, `Tab`, `Backspace`, `Escape`, `ArrowUp`,
`ArrowDown`, `ArrowLeft`, `ArrowRight`, `Home`, and `End`.

## Limits and lifecycle

- Session lifetime: 60 minutes of inactivity.
- Maximum concurrent sessions: 30; creating a session at capacity evicts the
  oldest session.
- Open URL length: 4,096 characters.
- Text insertion length: 1,024 characters.
- Metadata and PNG response sizes are bounded by the MIDlet transport.
- PNG viewport dimensions are bounded by the Gateway viewport policy.
- Snapshot cache lifetime: 5 seconds; global cache budget: 8 MiB; up to two
  snapshots per session.
- Data Saver mode blocks image, media, and font requests.
- The MIDlet serializes interactive mutations for each active session and
  closes sessions when tabs are switched, replaced, or the MIDlet shuts down.

## Security and deployment

This is a development protocol, not a public proxy security specification.
The Gateway must remain on a trusted network until authentication, TLS, rate
limiting, quotas, process isolation, and hardened egress controls are deployed.
The current public-address URL policy is defense in depth and is not a complete
defense against DNS rebinding or infrastructure-level egress attacks.

Because the current API uses GET for text insertion and other state-changing
operations, do not expose it to untrusted clients or log full query strings.
A future protocol revision should move mutations to POST with request-body
limits and add authenticated sessions.

## Legacy protocol

`GET /v1/page?url=...` remains available for the original NTM/1 text-and-link
client path. New interactive clients should use NTM/2 session endpoints.
