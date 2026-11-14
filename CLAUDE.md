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
