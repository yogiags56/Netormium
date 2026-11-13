# Android-only development (browser workflow)

Netormium is maintained from an Android phone without Termux and without a
personal computer. Use the browser-based workflow documented in
[`BROWSER-ONLY-WORKFLOW.md`](BROWSER-ONLY-WORKFLOW.md).

## Supported workflow

1. Open this private repository in Chrome or another mobile browser.
2. Edit individual files through GitHub's web interface.
3. Use GitHub Actions for cloud validation when Actions runners are available.
4. Use GitHub Codespaces for multi-file editing and remote terminal work if the
   feature is available to the account.
5. Review validation results and commit changes through GitHub.

No Android shell, Termux installation, or local Java compiler is required.

## Java ME release gate

The Java ME client still needs a genuine CLDC 1.1 API, MIDP 2.0 API, compatible
ECJ batch compiler, and Java ME preverifier. These components must be obtained
and used in accordance with their licenses. They are not bundled in this
repository.

A passing Node.js test suite or Ant project-help output is source validation,
not proof of a MIDlet build. Do not claim an installable JAR/JAD until the
actual compile, preverification, package validation, and emulator or handset
test have succeeded.
