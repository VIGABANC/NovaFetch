/* Native messages are untrusted hints until checked by Kotlin's bridge. */
(function () {
  'use strict';
  const nativeApp = 'app.novafetch.media';
  const scanner = globalThis.NovaFetchMediaScanner;
  if (!scanner || !globalThis.browser?.runtime?.sendNativeMessage) return;
  const seen = new Set();
  const pending = new Set();
  let scheduled = false;
  const pageUrl = location.href;

  function report(candidates) {
    const fresh = [];
    for (const item of candidates) {
      if (!item || item.url.length > 2048 || seen.has(item.url)) continue;
      if (seen.size >= 500) break;
      seen.add(item.url);
      fresh.push(item);
    }
    for (let index = 0; index < fresh.length; index += 10) {
      const payload = {
        type: 'media_candidates', schemaVersion: 1,
        pageUrl, items: fresh.slice(index, index + 10),
      };
      browser.runtime.sendNativeMessage(nativeApp, payload).catch(() => {});
    }
  }

  report(scanner.collectCandidates(document, pageUrl));
  function scanPending() {
    scheduled = false;
    const found = [];
    for (const node of pending) {
      found.push(...scanner.collectCandidates(node, pageUrl, 20));
    }
    pending.clear();
    report(found);
  }
  const observer = new MutationObserver(records => {
    for (const record of records) {
      if (record.type === 'childList') {
        for (const n of record.addedNodes) {
          if (n.nodeType === 1 || n.tagName) pending.add(n);
        }
      } else if (record.type === 'attributes') {
        pending.add(record.target);
      }
    }
    if (pending.size > 0 && !scheduled) {
      scheduled = true;
      setTimeout(scanPending, 80);
    }
  });
  observer.observe(document, {
    subtree: true, childList: true, attributes: true,
    attributeFilter: ['src', 'href', 'content', 'type'],
  });
  globalThis.addEventListener?.('pagehide', () => observer.disconnect(), {once: true});
})();
