# Netormium Alpha MIDlet Package

## Verified CI package

The Java ME Toolchain Probe completed successfully for commit
`26354ae6439ebebfdf29d6bf3d104aae27727b00` on 2026-11-13.

The workflow performed all of the following:

- Compiled the MIDlet sources against CLDC 1.1 and MIDP 2.0 API stubs.
- Ran ProGuard's Java ME preverification.
- Packaged `Netormium.jar` and generated `Netormium.jad`.
- Checked the MIDlet entry class, manifest attributes, JAD attributes,
  JAR size consistency, and exclusion of bundled `javax.microedition` APIs.
- Uploaded the Alpha package as a GitHub Actions artifact.

Package validation reported 54 JAR entries and a JAR size of 78,116 bytes.

Workflow run:
https://github.com/yogiags56/Netormium/actions/runs/43448930827

Artifact:
`netormium-alpha-midlet-package`

Open the workflow run's **Artifacts** section to download the ZIP containing
`Netormium.jar` and `Netormium.jad`. The artifact is temporary and expires
according to GitHub Actions artifact retention (the current upload expires
2027-02-11).

## Before installing

1. Extract both files from the artifact ZIP into the same directory.
2. Open `Netormium.jad` in a text editor.
3. Change `Netormium-Gateway-URL` from the example LAN address to the
   reachable URL of your own Netormium gateway.
4. Keep `MIDlet-Jar-URL: Netormium.jar` and the matching JAR beside the JAD.
5. Install the JAD on a MIDP 2.0 / CLDC 1.1 compatible emulator or handset.

Do not expose the prototype gateway publicly without authentication, TLS,
SSRF protections, process isolation, rate limits, quotas, and suitable data
handling controls.

## Device test checklist

- [ ] JAD installs and the MIDlet appears in the application list.
- [ ] MIDlet launches without an uncaught exception.
- [ ] Start Page draws correctly at the device's native resolution.
- [ ] Keypad navigation works with the device's directional and soft keys.
- [ ] Gateway connection succeeds on the same reachable network.
- [ ] A page loads and the returned viewport can be scrolled.
- [ ] Back, reload, address entry, and exit behave correctly.
- [ ] Bookmarks, History, and Speed Dial survive closing and relaunching.
- [ ] Data Saver, Load Images, Mobile View, and text-size settings behave as expected.
- [ ] Memory use and response time remain acceptable on the target handset.

## What this CI result does not prove

A successful compile, preverification, and structural package check does not
prove runtime compatibility on every Java ME implementation. The Alpha has
not been certified on a physical handset or across the full range of target
screen sizes. Record emulator/handset model, screen resolution, Java ME
profile, gateway version, and any crash details when reporting test results.
