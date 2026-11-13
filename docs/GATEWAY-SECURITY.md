# Gateway network security notes

Netormium Gateway runs Chromium on a server. Treat every requested URL and every
subresource URL as untrusted input.

## Implemented URL policy

The gateway's `host-policy.js` currently:
- accepts HTTP and HTTPS only;
- rejects URL credentials and local hostnames such as localhost, .local,
  and .internal;
- rejects non-public IPv4 ranges, including loopback, RFC1918, link-local,
  carrier-grade NAT, documentation, benchmarking, multicast and reserved
  ranges;
- accepts only global-unicast IPv6 and rejects loopback, link-local, unique
  local, multicast, documentation, benchmarking, 6to4 and IPv4-mapped private
  addresses;
- resolves hostnames before navigation and rejects a hostname if any returned
  address is not public;
- applies the same check to the initial URL and Chromium subresource requests.

## Remaining deployment risks

DNS validation at the application layer is defense in depth, not a complete
network isolation boundary. DNS answers can change between validation and
connection (DNS rebinding / time-of-check-to-time-of-use). A production
deployment must additionally enforce outbound network policy at the host or
container boundary, deny access to loopback/private/link-local/metadata
networks, and ideally use a controlled egress proxy that validates and pins
resolved destinations.

Also required before public deployment:
- authentication and per-user session ownership;
- TLS between MIDlet and Gateway;
- request, session, bandwidth and concurrency quotas;
- browser process/container isolation and regular Chromium updates;
- strict origin policy and safe handling of downloads, popups and redirects;
- logs and retention policies that avoid collecting unnecessary page content.

The Gateway is still a development prototype. Do not expose it publicly or use
it for banking, identity, medical or other sensitive accounts.
