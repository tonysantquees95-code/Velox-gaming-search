const test = require("node:test");
const assert = require("node:assert/strict");
const { cpuPorcentagem, lerGpuContador, lerNvidia, lerTempAcpi, lerPing } = require("../desempenho");

const nucleo = (user, sys, idle) => ({ times: { user, nice: 0, sys, irq: 0, idle } });

test("uso do processador entre duas fotos", () => {
  const antes = [nucleo(100, 100, 800), nucleo(100, 100, 800)];
  const depois = [nucleo(200, 150, 850), nucleo(300, 100, 900)];
  // núcleo 1: 150 de trabalho e 50 ocioso; núcleo 2: 200 de trabalho e 100 ocioso -> 350/500 = 70%
  assert.equal(cpuPorcentagem(antes, depois), 70);
  assert.equal(cpuPorcentagem(antes, antes), 0);
});

test("GPU do contador do Windows", () => {
  assert.equal(lerGpuContador("37,5\r\n"), 38);
  assert.equal(lerGpuContador("250"), 100);
  assert.equal(lerGpuContador(""), null);
  assert.equal(lerGpuContador("erro"), null);
});

test("nvidia-smi", () => {
  assert.deepEqual(lerNvidia("45, 61, NVIDIA GeForce RTX 3060\r\n"), { uso: 45, temp: 61, nome: "NVIDIA GeForce RTX 3060" });
  assert.equal(lerNvidia(""), null);
  assert.equal(lerNvidia("N/A, N/A, X"), null);
});

test("temperatura ACPI em décimos de kelvin", () => {
  assert.equal(lerTempAcpi("3182"), 45);
  assert.equal(lerTempAcpi(""), null);
  assert.equal(lerTempAcpi("12"), null);
});

test("ping em português e inglês", () => {
  assert.equal(lerPing("Resposta de 1.1.1.1: bytes=32 tempo=14ms TTL=58"), 14);
  assert.equal(lerPing("Reply from 1.1.1.1: bytes=32 time=9ms TTL=58"), 9);
  assert.equal(lerPing("Resposta de 1.1.1.1: bytes=32 tempo<1ms TTL=58"), 1);
  assert.equal(lerPing("Esgotado o tempo limite do pedido."), null);
});
