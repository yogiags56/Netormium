# Java ME SDK acquisition and provenance

Netormium targets **CLDC 1.1 + MIDP 2.0**. The handset does not run Chromium;
the Java ME MIDlet remains a thin client for the remote Chromium gateway.

## 1. Standalone Eclipse compiler (ECJ)

The ECJ acquisition helper downloads the stable Eclipse 4.41 GA batch compiler
and verifies its official SHA-512 before installing it. It then launches ECJ
and requires the legacy `-1.3` option to be present before finalizing the file:

```sh
cd gateway
node scripts/acquire-ecj.js
```

The default destination is `tools/ecj-4.41.jar` at the project root.
Override it with `--dest PATH`. The helper refuses to overwrite an existing
file that does not match the pinned checksum.

Pinned artifact:
- Release: `4.41 / R-4.41-202608281142`
- URL: https://download.eclipse.org/eclipse/downloads/drops4/R-4.41-202608281142/ecj-4.41.jar
- SHA-512:
  `080bb403adb3b88b5b5f382388922425de5274f7729f0a94702f0e2a516c7d13c6740faab1cb20786b1dda0c5da933aa9b842f7c64867414ff75fa668ea06b09`

After acquisition, set `ecj.jar=tools/ecj-4.41.jar` in
`build.properties`, select `compiler.backend=ecj`, and run:

```sh
npm run check:j2me-toolchain
ant doctor
ant compile
```

The toolchain check only establishes that required inputs are present and
recognized. A successful real compile is a separate gate.

## 2. CLDC 1.1, MIDP 2.0 and preverifier

Use a genuine Java ME SDK/WTK distribution that includes the relevant API
archives and preverifier. Oracle's legacy archive lists:
- Java ME SDK 3.0 (Windows XP/Vista 32-bit and Mac OS distributions)
- Sun Java Wireless Toolkit 2.5.2_01 for CLDC (Linux i486 and Windows)
- MIDP 2.0 reference implementation source packages

Official sources:
- https://www.oracle.com/java/technologies/java-archive-downloads-javame-downloads.html
- https://www.oracle.com/java/technologies/java-me-sdk-v3.html
- https://www.oracle.com/java/technologies/javame-sdk-downloads.html

These are legacy packages with platform and licensing constraints. Review and
accept the applicable Oracle license yourself. Do not unpack or redistribute
vendor SDK binaries in the Netormium source ZIP unless their license permits it.

For WTK 2.5.2, the expected core inputs are commonly named
`lib/cldcapi11.jar`, `lib/midpapi20.jar`, and `bin/preverify` (or
`bin/preverify.exe` on Windows). Confirm the exact files in the SDK you install.
Configure `cldc.api.jar`, `midp.api.jar`, and `preverify.executable` in
`build.properties`; the paths vary by SDK and host OS.

The doctor inspects archive contents, not only ZIP signatures: CLDC must expose
`java.lang.Object` and Generic Connection Framework classes; MIDP must expose
`MIDlet`, `Display`, and `Canvas`. Then run
`npm run check:j2me-toolchain` and `ant doctor`.

## 3. Release gates

1. Verify ECJ checksum and launch the batch compiler.
2. Verify genuine CLDC 1.1 and MIDP 2.0 API archives.
3. Compile every MIDlet source file against those APIs.
4. Run the Java ME preverifier against compiled classes.
5. Build JAR/JAD and inspect manifest/profile/version consistency.
6. Run an emulator smoke test, then test on physical target handsets.

No installable MIDlet is claimed until all applicable gates pass. A desktop
`javac` build or a successful toolchain preflight is not a substitute for
Java ME compilation and preverifier execution.


## 4. Local SDK discovery (RC14)

Run from `gateway/`:

```sh
npm run discover:j2me-sdk
node scripts/discover-j2me-sdk.js --root /absolute/path/to/WTK --json
```

The discovery helper checks common WTK/Java ME SDK locations and an optional
explicit root. It reports the paths for `cldcapi11.jar`, `midpapi20.jar`, and
`preverify` (or Windows equivalents). It does not download, unpack, install,
or accept licenses for vendor SDKs. Discovery only locates files; the
`check:j2me-toolchain` command separately verifies archive contents and
preverifier executability.


## 5. RC15 build and package validation

After installing a genuine Java ME toolchain and configuring `build.properties`,
run:

```sh
ant -propertyfile build.properties clean release
```

The ECJ Ant target passes every discovered `.java` source file explicitly and
sets `-sourcepath`; it does not rely on the compiler treating the source root
directory itself as a source file. The preverification output directory is
cleared before each run to prevent stale classes from leaking into a release.

`release` runs the structural JAR/JAD validator after packaging. It verifies
the MIDlet entry class, manifest/JAD required attributes, MIDP/CLDC profile
declarations, matching suite metadata, exact `MIDlet-Jar-Size`, and absence
of bundled `javax.microedition` API classes. This is a structural check only;
it does not replace emulator or physical handset execution.

The RC15 validator also requires the `MIDlet-1` entry to be present in both the manifest and JAD and to match exactly.


## 6. RC16 acquisition attempt

RC16 retries the pinned ECJ 4.41 download and local Java ME SDK discovery. If the runtime cannot resolve or reach the official Eclipse host, acquisition stops without substituting an unverified compiler. Install a genuine WTK/Java ME SDK separately, review its license, and configure the API archives and preverifier locally. A toolchain candidate is not a successful MIDlet build: compile, preverify, package, emulator, and handset gates remain distinct.


## 7. RC17 official mirror fallback

RC17 attempts the pinned ECJ 4.41 artifact from the primary Eclipse download host and the official Eclipse archive host. Both URLs are tied to the same published SHA-512 digest; a downloaded artifact must also pass the JAR signature check and batch-compiler launch probe advertising legacy `-1.3` mode. A mirror failure is reported separately, and no artifact is installed unless all checks pass. This does not supply the CLDC/MIDP API archives or preverifier; those still require a genuine Java ME SDK/WTK installation.


## 8. RC18 local toolchain configuration (preferred path)

The repeated automatic downloads are no longer the primary acquisition path. Install a genuine WTK/Java ME SDK locally after reviewing the vendor license, then run:

```sh
cd gateway
node scripts/configure-local-toolchain.js --root /absolute/path/to/WTK --ecj /absolute/path/to/ecj.jar --output ../build.properties
cd ..
ant -propertyfile build.properties clean release
```

The helper recursively locates CLDC/MIDP API archives and the host-appropriate preverifier, validates the expected class entries with the JDK `jar` tool, verifies the ECJ artifact using the pinned checksum and compiler launch probe, and only then writes a complete properties file. It refuses to create a partial configuration. SDK and ECJ binaries remain outside the source package.

Oracle's Java archive lists Java ME SDK 3.0 and Sun Java Wireless Toolkit 2.5.2_01. Oracle states WTK 2.5.2 implements CLDC 1.1 and MIDP 2.0. The WTK archive is legacy software and its Linux installer targets old x86 environments; on modern hosts, a compatible local installation or a supported Windows environment may be more practical. See the official archive and WTK download documentation before installing.
