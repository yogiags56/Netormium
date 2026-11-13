# Netormium Browser-Only Development Workflow

This workflow is designed for contributors who have only an Android phone and a
mobile browser. No Termux, local shell, or personal computer is required.

## What this changes

- Source editing and repository management happen through GitHub's website.
- Build/test jobs run on GitHub-hosted Actions runners.
- Optional Codespaces access provides a browser-based VS Code and remote terminal.
- Android is used only as the browser/device; compilation does not run locally.

Official references:
- GitHub Codespaces browser development:
  https://docs.github.com/en/codespaces/developing-in-a-codespace
- Manually running GitHub Actions:
  https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow

## First-time setup

1. Create or open a GitHub repository in the mobile browser.
2. Upload the contents of this Netormium source package to the repository's
   default branch. Keep the directory structure intact.
3. Open the Actions tab.
4. Select **Netormium Browser Build**.
5. Choose **Run workflow**.
6. Leave `run_java_me_release` disabled for the first run.
7. Inspect the run summary and download `netormium-browser-validation`.

## What the first workflow proves

It validates the Node.js gateway tests, release consistency, and Ant diagnostics
on a cloud runner. It does NOT claim a Java ME MIDlet was built.

## Genuine Java ME release gate

The optional Java ME release job is deliberately disabled by default. It requires
a real CLDC 1.1 / MIDP 2.0 SDK installation and ECJ JAR to be provisioned on the
runner. Merely setting path variables does not install files on GitHub-hosted
runners. Oracle's legacy SDK may require interactive license acceptance and
cannot be assumed to be downloadable in an unattended workflow.

Do not put license credentials, account passwords, or private tokens in source
files. Do not upload unlicensed SDK binaries. Once a legally obtained SDK is
available, configure a controlled artifact/provisioning route and verify its
checksums before enabling the Java ME release job.

## Codespaces option

For interactive browser-based editing, open the repository on GitHub and choose
**Code > Codespaces > Create codespace** if available. GitHub documents editing,
terminal, Git, and running applications in the browser. Availability and included
usage depend on the GitHub account and repository.

## Mobile usability tips

- Use Chrome or another Chromium-based browser.
- Enable **Desktop site** if GitHub hides the Actions workflow controls.
- Use landscape orientation for the Codespaces editor.
- Use GitHub's web file upload for small source edits; use Codespaces for multi-file
  changes and terminal commands.
- Stop Codespaces when finished to avoid unnecessary compute usage.

## Current limitation

The genuine Java ME toolchain is still missing from the project environment.
Until the official APIs and preverifier are available and verified, no installable
MIDlet JAR/JAD can be claimed.
