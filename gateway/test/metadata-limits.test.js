'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const limits = require('../metadata-limits');

test('metadata string bounds are enforced', () => {
  assert.equal(limits.boundedString('abcdef', 3), 'abc');
  assert.equal(limits.boundedString(null, 4), '');
});

test('metadata limits fit the compact feature-phone protocol budget', () => {
  assert.equal(limits.MAX_PAGE_TEXT_CHARS, 6000);
  assert.equal(limits.MAX_ELEMENTS, 24);
  assert.equal(limits.MAX_ELEMENT_LABEL_CHARS, 72);
  assert.equal(limits.MAX_ELEMENT_HREF_CHARS, 256);
  assert.ok(limits.MAX_URL_CHARS <= 1024);
});
