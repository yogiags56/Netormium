'use strict';

const MIN_VIEWPORT_WIDTH = 128;
const MAX_VIEWPORT_WIDTH = 640;
const MIN_VIEWPORT_HEIGHT = 128;
const MAX_VIEWPORT_HEIGHT = 960;
const DEFAULT_VIEWPORT_WIDTH = 240;
const DEFAULT_SESSION_HEIGHT = 260;
const DEFAULT_SNAPSHOT_HEIGHT = 320;
const MAX_VIEWPORT_PIXELS = MAX_VIEWPORT_WIDTH * MAX_VIEWPORT_HEIGHT;

function boundedDimension(raw, fallback, minimum, maximum) {
  const value = raw === undefined || raw === null || raw === '' ? fallback : Number(raw);
  if (!Number.isFinite(value)) return fallback;
  return Math.max(minimum, Math.min(maximum, Math.floor(value)));
}

function viewportDimensions(rawWidth, rawHeight, defaultHeight) {
  const width = boundedDimension(rawWidth, DEFAULT_VIEWPORT_WIDTH,
    MIN_VIEWPORT_WIDTH, MAX_VIEWPORT_WIDTH);
  const height = boundedDimension(rawHeight, defaultHeight,
    MIN_VIEWPORT_HEIGHT, MAX_VIEWPORT_HEIGHT);
  return { width, height, pixels: width * height };
}

module.exports = {
  MIN_VIEWPORT_WIDTH, MAX_VIEWPORT_WIDTH,
  MIN_VIEWPORT_HEIGHT, MAX_VIEWPORT_HEIGHT,
  DEFAULT_VIEWPORT_WIDTH, DEFAULT_SESSION_HEIGHT, DEFAULT_SNAPSHOT_HEIGHT,
  MAX_VIEWPORT_PIXELS, boundedDimension, viewportDimensions
};
