const test = require('node:test');
const assert = require('node:assert/strict');
const { collectCandidates, normalizeMediaUrl, classifyMedia } = require('../../browser-core/src/main/assets/extensions/media-detector/scan-media.js');

const node = (tagName, attrs = {}, children = []) => ({
  tagName: tagName.toUpperCase(),
  currentSrc: attrs.currentSrc || '',
  src: attrs.src || '', href: attrs.href || '',
  getAttribute(k) { return attrs[k] ?? null },
  querySelectorAll() { return children },
});
const page = 'https://example.org/article';

test('video currentSrc, audio, source and direct anchors are detected', () => {
  const elements = [
    node('video', { currentSrc: '/video.mp4' }),
    node('audio', { src: 'https://cdn.example.org/music.mp3' }),
    node('source', { src: '/movie.webm', type: 'video/webm' }),
    node('a', { href: '/file.m4a?expires=42' }),
  ];
  const results = collectCandidates({querySelectorAll: () => elements}, page);
  assert.equal(results.length, 4);
  assert.equal(results[0].url, 'https://example.org/video.mp4');
  assert.deepEqual(results.map(item => item.kind), ['video', 'audio', 'video', 'audio']);
});

test('social metadata is treated as a hint and not confused with download certainty', () => {
  const results = collectCandidates({querySelectorAll: () => [node('meta', { property: 'og:video', content: '/player.mp4' })]}, page);
  assert.equal(results.length, 1);
  assert.equal(results[0].method, 'metadata');
});

test('unsafe schemes and blob URLs are not sent as direct media', () => {
  const urls = ['javascript:alert(1)', 'file:///etc/passwd', 'data:text/plain,a', 'blob:https://example.org/xyz'];
  for (const url of urls) assert.equal(normalizeMediaUrl(url, page), null);
});

test('unrelated trackers and script files are ignored', () => {
  const results = collectCandidates({querySelectorAll: () => [node('a', {href:'/track.gif'}), node('a', {href:'/ads.js'}), node('a', {href:'/file.mp4'})]}, page);
  assert.equal(results.length, 1);
  assert.equal(results[0].url, 'https://example.org/file.mp4');
});

test('relative duplicate links collapse and result count is bounded', () => {
  const elements = Array.from({length: 70}, (_, i) => node('a', {href: i % 2 === 0 ? '/x.mp4' : '/x.mp4#fragment'}));
  assert.equal(collectCandidates({querySelectorAll: () => elements}, page, 10).length, 1);
});

test('kind is determined from file extension or MIME', () => {
  assert.equal(classifyMedia('https://x.test/stream', 'audio/mpeg'), 'audio');
  assert.equal(classifyMedia('https://x.test/media.m3u8', ''), 'manifest');
  assert.equal(classifyMedia('https://x.test/app.js', ''), null);
});

test('scanning a dynamically inserted subtree does not require full document scan', () => {
  const subtree = node('section', {}, [node('video', {src:'/new.mp4'})]);
  const found = collectCandidates(subtree, page);
  assert.equal(found.length, 1);
  assert.equal(found[0].url, 'https://example.org/new.mp4');
});
