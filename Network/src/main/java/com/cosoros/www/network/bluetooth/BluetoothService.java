package com.cosoros.www.network.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by dltmd on 2018-03-21.
 */

@SuppressWarnings("serial")
public class BluetoothService {
    private static final String TAG = "BluetoothService";

    private static final String NAME_SECURE = "BluetoothSecure";
    private static final String NAME_INSECURE = "BluetoothInsecure";

    private static final UUID UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private ArrayList<Handler> _handlers;
    private AcceptThread _acceptThreadSecure;
    private AcceptThread _acceptThreadInsecure;
    private ConnectThread _connectThread;
    private ConnectedThread _connectedThread;
    private int _state;
    private int _newState;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private static BluetoothService instance = null;

    public static synchronized BluetoothService getInstance() {
        if (instance == null) {
            instance = new BluetoothService();
        }
        return instance;
    }

    public BluetoothService() {
        _newState = _state = STATE_NONE;
        _handlers = new ArrayList<>();
    }

    public synchronized void addHandler(Handler handler) {
        _handlers.add(handler);
    }

    public synchronized void removeHandler(Handler handler) {
        _handlers.remove(handler);
    }

    private synchronized void updateTitle() {
        _state = getState();
        _newState = _state;
        for (int i = 0; i < _handlers.size(); ++i) {
            _handlers.get(i).obtainMessage(Constants.MESSAGE_STATE_CHANGE, _newState, -1).sendToTarget();
        }
    }

    public synchronized int getState() { return _state; }

    public synchronized void start() {
        if (_connectThread != null) {
            _connectThread.cancel();
            _connectThread = null;
        }

        if (_connectedThread != null) {
            _connectedThread.cancel();
            _connectedThread = null;
        }

        if (_acceptThreadSecure == null) {
            _acceptThreadSecure = new AcceptThread(false);
            _acceptThreadSecure.start();
        }
        if (_acceptThreadInsecure == null) {
            _acceptThreadInsecure = new AcceptThread(false);
            _acceptThreadInsecure.start();
        }
        updateTitle();
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (_state == STATE_CONNECTING) {
            if (_connectThread != null) {
                _connectThread.cancel();
                _connectThread = null;
            }
        }
        if (_connectedThread != null) {
            _connectedThread.cancel();
            _connectedThread = null;
        }

        _connectThread = new ConnectThread(device, secure);
        _connectThread.start();
        updateTitle();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        if (_connectThread != null) {
            _connectThread.cancel();
            _connectThread = null;
        }
        if (_connectedThread != null) {
            _connectedThread.cancel();
            _connectedThread = null;
        }

        if (_acceptThreadSecure != null) {
            _acceptThreadSecure.cancel();
            _acceptThreadSecure = null;
        }

        if (_acceptThreadInsecure != null) {
            _acceptThreadInsecure.cancel();
            _acceptThreadInsecure = null;
        }

        _connectedThread = new ConnectedThread(socket, socketType);
        _connectedThread.start();

        for (int i = 0; i < _handlers.size(); ++i) {
            Message msg = _handlers.get(i).obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            _handlers.get(i).sendMessage(msg);
        }
        updateTitle();
    }

    public synchronized void stop() {
        if (_connectThread != null) {
            _connectThread.cancel();
            _connectThread = null;
        }
        if (_connectedThread != null) {
            _connectedThread.cancel();
            _connectedThread = null;
        }
        if (_acceptThreadSecure != null) {
            _acceptThreadSecure.cancel();
            _acceptThreadSecure = null;
        }
        if (_acceptThreadInsecure != null) {
            _acceptThreadInsecure.cancel();
            _acceptThreadInsecure = null;
        }
        _state = STATE_NONE;

        updateTitle();
    }

    public void write(byte[] out) {
        ConnectedThread temp;
        synchronized (this) {
            if (_state != STATE_CONNECTED) return;

            temp = _connectedThread;
        }
        temp.write(out);
    }

    private synchronized void connectionFailed() {
        // Send a failure message back to the Activity
        for (int i = 0; i < _handlers.size(); ++i) {
            Message msg = _handlers.get(i).obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, "Unable to connect device");
            msg.setData(bundle);
            _handlers.get(i).sendMessage(msg);
        }

        _state = STATE_NONE;
        updateTitle();

        BluetoothService.this.start();
    }

    private synchronized void connectionLost() {
        for (int i = 0; i < _handlers.size(); ++i) {
            Message msg = _handlers.get(i).obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, "Device connection was lost");
            msg.setData(bundle);
            _handlers.get(i).sendMessage(msg);
        }
        _state = STATE_NONE;
        updateTitle();

        BluetoothService.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket _serverSocket;
        private String _socketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            _socketType = secure ? "Secure" : "Insecure";

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, UUID_SECURE);
                } else {
                    tmp = adapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, UUID_INSECURE);
                }
            } catch (IOException e) {
            }
            _serverSocket = tmp;
            _state = STATE_LISTEN;
        }

        public void run() {
            setName("AcceptThread" + _socketType);

            BluetoothSocket socket = null;

            while (_state != STATE_CONNECTED) {
                try {
                    socket = _serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (_state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), _socketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                _serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket _socket;
        private final BluetoothDevice _device;
        private String _socketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            _device = device;
            BluetoothSocket temp = null;
            _socketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    temp = device.createRfcommSocketToServiceRecord(UUID_SECURE);
                } else {
                    temp = device.createInsecureRfcommSocketToServiceRecord(UUID_INSECURE);
                }
            } catch (IOException e) {
            }
            _socket = temp;
            _state = STATE_CONNECTING;
        }

        public void run() {
            setName("ConnectThread" + _socketType);

            // Always cancel discovery because it will slow down a connection
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            adapter.cancelDiscovery();

            try {
                _socket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    _socket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                _connectThread = null;
            }

            // Start the connected thread
            connected(_socket, _device, _socketType);
        }

        public void cancel() {
            try {
                _socket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket _socket;
        private final InputStream _inStream;
        private final OutputStream _outStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            _socket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            _inStream = tempIn;
            _outStream = tempOut;
            _state = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (_state == STATE_CONNECTED) {
                try {
                    sleep(200);
                } catch (InterruptedException e) {

                }
                try {
                    // Read from the InputStream
                    bytes = _inStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    synchronized (_handlers) {
                        for (int i = 0; i < _handlers.size(); ++i) {
                            _handlers.get(i).obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        }
                    }

                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                _outStream.write(buffer);

                // Share the sent message back to the UI Activity
                synchronized (_handlers) {
                    for (int i = 0; i < _handlers.size(); ++i) {
                        _handlers.get(i).obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                    }
                }
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                _socket.close();
            } catch (IOException e) {
            }
        }
    }
}
