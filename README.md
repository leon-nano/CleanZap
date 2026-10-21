# Limpa Mídia

App Android que lista as fotos, vídeos, áudios, mensagens de voz e documentos do WhatsApp recebidos
**hoje** (ou ontem, 7 dias, 30 dias, tudo) e deixa você apagar os arquivos de uma vez. As mensagens
continuam na conversa; só os arquivos somem do celular.

## Como abrir e instalar

1. Instale o **Android Studio** (versão recente) e abra a pasta `LimpaMidia` (File > Open).
2. Aguarde o *Gradle Sync* terminar (na primeira vez baixa as dependências; precisa de internet).
   Se o Android Studio sugerir atualizar o plugin do Gradle ou do Kotlin, pode aceitar.
3. Para instalar direto no celular: ative **Opções do desenvolvedor > Depuração USB**, conecte o cabo
   e clique em **Run ▶**.
4. Para gerar um APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**. O arquivo fica em
   `app/build/outputs/apk/debug/app-debug.apk`. Envie para o celular e instale
   (o Android vai pedir para permitir "instalar apps desconhecidos").

## Alternativa sem instalar nada: gerar o APK no GitHub

1. Crie uma conta em github.com e um repositório novo (pode ser **privado**).
2. Envie todos os arquivos deste projeto para ele (na página do repositório: **Add file > Upload files**,
   arrastando o conteúdo da pasta `LimpaMidia`, **incluindo a pasta oculta `.github`**).
3. Abra a aba **Actions**. O fluxo "Gerar APK" roda sozinho a cada envio (ou clique em
   **Run workflow**). Leva cerca de 5 a 10 minutos.
4. Quando ficar verde, clique na execução e, em **Artifacts**, baixe **LimpaMidia-APK** (um ZIP com o
   `app-debug.apk`). Passe para o celular e instale.
5. Se ficar vermelho, abra a execução, copie o texto do erro e me mande.

## Como usar

1. Na primeira abertura, toque em **Escolher pasta do WhatsApp** e navegue até
   `Android > media > com.whatsapp > WhatsApp > Media`, depois **Usar esta pasta > Permitir**.
   (WhatsApp Business: `com.whatsapp.w4b > WhatsApp Business > Media`.)
   Em Android 10 ou anterior a pasta é `WhatsApp > Media`, na raiz do armazenamento.
2. A tela inicial mostra cada tipo de mídia com a quantidade e o tamanho do período escolhido
   (começa em **Hoje**).
3. Toque em um tipo, marque o que quer apagar (ou **Selecionar tudo**) e toque em **Apagar**.
   - Fotos/vídeos: toque seleciona, segurar abre no visualizador.
   - Mensagens de voz/áudios: botão ▶ para ouvir antes de decidir.

## Detalhes técnicos

- Kotlin + Jetpack Compose (Material 3), Coil para miniaturas (inclusive de vídeos).
- Acesso à pasta pelo Storage Access Framework: **nenhuma permissão** no manifest, sem internet.
- A data de cada arquivo vem do nome (`PTT-20261021-WA0001.opus`); se não tiver, usa a data de modificação.
- Mensagens de voz ficam em subpastas por semana (ex.: `202643`); o app varre todas e filtra pela data.
- minSdk 26 (Android 8.0), targetSdk 34.

## Estrutura

```
app/src/main/java/com/limpamidia/app/
├── MainActivity.kt          entrada do app
├── MainViewModel.kt         estado, varredura, seleção e exclusão
├── data/
│   ├── Model.kt             categorias, filtros de data, item de mídia
│   ├── WhatsAppRepository.kt  leitura/exclusão via SAF
│   └── AudioPlayer.kt       reprodução de áudios
└── ui/
    ├── Theme.kt
    └── Screens.kt           configuração inicial, início e lista por categoria
```
