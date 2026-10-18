/* NovaFetch clean-room DOM media candidate scanner. No network or privileged APIs. */
(function (root) {
  'use strict';
  const allowedTags = new Set(['VIDEO', 'AUDIO', 'SOURCE', 'A', 'META']);
  const mediaExtensions = /\.(mp4|m4v|webm|mov|mp3|m4a|aac|ogg|opus|wav|flac|m3u8|mpd)$/i;
  const metadataProperties = new Set(['og:video', 'og:video:url', 'og:video:secure_url', 'og:audio', 'og:audio:url', 'twitter:player:stream']);

  function normalizeMediaUrl(raw, base) {
    if (typeof raw !== 'string' || !raw.trim() || raw.length > 8192) return null;
    try {
      const url = new URL(raw.trim(), base);
      if (url.protocol !== 'https:' && url.protocol !== 'http:') return null;
      if (!url.hostname || url.username || url.password) return null;
      url.hash = '';
      return url.href;
    } catch (_) { return null; }
  }

  function classifyMedia(url, mime = '') {
    if (typeof url !== 'string') return null;
    const type = String(mime || '').toLowerCase().split(';')[0].trim();
    if (type === 'application/vnd.apple.mpegurl' || type === 'application/x-mpegurl' || type === 'application/dash+xml') return 'manifest';
    if (type.startsWith('video/')) return 'video';
    if (type.startsWith('audio/')) return 'audio';
    let path;
    try { path = new URL(url).pathname; } catch (_) { return null; }
    const match = path.match(mediaExtensions);
    if (!match) return null;
    const ext = match[1].toLowerCase();
    if (ext === 'm3u8' || ext === 'mpd') return 'manifest';
    if (['mp3', 'm4a', 'aac', 'ogg', 'opus', 'wav', 'flac'].includes(ext)) return 'audio';
    return 'video';
  }

  function candidateFor(element, base) {
    const tag = element?.tagName?.toUpperCase();
    if (!allowedTags.has(tag)) return null;
    let raw, mime = '', method = 'dom';
    if (tag === 'META') {
      const property = String(element.getAttribute?.('property') || element.getAttribute?.('name') || '').toLowerCase();
      if (!metadataProperties.has(property)) return null;
      raw = element.getAttribute('content');
      method = 'metadata';
      mime = property.includes('audio') ? 'audio/unknown' : 'video/unknown';
    } else if (tag === 'A') {
      raw = element.href || element.getAttribute?.('href');
      method = 'direct_link';
    } else {
      raw = element.currentSrc || element.src || element.getAttribute?.('src');
      mime = element.getAttribute?.('type') || '';
    }
    const url = normalizeMediaUrl(raw, base);
    if (!url) return null;
    const kind = classifyMedia(url, mime);
    if (!kind) return null;
    return { url, kind, method, mimeType: mime.slice(0, 100) };
  }

  function collectCandidates(rootNode, pageUrl, max = 40) {
    if (!rootNode || !normalizeMediaUrl(pageUrl, pageUrl)) return [];
    const limit = Number.isInteger(max) ? Math.max(1, Math.min(100, max)) : 40;
    const nodes = [];
    if (allowedTags.has(rootNode?.tagName?.toUpperCase())) nodes.push(rootNode);
    if (typeof rootNode.querySelectorAll === 'function') {
      for (const item of rootNode.querySelectorAll('video,audio,source,a,meta[property],meta[name]')) nodes.push(item);
    }
    const found = new Map();
    for (const el of nodes) {
      const item = candidateFor(el, pageUrl);
      if (item && !found.has(item.url)) found.set(item.url, item);
      if (found.size >= limit) break;
    }
    return [...found.values()];
  }

  const api = { normalizeMediaUrl, classifyMedia, collectCandidates };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.NovaFetchMediaScanner = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
