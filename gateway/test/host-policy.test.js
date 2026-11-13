'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { isPublicIp, parsePublicHttpUrl } = require('../host-policy');

test('allows representative public IPv4 addresses', () => {
  assert.equal(isPublicIp('1.1.1.1'), true);
  assert.equal(isPublicIp('8.8.8.8'), true);
});

test('blocks private, loopback, link-local, carrier-grade and reserved IPv4', () => {
  for (const ip of [
    '0.1.2.3', '10.0.0.1', '100.64.0.1', '127.0.0.1',
    '169.254.169.254', '172.16.0.1', '192.168.1.1',
    '192.0.2.1', '198.18.0.1', '203.0.113.1', '224.0.0.1',
    '240.0.0.1'
  ]) assert.equal(isPublicIp(ip), false, ip);
});

test('allows representative global IPv6 and blocks local/special ranges', () => {
  assert.equal(isPublicIp('2606:4700:4700::1111'), true);
  for (const ip of [
    '::', '::1', 'fc00::1', 'fd00::1', 'fe80::1',
    'ff02::1', '2001:db8::1', '2001:2::1', '2002::1'
  ]) assert.equal(isPublicIp(ip), false, ip);
});

test('applies IPv4 policy to IPv4-mapped IPv6', () => {
  assert.equal(isPublicIp('::ffff:127.0.0.1'), false);
  assert.equal(isPublicIp('::ffff:8.8.8.8'), true);
});

test('rejects non-http schemes, credentials and local hostnames', () => {
  assert.equal(parsePublicHttpUrl('file:///etc/passwd'), null);
  assert.equal(parsePublicHttpUrl('http://user:pass@example.com'), null);
  assert.equal(parsePublicHttpUrl('http://localhost'), null);
  assert.equal(parsePublicHttpUrl('http://router.local'), null);
  assert.ok(parsePublicHttpUrl('https://example.com'));
});
