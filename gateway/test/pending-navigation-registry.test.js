'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const PendingNavigationRegistry = require('../pending-navigation-registry');

test('registers and cancels a pending navigation exactly once', () => {
  const registry = new PendingNavigationRegistry();
  let calls = 0;
  assert.equal(registry.register('request-1', () => { calls++; }), true);
  assert.equal(registry.has('request-1'), true);
  assert.equal(registry.cancel('request-1'), true);
  assert.equal(registry.cancel('request-1'), false);
  assert.equal(registry.has('request-1'), false);
  assert.equal(calls, 1);
});

test('finishing a navigation removes its cancellation handler', () => {
  const registry = new PendingNavigationRegistry();
  let calls = 0;
  registry.register('request-2', () => { calls++; });
  registry.finish('request-2');
  assert.equal(registry.cancel('request-2'), false);
  assert.equal(calls, 0);
});

test('unknown and empty identifiers are harmless', () => {
  const registry = new PendingNavigationRegistry();
  assert.equal(registry.register('', () => {}), false);
  assert.equal(registry.cancel('missing'), false);
  assert.equal(registry.has('missing'), false);
});
