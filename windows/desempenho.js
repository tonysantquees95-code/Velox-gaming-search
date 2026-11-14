// Medidas do computador para a game bar: processador, memória, placa de vídeo, temperaturas, ping e FPS da página.
// As leituras do Windows (PowerShell, nvidia-smi, ping) ficam em funções separadas; o que interpreta texto é puro e testado.
const os = require("os");
const { execFile } = require("child_process");

/** Uso do processador (0 a 100) entre duas fotos de os.cpus(). */
function cpuPorcentagem(antes, depois) {
  let total = 0, ocioso = 0;
  for (let i = 0; i < depois.length && i < antes.length; i++) {
    const a = antes[i].times, d = depois[i].times;
    const soma = (t) => t.user + t.nice + t.sys + t.irq + t.idle;
    total += soma(d) - soma(a);
    ocioso += d.idle - a.idle;
  }
  return total > 0 ? Math.max(0, Math.min(100, Math.round(100 * (1 - ocioso / total)))) : 0;
}

/** Saída do Get-Counter somada ("37,5" ou "37.5"): vira inteiro de 0 a 100, ou null se não for número. */
function lerGpuContador(texto) {
  const n = parseFloat(String(texto || "").trim().replace(",", "."));
  return Number.isFinite(n) ? Math.max(0, Math.min(100, Math.round(n))) : null;
}

/** nvidia-smi --query-gpu=utilization.gpu,temperature.gpu,name --format=csv,noheader,nounits */
function lerNvidia(texto) {
  const linha = String(texto || "").split(/\r?\n/).map((l) => l.trim()).filter(Boolean)[0];
  if (!linha) return null;
  const p = linha.split(",").map((x) => x.trim());
  const uso = parseInt(p[0], 10), temp = parseInt(p[1], 10);
  if (!Number.isFinite(uso) && !Number.isFinite(temp)) return null;
  return { uso: Number.isFinite(uso) ? uso : null, temp: Number.isFinite(temp) ? temp : null, nome: p.slice(2).join(",") || "" };
}

/** MSAcpi_ThermalZoneTemperature vem em décimos de kelvin. */
function lerTempAcpi(texto) {
  const k = parseFloat(String(texto || "").trim().replace(",", "."));
  if (!Number.isFinite(k) || k < 2000 || k > 4000) return null;
  return Math.round(k / 10 - 273.15);
}

/** "tempo=12ms", "time=12ms", "tempo<1ms" (Windows em português ou inglês). */
function lerPing(texto) {
  const m = /(?:tempo|time)\s*[=<]\s*(\d+)\s*ms/i.exec(String(texto || ""));
  return m ? Number(m[1]) : null;
}

const SCRIPT_FPS = "(function(){if(!window.__vxfps){var n=0,t=performance.now();window.__vxfps={fps:0};(function l(){n++;var a=performance.now();if(a-t>=1000){window.__vxfps.fps=Math.round(n*1000/(a-t));n=0;t=a;}requestAnimationFrame(l);})();}return window.__vxfps.fps;})()";

function rodar(arquivo, args, ms = 4000) {
  return new Promise((resolve) => {
    try { execFile(arquivo, args, { timeout: ms, windowsHide: true }, (erro, saida) => resolve(erro ? "" : String(saida))); } catch { resolve(""); }
  });
}
const ps = (comando) => rodar("powershell.exe", ["-NoProfile", "-NonInteractive", "-Command", comando]);

class Medidor {
  /** pegarFps: () => Promise<number|null> com o FPS da página em primeiro plano; aoMedir recebe as medidas a cada segundo. */
  constructor(pegarFps, aoMedir) {
    this.pegarFps = pegarFps; this.aoMedir = aoMedir;
    this.m = { fps: null, cpu: null, ram: null, gpu: null, tempGpu: null, tempCpu: null, ping: null, gpuNome: "" };
    this.timer = null; this.tick = 0; this.cpus = null; this.ocupado = {};
    this.semNvidia = false; this.semAcpi = false;
  }
  iniciar() {
    if (this.timer) return;
    this.cpus = os.cpus(); this.tick = 0;
    this.timer = setInterval(() => void this.passo(), 1000);
    void this.passo();
  }
  parar() { if (this.timer) clearInterval(this.timer); this.timer = null; }
  // cada leitura lenta roda uma de cada vez, para nunca se acumular
  async uma(nome, f) { if (this.ocupado[nome]) return; this.ocupado[nome] = true; try { await f(); } finally { this.ocupado[nome] = false; } }
  async passo() {
    const agora = os.cpus();
    this.m.cpu = cpuPorcentagem(this.cpus, agora); this.cpus = agora;
    this.m.ram = Math.round(100 * (1 - os.freemem() / os.totalmem()));
    try { this.m.fps = await this.pegarFps(); } catch { this.m.fps = null; }
    if (this.tick % 3 === 0) {
      void this.uma("gpu", async () => {
        if (!this.semNvidia) {
          const n = lerNvidia(await rodar("nvidia-smi", ["--query-gpu=utilization.gpu,temperature.gpu,name", "--format=csv,noheader,nounits"]));
          if (n) { this.m.gpu = n.uso; this.m.tempGpu = n.temp; this.m.gpuNome = n.nome; return; }
          this.semNvidia = true; // sem nvidia-smi: usa o contador do Windows (serve para qualquer placa)
        }
        this.m.gpu = lerGpuContador(await ps("(Get-Counter '\\GPU Engine(*engtype_3D)\\Utilization Percentage' -ErrorAction SilentlyContinue).CounterSamples | Measure-Object CookedValue -Sum | Select-Object -ExpandProperty Sum"));
      });
    }
    if (this.tick % 5 === 0 && !this.semAcpi) {
      void this.uma("acpi", async () => {
        const t = lerTempAcpi(await ps("(Get-CimInstance -Namespace root/wmi -ClassName MSAcpi_ThermalZoneTemperature -ErrorAction SilentlyContinue | Select-Object -First 1).CurrentTemperature"));
        if (t == null) this.semAcpi = true; else this.m.tempCpu = t;
      });
    }
    if (this.tick % 5 === 2) {
      void this.uma("ping", async () => { this.m.ping = lerPing(await rodar("ping", ["-n", "1", "-w", "1500", "1.1.1.1"], 3000)); });
    }
    this.tick++;
    this.aoMedir({ ...this.m });
  }
}

module.exports = { Medidor, cpuPorcentagem, lerGpuContador, lerNvidia, lerTempAcpi, lerPing, SCRIPT_FPS };
