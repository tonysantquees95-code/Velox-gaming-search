# Velox Search

Navegador Android da Vetorial Tech, com visual gamer: abas, bloqueio de anúncios e rastreadores, temas (Neon, Pôr do sol e Cromado), aba anônima, site para computador e downloads.

- Usa o WebView do Android (mesmo motor do Chrome); não é uma cópia nem usa marca ou arte de outro navegador.
- Kotlin, sem bibliotecas além do Android. Compila só no GitHub Actions (`Gerar APK`).
- O APK é assinado com a chave permanente guardada no servidor da Vênus (`.github/assinar-apk.sh`), para cada versão instalar por cima da anterior.

## Limites conhecidos

- Extensões do Chrome não rodam no WebView.
- Aba anônima: não guarda histórico nem cache e apaga os cookies quando a última anônima fecha (o WebView divide os cookies entre as abas).
- Bloqueio de anúncios por lista de domínios (cobre menos que um bloqueador completo).
