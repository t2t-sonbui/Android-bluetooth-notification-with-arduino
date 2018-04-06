package com.example.androidbluetoothservice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import co.vismart.app.bluetoothpk.BluetoothStatusCallback;

public class ManagerServiceFragment extends Fragment {

	BluetoothStatusCallback mBluetoothStatusCallback;
	View rootView;
	Button btnStart, btnStop;
	Button btnSendOne, btnSendTwo;
	EditText editSendOne, editSendTwo;

	TextView editRecData;
	private String messageStr;

	// Debugging
	private static final String TAG = ManagerServiceFragment.class.getName();
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	// Name of the connected device
	private String mConnectedDeviceName = null;

	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services

	String device_add;
	boolean device_secur;

	// Khoi tao broadcast nhan gia tri truyen qua bluetooth tu service
	BroadcastReceiver dataServiceBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Constants.DATA_RECEIVED_ACTION_INTENT.equals(action)) {
				if (intent.hasExtra(Constants.DATA_COMUNICATION_WHAT_EXTRA)) {
					final Bundle data = intent.getExtras();
					//
					int what = data
							.getInt(Constants.DATA_COMUNICATION_WHAT_EXTRA);
					Log.d(TAG, "What=" + Integer.toString(what));

					switch (what) {
					case MESSAGE_STATE_CHANGE:
						int state = data.getInt(Constants.KEY_STATE_EXTRA);
						if (D)
							Log.i(TAG, "MESSAGE_STATE_CHANGE: " + state);
						switch (state) {
						case BackgroundService.STATE_CONNECTED:
							setStatus(getString(R.string.title_connected_to,
									mConnectedDeviceName));
							// txtValue.setText(mOutStringBuffer);
							break;
						case BackgroundService.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;
						case BackgroundService.STATE_LISTEN:
						case BackgroundService.STATE_NONE:
							setStatus(R.string.title_not_connected);
							break;
						}
						break;
					case MESSAGE_WRITE:
						byte[] writeBuf = (byte[]) data
								.getByteArray(Constants.KEY_BUFF_WRITE);
						// construct a string from the buffer
						String writeMessage = new String(writeBuf);
						// mConversationArrayAdapter.add("Me:  " +
						// writeMessage);
						break;
					case MESSAGE_READ:
						byte[] readBuf = (byte[]) (byte[]) data
								.getByteArray(Constants.KEY_BUFF_READ);
						int byteCount = data
								.getInt(Constants.KEY_BYTE_READ_COUNT);
						// construct a string from the valid bytes in the buffer
						String readMessage = new String(readBuf, 0, byteCount);
						editRecData.setText(readMessage);// Update text recieved
						break;
					case MESSAGE_DEVICE_NAME:
						// save the connected device's name
						mConnectedDeviceName = data.getString(DEVICE_NAME);
						if (null != getActivity()) {
							Toast.makeText(getActivity(),
									"Connected to " + mConnectedDeviceName,
									Toast.LENGTH_SHORT).show();
						}
						setStatus("Connected to " + mConnectedDeviceName);
						break;
					case MESSAGE_TOAST:
						if (null != getActivity()) {
							Toast.makeText(getActivity(),
									data.getString(TOAST), Toast.LENGTH_SHORT)
									.show();
						}
						break;

					}

				}
			}
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mBluetoothStatusCallback = (BluetoothStatusCallback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement BluetoothStatusCallback");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);

		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		inflater.inflate(R.menu.main, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(getActivity(), DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.insecure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(getActivity(), DeviceListActivity.class);
			startActivityForResult(serverIntent,
					REQUEST_CONNECT_DEVICE_INSECURE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(getActivity(), "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			getActivity().finish();
		}
		// Init GUI
		rootView = inflater.inflate(R.layout.fragment_manager_service,
				container, false);
		btnStart = (Button) rootView.findViewById(R.id.button_start);
		btnStop = (Button) rootView.findViewById(R.id.button_stop);
		btnSendOne = (Button) rootView.findViewById(R.id.button_send);
		editSendOne = (EditText) rootView.findViewById(R.id.edit_text_out);
		editRecData = (TextView) rootView.findViewById(R.id.edit_text_received);
		// btnStart.setEnabled(false);
		// btnStop.setEnabled(false);
		btnStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startService(v);

			}
		});
		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				stopService(v);
			}
		});
		btnSendOne.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = editSendOne.getText().toString();
				sendMessage(text);
			}
		});

		// Event UI

		return rootView;
	}

	// Method to start the service
	public void startService(View view) {
		Intent i = new Intent(getActivity().getBaseContext(),
				BackgroundService.class);
		i.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
		// potentially add data to the intent
		i.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, device_add);
		i.putExtra(Constants.EXTRA_DEVICE_SECUR, device_secur);
		getActivity().getBaseContext().startService(i);
	}

	// Method to stop the service
	public void stopService(View view) {
		// Stop service
		// getActivity().stopService(
		// new Intent(getActivity().getBaseContext(),
		// BackgroundService.class));

		// Stop foreground service
		Intent i = new Intent(getActivity().getBaseContext(),
				BackgroundService.class);
		i.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
		// potentially add data to the intent
		i.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, device_add);
		i.putExtra(Constants.EXTRA_DEVICE_SECUR, device_secur);
		getActivity().getBaseContext().startService(i);

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			// Initialize the buffer for outgoing messages
			mOutStringBuffer = new StringBuffer("");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter dfilter = new IntentFilter();
		dfilter.addAction(Constants.DATA_RECEIVED_ACTION_INTENT);
		getActivity().registerReceiver(dataServiceBroadcast, dfilter);

	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(dataServiceBroadcast);

	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {

		// Check that there's actually something to send
		if (message.length() > 0) {
			Intent i = new Intent();
			i.setAction(Constants.DATA_SEND_ACTION_INTENT);
			i.putExtra(Constants.DATA_SEND_ACTION_EXTRA, message);
			getActivity().getApplicationContext().sendBroadcast(i);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);

		}
	}

	private final void setStatus(int resId) {

		try {

			// Call interface to update status bluetooth
			if (mBluetoothStatusCallback != null) {
				mBluetoothStatusCallback.UppdateStatus(resId);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Over Load setStatus
	private final void setStatus(CharSequence subTitle) {
		try {
			// Call interface to update status bluetooth
			if (mBluetoothStatusCallback != null) {
				mBluetoothStatusCallback.UppdateStatus(subTitle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				// setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		device_add = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		device_secur = secure;
		editRecData.setText(device_add);

	}
}
