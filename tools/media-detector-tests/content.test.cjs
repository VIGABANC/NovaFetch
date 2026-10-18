const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../browser-core/src/main/assets/extensions/media-detector');
const source = fs.readFileSync(path.join(root, 'scan-media.js'), 'utf8');
const content = fs.readFileSync(path.join(root, 'content.js'), 'utf8');
const tag = (name, src) => ({tagName:name.toUpperCase(),src, currentSrc:'',getAttribute(k){return k === 'src' ? src : null},querySelectorAll(){return []}});
function browserHarness(initial) {
  const messages = [], observers = [], timers = [];
  const document = {querySelectorAll() {return initial}, title:'My page'};
  class MutationObserver { constructor(cb){this.cb=cb; observers.push(this)} observe() {} disconnect() {} }
  const browser = {runtime: {sendNativeMessage: (_app,payload) => { messages.push(payload); return Promise.resolve(); }}};
  const context = {document,location:{href:'https://example.org/page'},browser,MutationObserver,
    setTimeout:(cb)=>{timers.push(cb);return timers.length}, clearTimeout(){},addEventListener(){}, URL};
  vm.createContext(context);
  vm.runInContext(source,context);
  vm.runInContext(content,context);
  return {messages,observers,timers,document};
}
test('initial detection reports HTTP video in bounded native message', () => {
  const h = browserHarness([tag('video','/video.mp4')]);
  assert.equal(h.messages.length,1);
  assert.equal(h.messages[0].type,'media_candidates');
  assert.equal(h.messages[0].schemaVersion,1);
  assert.equal(h.messages[0].items[0].url,'https://example.org/video.mp4');
});
test('dynamic subtree detection avoids a whole-document rescan', () => {
  let reads = 0;
  const h = browserHarness([]);
  h.document.querySelectorAll = () => { reads++; throw new Error('scanned entire page again') };
  h.observers[0].cb([{type:'childList',addedNodes:[tag('video','/new.mp4')]}]);
  while(h.timers.length) h.timers.shift()();
  assert.equal(reads,0);
  assert.equal(h.messages.length,1);
  assert.equal(h.messages[0].items[0].url,'https://example.org/new.mp4');
});
test('same media is not reported repeatedly', () => {
  const h = browserHarness([tag('video','/video.mp4')]);
  h.observers[0].cb([{type:'childList',addedNodes:[tag('video','/video.mp4')]}]);
  while(h.timers.length) h.timers.shift()();
  assert.equal(h.messages.length,1);
});
test('changed src attribute triggers detection and unsafe source is ignored', () => {
  const h = browserHarness([]);
  const changed=tag('video','https://example.org/fresh.mp4');
  h.observers[0].cb([{type:'attributes',target:changed,attributeName:'src'}]);
  while(h.timers.length) h.timers.shift()();
  assert.equal(h.messages.at(-1).items[0].url,'https://example.org/fresh.mp4');
  const bad=tag('video','file:///private');
  h.observers[0].cb([{type:'attributes',target:bad,attributeName:'src'}]);
  while(h.timers.length) h.timers.shift()();
  assert.equal(h.messages.length,1);
});
