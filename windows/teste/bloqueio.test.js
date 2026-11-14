const test = require("node:test");
const assert = require("node:assert/strict");
const { dominioBloqueado } = require("../bloqueio.js");

test("bloqueia domínios de anúncio e subdomínios, e deixa o resto", () => {
  assert.equal(dominioBloqueado("https://securepubads.g.doubleclick.net/gpt/pubads_impl.js"), true);
  assert.equal(dominioBloqueado("https://doubleclick.net/x"), true);
  assert.equal(dominioBloqueado("https://www.google.com/search?q=a"), false);
  assert.equal(dominioBloqueado("https://notdoubleclick.net/"), false);
  assert.equal(dominioBloqueado("isso não é url"), false);
});
