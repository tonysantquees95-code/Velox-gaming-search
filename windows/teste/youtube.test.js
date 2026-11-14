// Confere que o script do YouTube tira os campos de anúncio das respostas do player.
const fs = require("fs"), vm = require("vm"), path = require("path"), assert = require("assert");
const ctx = {
  document: { getElementById: () => null, createElement: () => ({}), head: { appendChild() {} }, addEventListener() {}, querySelector: () => null, querySelectorAll: () => [], documentElement: {} },
  Response: function () {}, setInterval() {}, MutationObserver: function () { this.observe = () => {}; }, JSON,
};
ctx.Response.prototype.json = function () { return Promise.resolve({}); };
ctx.window = ctx;
vm.createContext(ctx);
vm.runInContext(fs.readFileSync(path.join(__dirname, "..", "..", "app", "src", "main", "assets", "youtube.js"), "utf8"), ctx);
const r = ctx.JSON.parse('{"adPlacements":[1],"playerAds":[2],"playerResponse":{"adSlots":[3]},"x":1}');
assert.strictEqual(JSON.stringify(r), JSON.stringify({ adPlacements: [], playerAds: [], playerResponse: { adSlots: [] }, x: 1 }));
ctx.ytInitialPlayerResponse = { adPlacements: [1], a: 2 };
assert.strictEqual(JSON.stringify(ctx.ytInitialPlayerResponse), JSON.stringify({ adPlacements: [], a: 2 }));
console.log("youtube ok");
