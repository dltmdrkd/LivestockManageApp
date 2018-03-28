package com.cosoros.www.network.bluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cosoros.www.network.R;

/**
 * Created by dltmd on 3/15/2018.
 */

public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothFragment";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_SECURE = 2;
    private static final int REQUEST_CONNECT_INSECURE = 3;

    // UI controls.
    private Button   _sendButton;
    private EditText _sendEditText;
    private ListView _receivedView;

    private BluetoothAdapter _bluetoothAdapter = null;

    private ArrayAdapter<String> _conversationArrayAdapter;
    private String _connectedDeviceName = null;
    private StringBuffer _outStringBuffer;
    private String _receiveBuffer = "";
    private PrintThread _printThread;

    private boolean _initialized = false;

    public static BluetoothFragment newInstance() {
        BluetoothFragment fragment = new BluetoothFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (_bluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();
            activity.finish();
        }
        _printThread = new PrintThread();
        _printThread.setDaemon(true);
        _printThread.setHandler(_printHandler);
        _printThread.start();
    }

    @Override
    public void onStart() {
        super.onStart();

        // if bluetooth is disabled, request that it be enabled.
        if (_bluetoothAdapter.isEnabled() != true) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, REQUEST_ENABLE_BT);
        }
        else if (_initialized != true) {
            setupService();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothService.getInstance().removeHandler(_handler);
        _printThread.quit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BluetoothService.getInstance().getState() == BluetoothService.STATE_NONE) {
            BluetoothService.getInstance().start();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        _receivedView = (ListView) view.findViewById(R.id.in);
        _sendEditText = (EditText) view.findViewById(R.id.edit_text_out);
        _sendEditText.setTextColor(Color.BLACK);
        _sendButton = (Button) view.findViewById(R.id.button_send);
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = _bluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        BluetoothService.getInstance().connect(device, secure);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupService();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    private final Handler _printHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String data = (String)msg.obj;
            _conversationArrayAdapter.add(data);
        }
    };

    private final Handler _handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, _connectedDeviceName));
                            _conversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    _conversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    _receiveBuffer = _receiveBuffer + readMessage;
                    //_conversationArrayAdapter.add(_connectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    _connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to " + _connectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private class PrintThread extends Thread {
        private boolean _quitRequested = false;
        private Handler _handler;

        public void setHandler(Handler handler) {
            _handler = handler;
        }
        public void run() {
            String data = "";
            int indexOfEnd = -1;
            while (_quitRequested != true) {
                data = "";
                synchronized (_receiveBuffer) {
                    if (_receiveBuffer.isEmpty() != true && _receiveBuffer.startsWith("[") != true) {
                        _receiveBuffer = _receiveBuffer.substring(_receiveBuffer.indexOf("["));
                    }
                    indexOfEnd = _receiveBuffer.indexOf("]");
                    if (_receiveBuffer.startsWith("[") && indexOfEnd != -1) {
                        data = _receiveBuffer.substring(0, indexOfEnd + 1);
                        if (_receiveBuffer.endsWith("]")) {
                            _receiveBuffer = "";
                        } else {
                            _receiveBuffer = _receiveBuffer.substring(indexOfEnd + 1);
                        }
                    }
                }
                if (data.isEmpty() != true) {
                    Message msg = new Message();
                    msg.obj = data;
                    _handler.sendMessage(msg);
                    //_conversationArrayAdapter.add(_connectedDeviceName + ":  " + data);
                }
            }
        }

        public void quit() {
            _quitRequested = true;
        }
    }

    private void setupService() {

        // Initialize the array adapter for the conversation thread
        _conversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        _receivedView.setAdapter(_conversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        _sendEditText.setOnEditorActionListener(_writeListener);

        // Initialize the send button with a listener that for click events
        _sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        BluetoothService.getInstance().addHandler(_handler);
        _outStringBuffer = new StringBuffer("");
        _initialized = true;
    }

    public void connectBluetooth() {
        Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
        startActivityForResult(serverIntent, BluetoothFragment.REQUEST_CONNECT_SECURE);
    }
    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (_bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (BluetoothService.getInstance().getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            BluetoothService.getInstance().write(send);

            // Reset out string buffer to zero and clear the edit text field
            _outStringBuffer.setLength(0);
            _sendEditText.setText(_outStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener _writeListener
        = new TextView.OnEditorActionListener() {
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        // If the action is a key-up event on the return key, send the message
        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
            String message = view.getText().toString();
            sendMessage(message);
        }
        return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }
}
