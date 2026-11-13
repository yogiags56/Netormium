'use strict';

const metadataLimits = require('./metadata-limits');

function encodeField(value) {
  return encodeURIComponent(String(value == null ? '' : value));
}

function boundedInteger(value, fallback, minimum, maximum) {
  const number = Number(value);
  if (!Number.isFinite(number)) return fallback;
  return Math.max(minimum, Math.min(maximum, Math.floor(number)));
}

function errorResponse(message) {
  return ['NTM/2', 'ERROR\t' + encodeField(String(message || 'Gateway error').slice(0, 500)), 'END', ''].join('\n');
}

function metadataResponse(item, data) {
  const page = data || {};
  const elements = Array.isArray(page.elements) ? page.elements : [];
  const lines = [
    'NTM/2',
    'SESSION\t' + String(item.id || ''),
    'DATASAVER\t' + (item.dataSaver ? '1' : '0'),
    'URL\t' + encodeField(metadataLimits.boundedString(page.url, metadataLimits.MAX_URL_CHARS)),
    'TITLE\t' + encodeField(metadataLimits.boundedString(page.title, metadataLimits.MAX_TITLE_CHARS)),
    'TEXT\t' + encodeField(metadataLimits.boundedString(page.text, metadataLimits.MAX_PAGE_TEXT_CHARS))
  ];
  elements.slice(0, metadataLimits.MAX_ELEMENTS).forEach((el, i) => {
    const element = el || {};
    const kind = ['link', 'input', 'button'].indexOf(element.kind) >= 0 ? element.kind : 'button';
    lines.push('ELEMENT\t' + (i + 1) + '\t' + kind + '\t' +
      boundedInteger(element.x, 0, -640, 640) + '\t' +
      boundedInteger(element.y, 0, -960, 960) + '\t' +
      boundedInteger(element.w, 0, 0, 640) + '\t' +
      boundedInteger(element.h, 0, 0, 960) + '\t' +
      encodeField(metadataLimits.boundedString(element.label, metadataLimits.MAX_ELEMENT_LABEL_CHARS)) + '\t' +
      encodeField(metadataLimits.boundedString(element.href, metadataLimits.MAX_ELEMENT_HREF_CHARS)) + '\t' +
      encodeField(metadataLimits.boundedString(element.inputType, metadataLimits.MAX_INPUT_TYPE_CHARS)) + '\t' +
      (element.focused ? '1' : '0'));
  });
  lines.push('END');
  return lines.join('\n') + '\n';
}

module.exports = { encodeField, errorResponse, metadataResponse, boundedInteger };
