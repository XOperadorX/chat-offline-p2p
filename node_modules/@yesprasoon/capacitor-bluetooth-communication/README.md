
# Capacitor Bluetooth Communication Plugin

The **BluetoothCommunicationPlugin** is a Capacitor plugin that provides Bluetooth Classic communication functionality. It supports managing Bluetooth devices (scanning, connecting, and disconnecting), as well as sending and receiving data over Bluetooth RFCOMM sockets. Web-specific implementations are available as stubs for testing purposes. **Note: This plugin currently supports Android only.**

---

![npm](https://img.shields.io/npm/v/@yesprasoon/capacitor-bluetooth-communication)
![npm downloads](https://img.shields.io/npm/dw/@yesprasoon/capacitor-bluetooth-communication)
![license](https://img.shields.io/github/license/yesprasoon/capacitor-bluetooth-communication)

## Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Platform Support](#platform-support)
- [Permissions](#permissions)
- [Usage](#usage)
  - [Initialization](#initialization)
  - [Enable Bluetooth](#enable-bluetooth)
  - [Scan Devices](#scan-devices)
  - [Start Server](#start-server)
  - [Stop Server](#stop-server)
  - [Connect](#connect)
  - [Disconnect](#disconnect)
  - [Send Data](#send-data)
  - [Listener](#listener)
- [Web Support](#web-support)
- [Example](#example)
- [License](#license)

## Features
- **Enable Bluetooth**: Enable Bluetooth on the device.
- **Scan Devices**: Scan for paired Bluetooth devices.
- **Start/Stop Server**: Start and stop a Bluetooth RFCOMM server for device connections.
- **Connect/Disconnect**: Connect to a Bluetooth device and disconnect from it.
- **Send Data**: Send data to the connected Bluetooth device.
- **Listen for Incoming Data**: Listen for incoming data from the connected Bluetooth device.

### Supported Platforms
- **Android**: Full functionality (Bluetooth scanning, connection, data transfer).
- **Web**: Stub implementation (for testing purposes, Bluetooth functionality is not supported on the web).

## Installation

To install the plugin, run the following command:

```bash
npm install @yesprasoon/capacitor-bluetooth-communication
npx cap sync
```

## Platform Support

- **Android**: Fully supported
- **Web**: Stubbed support (Bluetooth is not available on the web)
- **iOS**: Not supported by this plugin

## Permissions

### Android Permissions

This plugin requires Bluetooth and location permissions:

- **Bluetooth**:
  - `BLUETOOTH`
  - `BLUETOOTH_ADMIN`
  - `ACCESS_COARSE_LOCATION`
  - `ACCESS_FINE_LOCATION`
  
- **Bluetooth Scan**:
  - `BLUETOOTH_SCAN`
  - `BLUETOOTH_CONNECT`

### Web Permissions

For the web, Bluetooth functionalities are stubbed and not available.

## Usage

### Initialization

To initialize the plugin, call the `initialize` method:

```typescript
import { BluetoothCommunication } from '@yesprasoon/capacitor-bluetooth-communication';

BluetoothCommunication.initialize()
  .then(() => console.log('Bluetooth initialized'))
  .catch(error => console.error('Error initializing Bluetooth:', error));
```

### Enable Bluetooth

To enable Bluetooth on the device:

```typescript
BluetoothCommunication.enableBluetooth()
  .then(() => console.log('Bluetooth enabled'))
  .catch(error => console.error('Error enabling Bluetooth:', error));
```

### Scan Devices

To scan for paired Bluetooth devices:

```typescript
BluetoothCommunication.scanDevices()
  .then(result => {
    const devices = result.devices;
    console.log('Found devices:', devices);
  })
  .catch(error => console.error('Error scanning devices:', error));
```

### Start Server

To start the Bluetooth server:

```typescript
BluetoothCommunication.startServer()
  .then(() => console.log('Server started'))
  .catch(error => console.error('Error starting server:', error));
```

### Stop Server

To stop the Bluetooth server:

```typescript
BluetoothCommunication.stopServer()
  .then(() => console.log('Server stopped'))
  .catch(error => console.error('Error stopping server:', error));
```

### Connect

To connect to a Bluetooth device using its MAC address:

```typescript
const address = 'XX:XX:XX:XX:XX:XX'; // Replace with the device address
BluetoothCommunication.connect({ address })
  .then(() => console.log('Connected to device'))
  .catch(error => console.error('Error connecting to device:', error));
```

### Disconnect

To disconnect from the currently connected Bluetooth device:

```typescript
BluetoothCommunication.disconnect()
  .then(() => console.log('Disconnected'))
  .catch(error => console.error('Error disconnecting:', error));
```

### Send Data

To send data to the connected Bluetooth device:

```typescript
const data = 'Hello, Bluetooth!';
BluetoothCommunication.sendData({ data })
  .then(() => console.log('Data sent'))
  .catch(error => console.error('Error sending data:', error));
```

### Listener

To listen for incoming data:

```typescript
BluetoothCommunication.addListener('dataReceived', (event) => {
  console.log('Data received:', event.data);
});
```

## Web Support

On the web, Bluetooth functionalities are stubbed. You can still call methods, but they will not work in a web environment:

```typescript
BluetoothCommunication.enableBluetooth() // Throws unavailable error
BluetoothCommunication.scanDevices() // Returns empty list
BluetoothCommunication.startServer() // Throws unavailable error
```

## Example Usage

Here’s an example of a simple use case in an Ionic/Angular app:

### Server Flow

1. **Initialize the Plugin**: Set up Bluetooth on the device.
2. **Enable Bluetooth**: Ensure Bluetooth is enabled on the device.
3. **Start Listening for Connections**: Start the server to listen for incoming client connections.
4. **Handle Data Reception**: Use an event listener to receive data from the client.

```typescript
import { BluetoothCommunication } from '@yesprasoon/capacitor-bluetooth-communication';

// Initialize the Bluetooth plugin
await BluetoothCommunication.initialize();

// Enable Bluetooth if it's not already enabled
await BluetoothCommunication.enableBluetooth();

// Start the server to listen for incoming connections
await BluetoothCommunication.startServer();

// Listen for incoming data from connected clients
BluetoothCommunication.addListener('dataReceived', (data) => {
  console.log('Received data:', data);
});
```

### Client Flow

1. **Initialize the Plugin**: Set up Bluetooth on the device.
2. **Enable Bluetooth**: Ensure Bluetooth is enabled on the device.
3. **Start Listening for Connections**: Start the server to listen for incoming client connections.
4. **Scan for Devices**: Search for available paired devices.
5. **Connect to a Device**: Select a device and connect.
6. **Handle Data Reception**: Use an event listener to receive data from the client.

```typescript
import { BluetoothCommunication } from '@yesprasoon/capacitor-bluetooth-communication';

// Initialize the Bluetooth plugin
await BluetoothCommunication.initialize();

// Enable Bluetooth if it's not already enabled
await BluetoothCommunication.enableBluetooth();

// Scan for paired devices
const result = await BluetoothCommunication.scanDevices();
const device = result.devices[0]; // Select the first device (or whichever you prefer)

// Connect to the selected device
await BluetoothCommunication.connect({ address: device.address });

// Listen for incoming data from the server
BluetoothCommunication.addListener('dataReceived', (data) => {
  console.log('Received data:', data);
});

```
---

## License

This project is licensed under the MIT License. See the LICENSE file for more details.
