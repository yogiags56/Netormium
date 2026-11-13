'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const protocol = require('../session-protocol');

function records(raw) {
  return raw.trimEnd().split('\n').map(line => line.split('\t'));
}

test('serializes NTM/2 metadata using real record separators and UTF-8 percent encoding', () => {
  const raw = protocol.metadataResponse(
    { id: 'abc123', dataSaver: true },
    {
      url: 'https://example.org/a?x=1&y=2',
      title: 'Halo 世界',
      text: 'Baris satu\nBaris dua',
      elements: [{
        kind: 'link', x: 12, y: 34, w: 90, h: 20,
        label: 'Buka\tcontoh', href: 'https://example.org/next',
        inputType: '', focused: false
      }]
    }
  );
  const parsed = records(raw);
  assert.equal(parsed[0][0], 'NTM/2');
  assert.deepEqual(parsed[1], ['SESSION', 'abc123']);
  assert.deepEqual(parsed[2], ['DATASAVER', '1']);
  assert.deepEqual(parsed[3], ['URL', 'https%3A%2F%2Fexample.org%2Fa%3Fx%3D1%26y%3D2']);
  assert.deepEqual(parsed[4], ['TITLE', 'Halo%20%E4%B8%96%E7%95%8C']);
  assert.deepEqual(parsed[5], ['TEXT', 'Baris%20satu%0ABaris%20dua']);
  assert.deepEqual(parsed[6], [
    'ELEMENT', '1', 'link', '12', '34', '90', '20',
    'Buka%09contoh', 'https%3A%2F%2Fexample.org%2Fnext', '', '0'
  ]);
  assert.deepEqual(parsed[7], ['END']);
  assert.ok(raw.endsWith('\n'));
});

test('serializes NTM/2 gateway errors as a parseable response', () => {
  const parsed = records(protocol.errorResponse('Session expired'));
  assert.deepEqual(parsed, [['NTM/2'], ['ERROR', 'Session%20expired'], ['END']]);
});

test('bounds interactive element count and metadata fields', () => {
  const elements = Array.from({ length: 30 }, (_, i) => ({
    kind: 'button', x: i, y: 0, w: 10, h: 10,
    label: 'x'.repeat(200), href: '', inputType: '', focused: false
  }));
  const parsed = records(protocol.metadataResponse({ id: 's' }, { elements }));
  assert.equal(parsed.filter(row => row[0] === 'ELEMENT').length, 24);
  assert.ok(parsed.find(row => row[0] === 'ELEMENT')[7].length <=
    require('../metadata-limits').MAX_ELEMENT_LABEL_CHARS * 3);
});
