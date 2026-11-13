# NTM/1 protocol

Encoding: UTF-8, one record per line, LF line endings. Field values are URI-component percent encoded.
The gateway returns `text/plain; charset=utf-8`.

Records:
```text
NTM/1
STATUS<TAB>200
URL<TAB>https%3A%2F%2Fexample.org%2F
TITLE<TAB>Example%20Domain
TEXT<TAB>Page%20text%0AAnother%20line
LINK<TAB>1<TAB>Example%20link<TAB>https%3A%2F%2Fexample.org%2Fnext
END
```

- `STATUS`: upstream HTTP status or gateway error status.
- `URL`: final URL after redirects.
- `TITLE`: document title.
- `TEXT`: readable body text, bounded by gateway.
- `LINK`: repeated; numeric index, label, absolute href.
- `ERROR`: optional gateway-side error message.
- `END`: terminates response.

Client must percent-decode UTF-8 fields. Do not assume all lines or records arrive in one network read.
The gateway is a trusted-server component. Production deployment must add authentication, TLS, rate limiting, DNS/IP validation on every redirect and subrequest, request quotas, and abuse monitoring.


## Java ME transport limits

The MIDP client requests response data with a strict byte ceiling and checks both
the declared HTTP entity length (when supplied by the server) and the actual
number of bytes received. A missing or unknown Content-Length is not treated as
zero: the streaming counter remains authoritative. The PNG and interactive
clients use separate limits.

The MIDP 2.0 `HttpConnection` API does not expose a portable numeric connect,
read, or overall request timeout setter. `Connector.open(..., true)` asks the
implementation for timeout-capable connection behavior, but the actual timeout
policy is vendor/runtime-specific. Therefore a gateway request can still block
on some handsets or network stacks. The client must not claim a hard timeout
guarantee until tested against the target devices; a server-side request
deadline and reverse-proxy timeout are also required.
