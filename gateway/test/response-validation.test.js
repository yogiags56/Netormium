'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { isPngBuffer } = require('../response-validation');

test('accepts a PNG signature with an IHDR chunk', () => {
  const png = Buffer.alloc(24);
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]).copy(png, 0);
  png.write('IHDR', 12, 'ascii');
  assert.equal(isPngBuffer(png), true);
});

test('rejects truncated or non-PNG data', () => {
  assert.equal(isPngBuffer(Buffer.from('not png')), false);
  assert.equal(isPngBuffer(Buffer.alloc(23)), false);
  assert.equal(isPngBuffer(null), false);
});
