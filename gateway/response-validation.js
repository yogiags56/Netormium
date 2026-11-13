'use strict';

function isPngBuffer(value) {
  return Buffer.isBuffer(value) &&
    value.length >= 24 &&
    value[0] === 0x89 && value[1] === 0x50 &&
    value[2] === 0x4e && value[3] === 0x47 &&
    value[4] === 0x0d && value[5] === 0x0a &&
    value[6] === 0x1a && value[7] === 0x0a &&
    value.toString('ascii', 12, 16) === 'IHDR';
}

module.exports = { isPngBuffer };
