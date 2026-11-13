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
