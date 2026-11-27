# Velox (Android + Windows) — notas para quem mexe no código

## Regra de ouro: toda mudança de função vai para "O que há de novo"
- Qualquer melhoria que mude o que o app faz ou como se usa (função nova, fluxo melhorado, correção de erro visível) ganha uma linha **nos dois lugares**:
  - Android: `app/src/main/java/tech/vetorial/velox/Apresentacao.kt` (`NOVIDADES`).
  - Windows: `windows/apresentacao.js` (`NOVIDADES`).
- `id` = o maior id existente + 1; `tipo` = `novo`, `melhoria` ou `correcao`; `titulo` curto; `texto` em português simples, dizendo o que mudou para a pessoa (não o que mudou no código).
- A tela aparece sozinha depois da atualização e mostra só o que a pessoa ainda não viu (não depende do número da versão). Na primeira instalação ela não aparece: quem explica é a apresentação.
- Testes: `ApresentacaoTest.kt` (Android) e `windows/teste/apresentacao.test.js` conferem ids, tipos e textos.

## Apresentação da primeira abertura
- Tour → permissões explicadas → termos a aceitar (diálogos em `MainActivity.kt` e `windows/main.js`; conteúdo nos mesmos arquivos `Apresentacao`).
- Função nova: acrescente (ou ajuste) um passo em `TOUR`. Permissão nova: explique o porquê em `PERMISSOES` ANTES de pedir. Mudou o texto dos termos: suba `VERSAO_DOS_TERMOS`.
- Também ficam em Painel › Mais › Sobre o Velox (Android) e Ajustes › Sobre o Velox (Windows).

## Visual: telinhas e ícones seguem o padrão do app
- Toda janelinha, caixa ou menu novo usa o visual do próprio Velox (formato, cantos, fonte, cores do tema instalado), nunca o cinza padrão do sistema.
- Todo ícone novo segue o padrão dos ícones que já existem: traço fino arredondado (stroke ~1.9, `currentColor`, sem preenchimento), tamanho igual ao dos vizinhos e cores do tema. No Windows os ícones ficam em `IC` (`ui/pop.js`) e no Android no `Visual.kt`/`vgx.js`.
- As janelinhas do Windows (`ui/dialogo.html`, `ui/pop.html`) têm CSP sem `unsafe-inline`: o estilo vai em arquivo `.css`, nunca em `<style>` ou `style=""` dentro da página.


## Feedback e relato de erro
- Ajustes › Sobre o Velox tem "Enviar feedback" e "Reportar um erro": abrem, numa aba nova do próprio Velox, a página do servidor `/feedback?app=velox&v=<versão>&p=<plataforma>` (Android: ação `feedback` em `MainActivity.kt`; Windows: `ajustes:sobre` com `feedback` em `windows/main.js`). Os relatos chegam no Painel Dev › Feedback e geram aviso de erro.

## Edições: Velox Gaming e Velox Pro (um app só)
- O tema ganhou a chave `ed` (`gaming` ou `pro`, padrão `gaming`) e a base `grafite`. Escolher uma edição aplica um pacote de cores e estilo: `EDICOES` em `windows/tema.js` (`aplicarEdicao`) e `Tema.kt`, e `VGX.PRESETS` em `assets/vgx.js` (painel do Android e Ajustes do Windows). Mudou o pacote? Ajuste os três.
- O visual Pro vem de `html[data-edicao="pro"]` em `vgx.css` e `windows/ui/comum.css` (sem brilho, LED, trilhas, cantos cortados nem fundo animado); o logo Pro é desenhado em `desenharVGX` (`vgx.js`) e em `LogoVgx.pro` (`Visual.kt`). O que só existe no Gaming leva a classe `so-gaming` (some no Pro).
- Quem instala agora escolhe a edição na primeira abertura (`mostrarEscolhaDeEdicao` / `escolherEdicao`); quem já usava continua no Gaming. Dá para trocar em Painel › Visual (Android) e Ajustes › Tema (Windows). A edição vai junto na sincronização do tema.

