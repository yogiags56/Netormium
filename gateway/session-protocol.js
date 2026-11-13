'use strict';

const metadataLimits = require('./metadata-limits');

function encodeField(value) {
  return encodeURIComponent(String(value == null ? '' : value));
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
    'URL\t' + encodeField(page.url),
    'TITLE\t' + encodeField(page.title),
    'TEXT\t' + encodeField(page.text)
  ];
  elements.slice(0, 24).forEach((el, i) => {
    const element = el || {};
    lines.push('ELEMENT\t' + (i + 1) + '\t' + String(element.kind || 'button') + '\t' +
      Number(element.x || 0) + '\t' + Number(element.y || 0) + '\t' +
      Number(element.w || 0) + '\t' + Number(element.h || 0) + '\t' +
      encodeField(metadataLimits.boundedString(element.label, metadataLimits.MAX_ELEMENT_LABEL_CHARS)) + '\t' +
      encodeField(metadataLimits.boundedString(element.href, metadataLimits.MAX_ELEMENT_HREF_CHARS)) + '\t' +
      encodeField(metadataLimits.boundedString(element.inputType, metadataLimits.MAX_INPUT_TYPE_CHARS)) + '\t' +
      (element.focused ? '1' : '0'));
  });
  lines.push('END');
  return lines.join('\n') + '\n';
}

module.exports = { encodeField, errorResponse, metadataResponse };
