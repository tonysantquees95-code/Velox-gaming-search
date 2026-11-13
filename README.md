# Velox Search

Navegador Android da Vetorial Tech, com visual gamer: abas, bloqueio de anúncios e rastreadores, visual todo personalizável (cores, bases, foto de fundo, pacotes de ícones, ícones próprios na barra de baixo), atalhos com logos das marcas, aba anônima, site para computador e downloads.

- Usa o WebView do Android (mesmo motor do Chrome); não é uma cópia nem usa marca ou arte de outro navegador.
- Kotlin, sem bibliotecas além do Android. Compila só no GitHub Actions (`Gerar APK`).
- O APK é assinado com a chave permanente guardada no servidor da Vênus (`.github/assinar-apk.sh`), para cada versão instalar por cima da anterior.

## Limites conhecidos

- Extensões do Chrome não rodam no WebView.
- Aba anônima: não guarda histórico nem cache e apaga os cookies quando a última anônima fecha (o WebView divide os cookies entre as abas).
- Bloqueio de anúncios por lista de domínios (cobre menos que um bloqueador completo).

## Personalização

- **Gaveta lateral** (botão do painel): Abas, Jogo, Visual, Fundo, Barra e Mais, com trilho de ícones.
- **Foto de fundo**: da galeria, por link https ou buscando na internet (segure o dedo numa imagem e escolha "Usar como fundo do Velox").
- **Sites no tema**: as páginas ficam transparentes e mostram o seu fundo, em vez de uma tela preta ou branca.
- **Barra de baixo**: três pacotes de ícones (linha, grossa, duplo) e foto própria em cada botão.
- **Vindex**: o botão "Abrir" na gaveta lança o Vindex (`tech.vetorial.vindex`) se estiver instalado.
