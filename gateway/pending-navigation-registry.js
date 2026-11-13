'use strict';

/** Tracks in-flight browser session creations so clients can cancel them. */
class PendingNavigationRegistry {
  constructor() { this.pending = new Map(); }

  register(id, cancel) {
    if (!id || typeof cancel !== 'function') return false;
    this.pending.set(String(id), cancel);
    return true;
  }

  cancel(id) {
    const key = String(id || '');
    const callback = this.pending.get(key);
    if (!callback) return false;
    this.pending.delete(key);
    try { callback(); } catch (_) {}
    return true;
  }

  finish(id) { this.pending.delete(String(id || '')); }
  has(id) { return this.pending.has(String(id)); }
}

module.exports = PendingNavigationRegistry;
