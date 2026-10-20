import { BleClient } from '@capacitor-community/bluetooth-le';

let meuNome, meuId, deviceConectado = null;
const SERVICE_UUID = '0000feed-0000-1000-8000-00805f9b34fb';
const CHAR_UUID = '0000beef-0000-1000-8000-00805f9b34fb';

// LOGIN COM QR
document.getElementById('btnLogin').onclick = async () => {
  meuNome = document.getElementById('pairName').value.trim();
  if(!meuNome) return alert('Digite nome');
  meuId = meuNome.replace(/\s/g,'-') + '-' + Math.floor(Math.random()*900+100);
  localStorage.setItem('pairName', meuNome);

  document.getElementById('login-screen').style.display='none';
  document.getElementById('app-screen').style.display='block';
  document.getElementById('displayName').innerText = meuNome;
  document.getElementById('displayId').innerText = meuId;

  // Gera QR com seu ID
  QRCode.toCanvas(document.getElementById('qrCanvas'), meuId, {width:200});
  
  await BleClient.initialize();
  // Fica anunciando via BT
  await BleClient.startAdvertising({
    name: meuId,
    service: SERVICE_UUID
  });
};

// ESCANEAR BLUETOOTH DIRETO SEM WI-FI
document.getElementById('btnScanBT').onclick = async () => {
  await BleClient.requestLEScan({services:[SERVICE_UUID]}, (result) => {
    const div = document.createElement('div');
    div.innerHTML = `<b>${result.device.name || result.device.deviceId}</b> <button onclick="conectarBT('${result.device.deviceId}')">Conectar</button>`;
    document.getElementById('listaBT').appendChild(div);
  });
  setTimeout(()=>BleClient.stopLEScan(), 10000);
};

window.conectarBT = async (deviceId) => {
  await BleClient.connect(deviceId, (id)=>console.log('desconectado',id));
  deviceConectado = deviceId;
  alert('Pareado via Bluetooth com: ' + deviceId);
  // Cria serviço de chat
  await BleClient.startNotifications(deviceId, SERVICE_UUID, CHAR_UUID, (value)=>{
    const texto = new TextDecoder().decode(value);
    const data = JSON.parse(texto);
    renderMsg(data, 'entrada');
  });
  showTab('chats');
};

// ENVIAR QUALQUER COISA - texto, imagem, pdf, apk, etc
document.getElementById('btnFile').onclick = () => document.getElementById('fileInput').click();

document.getElementById('fileInput').onchange = async (e) => {
  const file = e.target.files[0];
  if(!file) return;
  const buffer = await file.arrayBuffer();
  // Divide em chunks de 512 bytes (limite BLE)
  const chunkSize = 500;
  for(let i=0; i<buffer.byteLength; i+=chunkSize){
    const chunk = buffer.slice(i, i+chunkSize);
    const payload = {
      de: meuNome,
      nomeArquivo: file.name,
      tipo: file.type,
      tamanho: file.size,
      index: i/chunkSize,
      total: Math.ceil(buffer.byteLength/chunkSize),
      data: Array.from(new Uint8Array(chunk))
    };
    await enviarPayload(payload);
  }
  renderMsg({de:meuNome, texto:`📎 Enviado: ${file.name} (${(file.size/1024).toFixed(1)}KB)`, data:new Date().toLocaleString()}, 'saida');
};

document.getElementById('btnSend').onclick = async () => {
  const texto = document.getElementById('msgInput').value;
  if(!texto) return;
  const payload = {de:meuNome, texto, data:new Date().toLocaleString(), tipoMsg:'texto'};
  await enviarPayload(payload);
  renderMsg(payload, 'saida');
  document.getElementById('msgInput').value='';
};

async function enviarPayload(obj){
  const json = JSON.stringify(obj);
  const bytes = new TextEncoder().encode(json);
  if(deviceConectado){
    await BleClient.write(deviceConectado, SERVICE_UUID, CHAR_UUID, bytes);
  }
}

// QR CODE READER PARA PAREAMENTO
document.getElementById('btnQrScan').onclick = async () => {
  const video = document.getElementById('video');
  document.getElementById('qrReader').style.display='block';
  const stream = await navigator.mediaDevices.getUserMedia({video:{facingMode:'environment'}});
  video.srcObject = stream;
  // usa BarcodeDetector se tiver
  const detector = new BarcodeDetector({formats:['qr_code']});
  setInterval(async ()=>{
    try{
      const codes = await detector.detect(video);
      if(codes.length){
        const id = codes[0].rawValue;
        document.getElementById('qrReader').style.display='none';
        stream.getTracks().forEach(t=>t.stop());
        conectarBT(id); // conecta pelo ID do QR
      }
    }catch{}
  }, 500);
};

function renderMsg(m, tipo){
  const div=document.createElement('div');
  div.className=tipo;
  // Se for arquivo, cria link download
  if(m.nomeArquivo){
    div.innerHTML=`<b>${m.de}</b><p>📁 ${m.nomeArquivo}</p><button onclick="baixarArquivo('${m.nomeArquivo}')">Baixar</button><small>${m.data}</small>`;
  }else{
    div.innerHTML=`<b>${m.de}</b><p>${m.texto}</p><small>${m.data}</small>`;
  }
  document.getElementById('mensagens').appendChild(div);
}

window.showTab = (t)=>{
  document.getElementById('tab-parear').style.display=t==='parear'?'block':'none';
  document.getElementById('tab-chats').style.display=t==='chats'?'block':'none';
};