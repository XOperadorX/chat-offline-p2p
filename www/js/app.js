import { BluetoothCommunication } from '@yesprasoon/capacitor-bluetooth-communication';

let peer = null;
let conn = null;
let meuNome = '';
let meuId = '';
let btConectado = false;

const loginScreen = document.getElementById('login-screen');
const appScreen = document.getElementById('app-screen');
const displayName = document.getElementById('displayName');
const myIdText = document.getElementById('myIdText');
const myIdBox = document.getElementById('myIdBox');
const mensagensDiv = document.getElementById('mensagens');
const remoteIdInput = document.getElementById('remoteIdInput');
const msgInput = document.getElementById('msgInput');

// 1. LOGIN
document.getElementById('btnLogin').onclick = () => {
    meuNome = document.getElementById('pairName').value.trim();
    if (!meuNome) return alert('Digite um nome primeiro');
    
    meuId = meuNome.replace(/\s/g, '') + '-' + Math.floor(Math.random() * 9000 + 1000);
    
    myIdText.innerText = meuId;
    myIdBox.style.display = 'block';
    
    QRCode.toCanvas(document.getElementById('qrCanvas'), meuId, { 
        width: 200, margin: 2,
        color: { dark: '#000000', light: '#ffffff' }
    }, (error) => { if (error) console.error('Erro QR:', error); });
    
    loginScreen.style.display = 'none';
    appScreen.style.display = 'block';
    displayName.innerText = meuNome;
    
    // Tenta PeerJS em segundo plano (opcional)
    try {
        peer = new Peer(meuId, { debug: 1 });
        peer.on('error', (err) => console.log('Erro PeerJS:', err));
        peer.on('connection', (c) => { conn = c; setupConnection(); });
    } catch (e) { console.log('PeerJS falhou:', e); }
};

// 2. LÓGICA BLUETOOTH
document.getElementById('btnBtServer').onclick = async () => {
    try {
        await BluetoothCommunication.initialize();
        await BluetoothCommunication.enableBluetooth();
        await BluetoothCommunication.startServer();
        alert('✅ Bluetooth Ligado! Aguardando conexão...');
        
        BluetoothCommunication.addListener('dataReceived', (event) => {
            renderMsg(event.data, 'entrada', 'Bluetooth');
        });
        
        btConectado = true;
        showTab('chats');
    } catch (e) {
        alert('Erro ao ligar Bluetooth: ' + (e.message || e));
    }
};

document.getElementById('btnBtScan').onclick = async () => {
    try {
        await BluetoothCommunication.initialize();
        await BluetoothCommunication.enableBluetooth();
        
        const resultado = await BluetoothCommunication.scanDevices();
        const listaDiv = document.getElementById('listaBtDispositivos');
        listaDiv.innerHTML = '';
        
        if (resultado.devices.length === 0) {
            listaDiv.innerHTML = '<p style="color:#aaa;">Nenhum dispositivo pareado. Pareie no Android primeiro.</p>';
            return;
        }
        
        resultado.devices.forEach(device => {
            const btn = document.createElement('button');
            btn.innerText = `Conectar: ${device.name || device.address}`;
            btn.style.cssText = 'background:#25d366; margin:5px 0; width:100%; color:#000;';
            btn.onclick = async () => {
                try {
                    await BluetoothCommunication.connect({ address: device.address });
                    alert('✅ Conectado via Bluetooth!');
                    btConectado = true;
                    showTab('chats');
                } catch (e) {
                    alert('Erro ao conectar: ' + (e.message || e));
                }
            };
            listaDiv.appendChild(btn);
        });
    } catch (e) {
        alert('Erro ao procurar: ' + (e.message || e));
    }
};

// 3. CONECTAR POR ID (Wi-Fi)
document.getElementById('btnConnectId').onclick = () => {
    const remoteId = remoteIdInput.value.trim();
    if (!remoteId) return alert('Digite o ID do outro celular');
    if (!peer) return alert('Aguarde 2 segundos e tente novamente...');
    
    conn = peer.connect(remoteId);
    setupConnection();
};

function setupConnection() {
    if (!conn) return;
    conn.on('open', () => { alert('Conectado via Wi-Fi!'); showTab('chats'); });
    conn.on('data', (data) => {
        if (data.tipo === 'arquivo') renderMsg(`📎 Arquivo: ${data.nome}`, 'entrada');
        else renderMsg(data.texto, 'entrada', data.de);
    });
    conn.on('close', () => alert('Conexão Wi-Fi encerrada'));
}

// 4. ENVIO DE MENSAGEM (Tenta Bluetooth, depois Wi-Fi)
document.getElementById('btnSend').onclick = async () => {
    const texto = msgInput.value.trim();
    if (!texto) return;
    
    // Tenta Bluetooth primeiro
    if (btConectado) {
        try {
            await BluetoothCommunication.sendData({ data: texto });
            renderMsg(texto, 'saida', meuNome);
            msgInput.value = '';
            return;
        } catch (e) {
            console.log('Falha no Bluetooth, tentando Wi-Fi...');
        }
    }
    
    // Fallback Wi-Fi
    if (conn && conn.open) {
        conn.send({ de: meuNome, texto: texto });
        renderMsg(texto, 'saida', meuNome);
        msgInput.value = '';
    } else {
        alert('Você não está conectado a ninguém.');
    }
};

// 5. AUXILIARES
function renderMsg(texto, tipo, autor = '') {
    const div = document.createElement('div');
    div.className = tipo;
    const hora = new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    div.innerHTML = `<b>${autor || (tipo === 'saida' ? 'Eu' : 'Outro')}</b><p>${texto}</p><small>${hora}</small>`;
    mensagensDiv.appendChild(div);
    mensagensDiv.scrollTop = mensagensDiv.scrollHeight;
}

window.showTab = (tab) => {
    document.getElementById('tab-parear').style.display = tab === 'parear' ? 'block' : 'none';
    document.getElementById('tab-chats').style.display = tab === 'chats' ? 'block' : 'none';
    document.querySelectorAll('.tabs button').forEach(b => b.classList.remove('active-tab'));
    if (event && event.target) event.target.classList.add('active-tab');
};

document.getElementById('btnScan').onclick = () => showTab('parear');