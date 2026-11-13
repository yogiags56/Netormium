'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const limits = require('../viewport-limits');

test('uses defaults for missing or malformed viewport dimensions', () => {
  assert.deepEqual(limits.viewportDimensions(null, undefined, 260),
    { width: 240, height: 260, pixels: 62400 });
  assert.deepEqual(limits.viewportDimensions('invalid', 'NaN', 320),
    { width: 240, height: 320, pixels: 76800 });
});

test('clamps viewport dimensions to safe feature-phone bounds', () => {
  assert.deepEqual(limits.viewportDimensions('1', '9999', 260),
    { width: 128, height: 960, pixels: 122880 });
  assert.deepEqual(limits.viewportDimensions('9999', '1', 260),
    { width: 640, height: 128, pixels: 81920 });
});

test('floors fractional viewport dimensions and stays within pixel budget', () => {
  const size = limits.viewportDimensions('239.9', '319.8', 260);
  assert.deepEqual(size, { width: 239, height: 319, pixels: 76241 });
  assert.ok(size.pixels <= limits.MAX_VIEWPORT_PIXELS);
});
