'use strict';

const MAX_PAGE_TEXT_CHARS = 6000;
const MAX_TITLE_CHARS = 100;
const MAX_URL_CHARS = 1024;
const MAX_ELEMENTS = 24;
const MAX_ELEMENT_LABEL_CHARS = 72;
const MAX_ELEMENT_HREF_CHARS = 256;
const MAX_INPUT_TYPE_CHARS = 24;

function boundedString(value, limit) {
  return String(value == null ? '' : value).slice(0, limit);
}

module.exports = {
  MAX_PAGE_TEXT_CHARS,
  MAX_TITLE_CHARS,
  MAX_URL_CHARS,
  MAX_ELEMENTS,
  MAX_ELEMENT_LABEL_CHARS,
  MAX_ELEMENT_HREF_CHARS,
  MAX_INPUT_TYPE_CHARS,
  boundedString
};
