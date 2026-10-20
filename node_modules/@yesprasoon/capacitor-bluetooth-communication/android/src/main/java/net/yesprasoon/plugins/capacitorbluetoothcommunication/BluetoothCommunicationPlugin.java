package net.yesprasoon.plugins.capacitorbluetoothcommunication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.content.Intent;
import android.util.Log;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.ParcelUuid;
import java.nio.charset.StandardCharsets;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import android.app.Activity;

@CapacitorPlugin(name = "BluetoothCommunication",
    permissions = {
        @Permission(alias = "bluetooth", strings = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        }),
        @Permission(alias = "bluetoothScan", strings = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        })
    }
)

public class BluetoothCommunicationPlugin extends Plugin {

    private static final String TAG = "BluetoothCommunication";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket serverSocketConnection; // Server-specific socket
    private BluetoothSocket clientSocketConnection; // Client-specific socket
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean listening = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private BluetoothServerSocket serverSocket; // For the server
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    @Override
    public void load() {
        super.load();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device.");
        }

        enableBluetoothLauncher = getActivity().registerForActivityResult(
            new StartActivityForResult(),
            this::handleBluetoothEnableResult
        );

    }

    @Override
    public void handleOnDestroy() {
        // Stop the server if running
        if (serverSocket != null) {
            try {
                stopServer(null); // Pass null since no PluginCall is available during onDestroy
            } catch (Exception e) {
                Log.e(TAG, "Error stopping server during destroy: " + e.getMessage(), e);
            }
        }

        // Disconnect any active client connection
        if (clientSocketConnection != null && clientSocketConnection.isConnected()) {
            try {
                disconnect(null); // Pass null since no PluginCall is available during onDestroy
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting client during destroy: " + e.getMessage(), e);
            }
        }

        // Shut down the ExecutorService
        executorService.shutdownNow();

        // Call the superclass method
        super.handleOnDestroy();
    }

    private void setupStreams(BluetoothSocket socket) throws IOException {
         if (inputStream != null) {
            inputStream.close();
        }
        if (outputStream != null) {
            outputStream.close();
        }
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        if (bluetoothAdapter == null) {
            call.reject("Bluetooth is not supported on this device.");
            return;
        }
        call.resolve();
    }

    @PluginMethod
    public void enableBluetooth(PluginCall call) {
        // Check if Bluetooth permissions are granted
        if (!hasPermissions()) {
            requestBluetoothPermissions(call);
            return;
        }

        if (bluetoothAdapter == null) {
            call.reject("Bluetooth is not supported on this device.");
            return;
        }
        
        // Check if Bluetooth is already enabled
        if (bluetoothAdapter.isEnabled()) {
            call.resolve();
            return;
        }

        // If Bluetooth is not enabled, prompt the user to enable it
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        saveCall(call); // Save the call for resolving/rejecting later
        enableBluetoothLauncher.launch(enableBtIntent);
    }

    @PluginMethod
    public void scanDevices(PluginCall call) {
        // Check if Bluetooth permissions are granted
        if (!hasPermissions()) {
            requestBluetoothPermissions(call);
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            call.reject("Bluetooth must be enabled to scan for devices.");
            return;
        }
        
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            JSArray devices = new JSArray();

            for (BluetoothDevice device : pairedDevices) {
                JSObject deviceInfo = new JSObject();
                deviceInfo.put("name", device.getName());
                deviceInfo.put("address", device.getAddress());
                devices.put(deviceInfo);
            }
            JSObject result = new JSObject();
            result.put("devices", devices);
            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error during device scan", e);
            call.reject("Error scanning devices: " + e.getMessage());
        }
    }

    @PluginMethod
    public void startServer(PluginCall call) {
        // Check if Bluetooth permissions are granted
        if (!hasPermissions()) {
            requestBluetoothPermissions(call);
            return;
        }
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            call.reject("Bluetooth must be enabled to start the server.");
            return;
        }
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // SPP UUID
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothChatServer", uuid);
            executorService.execute(() -> {
                try {
                    Log.d(TAG, "Server is waiting for a connection...");
                    serverSocketConnection = serverSocket.accept(); // Blocks until a client connects

                    if (serverSocketConnection != null) {
                        setupStreams(serverSocketConnection);
                        startListeningForData(); // Start listening for incoming data
                        call.resolve();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error accepting connection: " + e.getMessage(), e);
                    call.reject("Failed to accept connection: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected thread error: " + e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server: " + e.getMessage(), e);
            call.reject("Failed to start server: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopServer(PluginCall call) {
        try {
            if (serverSocket != null) {
                serverSocket.close(); // Close the server socket
                serverSocket = null;
                Log.d(TAG, "Server stopped successfully.");
            }
            if (call != null) {
                call.resolve();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to stop server: " + e.getMessage(), e);
            if (call != null) {
                call.reject("Failed to stop server: " + e.getMessage());
            }
        }
    }

    @PluginMethod
    public void connect(PluginCall call) {
        // Check if Bluetooth permissions are granted
        if (!hasPermissions()) {
            requestBluetoothPermissions(call);
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            call.reject("Bluetooth must be enabled to connect to a device.");
            return;
        }

        String address = call.getString("address");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        if (address == null || address.isEmpty()) {
            call.reject("Device address is required to connect. It cannot be null or empty.");
            return;
        }

        // Check if already connected to a device
        if (clientSocketConnection != null && clientSocketConnection.isConnected()) {
            call.reject("Already connected to a device.");
            return;
        }

        try {
            // Log the address being used to connect
            Log.d(TAG, "Connecting to device with address: " + address);

            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            if (device == null) {
                Log.e(TAG, "Bluetooth Device not found for address: " + address);
                call.reject("Bluetooth device not found for address: " + address);
                return;
            }
            
            // Close any existing socket before creating a new connection
            if (clientSocketConnection != null && clientSocketConnection.isConnected()) {
                clientSocketConnection.close();
                clientSocketConnection = null;
            }
            
            // Proceed with the connection
            clientSocketConnection = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery(); // Cancel discovery as it slows down the connection
            clientSocketConnection.connect();
            setupStreams(clientSocketConnection);
            startListeningForData();
            call.resolve();
        } catch (IllegalArgumentException e) {
            // Handle invalid UUIDs
            Log.e(TAG, "Invalid UUID: " + e.getMessage(), e);
            call.reject("Invalid UUID: " + e.getMessage());
        } catch (IOException e) {
            // Handle connection failure
            Log.e(TAG, "Failed to connect to the device: " + e.getMessage(), e);
            call.reject("Failed to connect to the device: " + e.getMessage());
        } catch (Exception e) {
            // Catch any unexpected errors
            Log.e(TAG, "An unexpected error occurred: " + e.getMessage(), e);
            call.reject("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        try {
            // Check if the socket is null or already closed
            if (clientSocketConnection != null && clientSocketConnection.isConnected()) {
                // Stop listening for data if it's still running
                listening = false;

                // Close the input and output streams
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }

                // Close the Bluetooth socket
                clientSocketConnection.close();
                clientSocketConnection = null;
                Log.d(TAG, "Bluetooth connection disconnected successfully.");
            }
            if (call != null) {
                call.resolve(); // Return success to the caller
            }
        } catch (IOException e) {
            Log.e(TAG, "Error disconnecting", e);
             if (call != null) {
                call.reject("Failed to disconnect: " + e.getMessage());
            }
        }
    }

    @PluginMethod
    public void sendData(PluginCall call) {
        // Check if Bluetooth permissions are granted
        if (!hasPermissions()) {
            requestBluetoothPermissions(call);
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            call.reject("Bluetooth must be enabled to send data.");
            return;
        }

        String data = call.getString("data");

        if (data == null) {
            call.reject("Data is required to send.");
            return;
        }

        try {
            outputStream.write(data.getBytes());
            call.resolve();
        } catch (IOException e) {
            Log.e(TAG, "Error sending data", e);
            call.reject("Failed to send data: " + e.getMessage());
        }
    }

    private void startListeningForData() {
        listening = true;
        executorService.execute(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (listening) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String data = new String(buffer, 0, bytes, StandardCharsets.UTF_8);
                        JSObject result = new JSObject();
                        result.put("data", data);
                        notifyListeners("dataReceived", result);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading data", e);
                    listening = false;
                }
            }
        });
    }

    @PermissionCallback
    private void onPermissionResult(PluginCall call) {
        if (hasPermissions()) {
            // Permissions are granted, proceed with the Bluetooth operation
            call.resolve();
        } else {
            // Permissions are denied
            call.reject("Bluetooth permissions are required.");
        }
    }

    private void requestBluetoothPermissions(PluginCall call) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissionForAliases(new String[]{"bluetoothScan"}, call, "onPermissionResult");
        } else {
            requestPermissionForAlias("bluetooth", call, "onPermissionResult");
        }
    }

    private boolean hasPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                   getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } else {
            return getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
    }

    private void handleBluetoothEnableResult(@NonNull ActivityResult result) {
        PluginCall savedCall = getSavedCall();
        if (savedCall == null) return;

        // clearCall(); // Ensure the saved call is cleared to prevent leaks.
        if (result.getResultCode() == Activity.RESULT_OK) {
            Log.d(TAG, "Bluetooth enabled successfully.");
            savedCall.resolve();
        } else {
            Log.e(TAG, "Bluetooth enabling was denied by the user.");
            savedCall.reject("Bluetooth is required for communication. Please enable Bluetooth.");
        }
    }

}
