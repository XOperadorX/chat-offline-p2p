markdown
# 📱 Comunicador Local (Chat Offline P2P)

Aplicativo Android de comunicação direta sem internet, usando **Bluetooth Clássico** (~10m) e **Wi-Fi/WebRTC** (~100m, dependendo da rede).

---

## ✨ Funcionalidades
- 🔵 **Bluetooth Clássico**: conexão direta entre aparelhos pareados (~10 metros).
- 📶 **Wi-Fi / WebRTC**: conexão via rede local ou hotspot (PeerJS).
- 💬 Conversa em tempo real entre dois dispositivos.
- 📅 Horário das mensagens exibido.
- 📋 Histórico da conversa em memória (durante a sessão).
- 🔑 Sem necessidade de internet (para o Bluetooth) ou rede móvel.
- 📷 Geração de QR Code e ID único para pareamento rápido.

---

## 📲 Como Usar

### 🔵 Via Bluetooth (Recomendado para uso offline)
1. Ative o Bluetooth nos dois celulares.
2. **Pareie os aparelhos** nas configurações de Bluetooth do próprio Android (isso é obrigatório para o app encontrar o dispositivo).
3. Abra o app nos dois celulares e insira os nomes.
4. **Celular A**: Toque em **"📡 Ficar Disponível (Servidor)"**. O app vai pedir permissões de Bluetooth, aceite todas.
5. **Celular B**: Toque em **"🔍 Procurar Dispositivos"**. O nome do Celular A vai aparecer na lista.
6. **Celular B**: Toque no nome do Celular A para conectar.
7. Conectado! ✅ Digite e envie mensagens.

### 🌐 Via Wi-Fi / PeerJS (Alternativa)
1. Conecte os dois celulares na **mesma rede Wi-Fi** (ou um celular cria um Hotspot e o outro se conecta).
2. Abra o app nos dois celulares e insira os nomes.
3. Veja o ID gerado na tela (ex: `Fabio-1234`).
4. No outro celular, vá na aba **Parear** e digite o ID no campo "ID do outro celular".
5. Toque em **Conectar por ID**.
6. Conectado! ✅ Digite e envie mensagens.

*(Nota: O modo Wi-Fi exige que os aparelhos tenham se conectado ao servidor de sinalização PeerJS pelo menos uma vez, o que requer uma conexão mínima com a internet).*

---

## ⚠️ Permissões Necessárias

O aplicativo solicita permissões para:
- 🔵 **Bluetooth** — escaneamento, anúncio e conexão.
- 📍 **Localização** — obrigatória para descoberta de dispositivos Bluetooth em versões antigas do Android.

### 📋 Permissões no `AndroidManifest.xml`

Para garantir que o Bluetooth funcione corretamente, as seguintes permissões estão declaradas:

- **Bluetooth (Android 12+)**: `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`
- **Bluetooth (Legado / Android 11-)**: `BLUETOOTH`, `BLUETOOTH_ADMIN`
- **Localização**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- **Internet (para PeerJS)**: `INTERNET`

Na primeira vez que o aplicativo for aberto e você usar a função Bluetooth, ele solicitará dinamicamente essas permissões em tempo de execução. É necessário que o usuário clique em **"Permitir"**.

---

## 📂 Estrutura do Projeto

```text
chat-offline-p2p/
│
├── package.json
│   └── @capacitor/core, @capacitor/android, @capacitor/cli
│   └── @yesprasoon/capacitor-bluetooth-communication (Bluetooth)
│   └── peerjs, qrcode
│
├── capacitor.config.json
│   └── appId: com.fabio.chatoffline
│   └── webDir: www
│
├── www/  <-- ESSA PASTA VAI PRO ANDROID
│   ├── index.html
│   │   ├── Tela Login: input nome + geração de ID/QR
│   │   ├── Header: mostra "Logado como: Fabio"
│   │   ├── Tabs: Parear / Chat
│   │   ├── Pareamento: Botões BT (Servidor/Scanner) + Input de ID
│   │   └── ChatBox com entrada/saída + data/hora
│   │
│   ├── css/
│   │   └── style.css
│   │       └── Tema escuro, bolhas de chat, botões arredondados
│   │
│   ├── lib/
│   │   ├── peerjs.min.js  (Biblioteca local)
│   │   └── qrcode.min.js  (Biblioteca local)
│   │
│   └── js/
│       └── app.js
│           ├── Lógica de Login e Geração de QR
│           ├── Inicialização do Servidor Bluetooth
│           ├── Scanner de Dispositivos Bluetooth Pareados
│           ├── Conexão via PeerJS (Fallback Wi-Fi)
│           ├── renderMsg() -> mostra entrada/saída + horário
│           └── showTab() -> navegação entre abas
│
├── android/  <-- CRIADO APÓS npx cap add android
│   ├── app/
│   │   └── src/main/
│   │       ├── AndroidManifest.xml  <-- Permissões BT declaradas
│   │       └── java/com/fabio/chatoffline/MainActivity.java
│   └── gradle/
│
└── node_modules/
🛠️ Como Compilar o APK
Instale as dependências:

bash
npm install
Sincronize com o Android:

bash
npx cap sync android
Abra no Android Studio:

bash
npx cap open android
No Android Studio, aguarde o Gradle sincronizar e vá em:
Build > Build Bundle(s) / APK(s) > Build APK(s).

O APK estará em android/app/build/outputs/apk/debug/.

🔄 Fluxo de Funcionamento (Bluetooth)
text
[ CELULAR A: Fabio-342 ]
       |
       |  1. Gera QR Code com ID
       |  2. Inicia Servidor Bluetooth (RFCOMM)
       |  3. Mostra "Logado como: Fabio" na tela
       |
   ---- BLUETOOTH CLÁSSICO (10m) ----
       |
[ CELULAR B: Maria-718 ]
       |
       |  1. Escaneia dispositivos BT pareados
       |  2. Conecta ao Fabio-342
       |  3. Abre canal de dados
       |
       +---> Conversa P2P: {"de":"Fabio","texto":"oi","data":"19/10/2026 14:22"}
Nota: O Bluetooth Clássico exige que os aparelhos estejam previamente pareados no sistema Android para que o aplicativo possa encontrá-los e conectar-se.

text

### O que mudou:
- **Tecnologia corrigida**: O README agora cita o plugin correto (`@yesprasoon/...`) e o Bluetooth Clássico.
- **Passo a passo do Bluetooth**: Inclui a instrução vital de **parear os celulares nas configurações do Android** antes de usar o app.
- **Estrutura de pastas**: Atualizada para refletir os arquivos reais (`lib/`, `app.js`).
- **Fluxo visual**: Diagrama atualizado para mostrar o funcionamento do servidor Bluetooth.