## Abas dormindo, enviar aba, traduzir, PDF no Vindex e bloqueio mais forte
- **Abas dormindo:** regra pura em `windows/dormir.js` e `Dormir.kt` (testadas). Aba parada em segundo plano é esvaziada (`about:blank`) e volta ao `urlDormida` quando aparece em `mostrar()`. Nunca dorme: a da tela, anônima, carregando, com som, interna ou fora de http(s). Windows: varredura de 60 s em `main.js`, tempo em Ajustes › Desempenho (padrão 15 min) e MB liberados medidos por `app.getAppMetrics()` antes e depois (5 s). Android: `tickAbas` a cada 60 s com o app aberto, 15 min fixos, chave "Abas dormindo" no painel (Jogo); não dorme nada se `AudioManager.isMusicActive`. Os eventos de título/navegação ignoram a aba enquanto `dormindo`.
- **Enviar aba:** documento `velox-abas` do Vetorial ID (chave liberada em `servidor/src/id/conta.js`, `CHAVES_SYNC`): `{itens:[{id,url,titulo,de,deNome,t,vistoPor}]}`, até 20, 7 dias, só http(s). Lógica em `windows/abasenviadas.js` e `AbasEnviadas.kt` (mesmas regras; mudou de um lado? mude do outro). Quem envia nunca recebe de volta; cada aparelho marca como visto. Consulta a cada 60–90 s com o app aberto; Windows avisa com `Notification`, Android com Toast e lista em Abas (painel). Mudou a chave/formato? Confira o servidor.
- **Traduzir página:** `traduzir.js`/`Traduzir.kt` abrem `translate.google.com/translate?...&u=<página>`; o endereço vai ao Google só quando a pessoa pede (está nos termos, `VERSAO_DOS_TERMOS` = 3). O tradutor automático do Chromium continua desligado.
- **PDF no Vindex:** downloads prontos `.pdf` ganham "Abrir no Vindex". Android: `ACTION_VIEW` com `setPackage(tech.vetorial.vindex)` (já está em `<queries>`). Windows: `windows/vindex.js` acha `%LOCALAPPDATA%\Programs\Vindex\Vindex.exe` (instalação do Tauri só para o usuário) e abre o arquivo como argumento.
- **Bloqueio:** Windows usa `ElectronBlocker.fromPrebuiltFull` (anúncios, rastreadores e avisos de cookies), com reserva `fromPrebuiltAdsAndTracking` e depois a lista própria. Android: lista de domínios maior (`ListaDeAnuncios`) + `LimpezaDePagina` (estilo que esconde banners e avisos de cookies, via `addDocumentStartJavaScript`, segue o interruptor "Bloquear anúncios").
- Testes novos: `windows/teste/{dormir,abasenviadas,traduzir}.test.js` e `DormirTest`, `AbasEnviadasTest`, `TraduzirTest`, `AnunciosTest`. Só dá para testar de verdade no aparelho: dormir/acordar, notificação de aba recebida, abrir no Vindex e o Tradutor.

## Cuidado com o vgx.js (barra do Windows e painel do Android)
- `app/src/main/assets/vgx.js` tem um bloco `(function(){…})()` (aplica o tema, com a função `hex` própria) e depois `window.VGX = {…}` (desenhos). O que está no bloco NÃO existe fora dele: usar `hex` dentro de `pacoteSvg` derrubou as abas e os botões da barra do Windows (2026-11-26). `windows/teste/vgx.test.js` carrega o arquivo e desenha os ícones de todos os pacotes: rode `npm test` em `windows/` antes de publicar.

## Teste de segurança (igual em todos os apps da suíte)
- `windows/teste/seguranca.test.mjs` roda junto com os outros testes e na publicação: se falhar, a publicação para. Confere o repositório inteiro: segredo escrito no código (chaves, tokens, senhas), arquivo que nunca pode ser versionado (.env, chaves de assinatura), CSP do Tauri sem `unsafe-eval`/curinga, manifesto do Android (debug, tráfego http, componente exposto sem permissão, `QUERY_ALL_PACKAGES`), `eval`, HTML montado com texto (`innerHTML`), endereço `http://` fora da máquina local, segredo no log, HTTPS desligado e WebView com acesso perigoso.
- Exceção justificada = uma linha em `seguranca.permitidos.json` (regra, arquivo, trecho e MOTIVO de verdade); sem motivo, não vale. Texto digitado pela pessoa ou vindo de site/servidor nunca entra em HTML sem escapar (`textContent` ou `esc()`).
- O arquivo é o mesmo em todos os apps: mudou uma regra? Mude em todos. Achou uma brecha nova? Escreva primeiro um teste que a reproduz.
- Velox Android e Windows: o passo "Teste de segurança" também roda no fluxo do APK (`gerar-apk.yml`), e `npm test` do Windows pega `teste/*.test.mjs`. `usesCleartextTraffic` fica ligado de propósito (é navegador); endereço digitado sem esquema vira https.


## Manual do app (aparece no botão "Manual" do Painel Dev)
- `MANUAL.md` na raiz explica o app: pastas, onde fica cada coisa, como voltar atrás. É público: só diz ONDE ficam as chaves, nunca os valores.
- Mudança importante (função nova, pasta nova, arquivo que passa a mandar em algo, regra nova, decisão): acrescente uma linha NO TOPO da tabela "Movimentações importantes" do `MANUAL.md` (data, o que mudou, onde) e ajuste o trecho que ficou velho. Os últimos commits o Painel Dev já mostra sozinho; o manual guarda o que vale lembrar.
