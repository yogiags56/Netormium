#!/usr/bin/env node
'use strict';

// Acquires the stable Eclipse 4.41 batch compiler. The archive is pinned to
// the SHA-512 printed on Eclipse's official release page and is executed once
// before it is moved into the project tools directory.
const fs = require('node:fs');
const path = require('node:path');
const https = require('node:https');
const { URL: URLClass } = require('node:url');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RELEASE = '4.41';
const BUILD = 'R-4.41-202608281142';
const FILE = `ecj-${RELEASE}.jar`;
const URLS = [
  `https://download.eclipse.org/eclipse/downloads/drops4/${BUILD}/${FILE}`,
  `https://archive.eclipse.org/eclipse/downloads/drops4/${BUILD}/${FILE}`
];
const URL = URLS[0];
const SHA512 = '080bb403adb3b88b5b5f382388922425de5274f7729f0a94702f0e2a516c7d13c6740faab1cb20786b1dda0c5da933aa9b842f7c64867414ff75fa668ea06b09';
const MAIN_CLASS = 'org.eclipse.jdt.compiler.batch.BatchCompiler';
const MAX_BYTES = 12 * 1024 * 1024;

function sha512(file) {
  return crypto.createHash('sha512').update(fs.readFileSync(file)).digest('hex');
}

function isJar(file) {
  let fd;
  try {
    if (!fs.existsSync(file) || !fs.statSync(file).isFile()) return false;
    fd = fs.openSync(file, 'r');
    const header = Buffer.alloc(4);
    const count = fs.readSync(fd, header, 0, 4, 0);
    return count === 4 && header[0] === 0x50 && header[1] === 0x4b &&
      ((header[2] === 0x03 && header[3] === 0x04) ||
       (header[2] === 0x05 && header[3] === 0x06) ||
       (header[2] === 0x07 && header[3] === 0x08));
  } catch (_) {
    return false;
  } finally {
    if (fd !== undefined) {
      try { fs.closeSync(fd); } catch (_) {}
    }
  }
}

function probeEcj(jarPath, javaCommand = process.env.JAVA || 'java', runner = spawnSync) {
  const result = runner(javaCommand,
    ['-cp', jarPath, MAIN_CLASS, '-help'],
    { encoding: 'utf8', timeout: 20000, maxBuffer: 4 * 1024 * 1024 });
  const output = `${result.stdout || ''}${result.stderr || ''}`;
  const legacyMode = /(?:^|\s)-1\.3(?:\s|$)/m.test(output);
  return {
    ok: !result.error && result.status === 0 && legacyMode,
    status: result.status,
    output: output.trim(),
    error: result.error ? result.error.message : '',
    legacyMode
  };
}

function verifyArtifact(file) {
  if (!isJar(file)) throw new Error('Artifact is not a ZIP/JAR archive');
  const actual = sha512(file);
  if (actual !== SHA512) throw new Error(`SHA-512 mismatch: expected ${SHA512}, got ${actual}`);
  const probe = probeEcj(file);
  if (!probe.ok) {
    const reason = probe.error || `exit ${probe.status}; -1.3 mode ${probe.legacyMode ? 'present' : 'absent'}`;
    throw new Error(`ECJ launch/legacy-mode probe failed: ${reason}`);
  }
  return { sha512: actual, probe };
}

function download(url, destination, redirects = 5) {
  return new Promise((resolve, reject) => {
    const request = https.get(url, response => {
      if ([301, 302, 303, 307, 308].includes(response.statusCode)) {
        response.resume();
        if (!response.headers.location || redirects <= 0) {
          reject(new Error('Invalid or excessive redirect'));
          return;
        }
        download(new URLClass(response.headers.location, url).toString(), destination, redirects - 1)
          .then(resolve, reject);
        return;
      }
      if (response.statusCode !== 200) {
        response.resume();
        reject(new Error(`HTTP ${response.statusCode} while downloading ${url}`));
        return;
      }
      const declaredLength = Number(response.headers['content-length'] || 0);
      if (declaredLength > MAX_BYTES) {
        response.resume();
        reject(new Error(`Artifact exceeds ${MAX_BYTES} byte safety limit`));
        return;
      }

      const temporary = `${destination}.part`;
      let received = 0;
      const output = fs.createWriteStream(temporary, { flags: 'w' });
      response.on('data', chunk => {
        received += chunk.length;
        if (received > MAX_BYTES) {
          response.destroy(new Error(`Artifact exceeds ${MAX_BYTES} byte safety limit`));
          output.destroy(new Error('Download size limit exceeded'));
        }
      });
      response.pipe(output);
      output.on('finish', () => output.close(() => {
        try {
          verifyArtifact(temporary);
          fs.renameSync(temporary, destination);
          resolve(destination);
        } catch (error) {
          try { fs.unlinkSync(temporary); } catch (_) {}
          reject(error);
        }
      }));
      output.on('error', error => {
        try { fs.unlinkSync(temporary); } catch (_) {}
        reject(error);
      });
      response.on('error', error => {
        try { fs.unlinkSync(temporary); } catch (_) {}
        reject(error);
      });
    });
    request.setTimeout(25000, () => request.destroy(new Error('Download timed out after 25 seconds')));
    request.on('error', reject);
  });
}

async function main(argv = process.argv.slice(2)) {
  const args = argv;
  if (args.includes('--help')) {
    console.log('Usage: node scripts/acquire-ecj.js [--dest PATH]');
    console.log('Pinned stable artifact mirrors:');
    for (const url of URLS) console.log(`  ${url}`);
    console.log(`SHA-512: ${SHA512}`);
    console.log('The downloaded compiler is launched and checked for -1.3 mode before installation.');
    return;
  }
  const destIndex = args.indexOf('--dest');
  if (destIndex >= 0 && !args[destIndex + 1]) {
    throw new Error('--dest requires a destination path');
  }
  const destination = path.resolve(destIndex >= 0
    ? args[destIndex + 1]
    : path.join(__dirname, '..', '..', 'tools', FILE));
  fs.mkdirSync(path.dirname(destination), { recursive: true });

  if (fs.existsSync(destination)) {
    verifyArtifact(destination);
    console.log(`Verified existing stable ECJ 4.41: ${destination}`);
    return;
  }

  console.log(`Downloading stable Eclipse ECJ ${RELEASE} (${BUILD})...`);
  const failures = [];
  for (const url of URLS) {
    try {
      console.log(`Trying official mirror: ${url}`);
      await download(url, destination);
      console.log(`Verified ECJ SHA-512, JAR structure, and -1.3 launch mode: ${destination}`);
      return;
    } catch (error) {
      failures.push(`${new URLClass(url).hostname}: ${error.message}`);
      console.error(`Mirror failed: ${failures[failures.length - 1]}`);
    }
  }
  throw new Error(`All official ECJ mirrors failed. ${failures.join(' | ')}`);
}

module.exports = {
  RELEASE, BUILD, FILE, URL, URLS, SHA512, MAIN_CLASS, sha512, isJar, probeEcj, verifyArtifact
};

if (require.main === module) {
  main().catch(error => {
    console.error(`ECJ acquisition failed: ${error.message}`);
    process.exitCode = 1;
  });
}
