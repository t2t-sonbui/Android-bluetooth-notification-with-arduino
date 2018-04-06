package com.example.androidbluetoothservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class BackgroundService extends Service {
	private static NotificationManager notificationManager;
	private static int NOTIFICATION_ID = 510191752;// Datetime
	private Builder nfc = null;

	private final String TAG = BackgroundService.class.getSimpleName();
	private final static boolean DEBUG = true;
	private final static boolean DEBUG_TOAST = true;
	private BluetoothAdapter mBluetoothAdapter = null;
	private StringBuilder m_oneLineBuf = new StringBuilder();
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE = UUID
	// .fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
			.fromString("00001101-0000-1000-8000-00805F9B34FB");// UUID for
																// Bluetooth
																// SPP: HC 05,
																// Hc06...
	private static final UUID MY_UUID_INSECURE = UUID
	// .fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Member fields
	private BluetoothAdapter mAdapter = null;

	private AcceptThread mSecureAcceptThread;
	private AcceptThread mInsecureAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	private int numMessages = 0;;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device

	// Khoi tao broadcast nhan gia tri can truyen qua bluetooth
	BroadcastReceiver dataSendBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Constants.DATA_SEND_ACTION_INTENT.equals(action)) {
				if (intent.hasExtra(Constants.DATA_SEND_ACTION_EXTRA)) {
					final String data = intent.getExtras().getString(
							Constants.DATA_SEND_ACTION_EXTRA);
					if (DEBUG)
						Log.d(TAG, data);
					byte[] send = data.getBytes();
					write(send);
				}
			}
		}
	};

	private static void sendBroadcastData(Bundle data, Context c) {
		Intent i = new Intent();
		i.setAction(Constants.DATA_RECEIVED_ACTION_INTENT);
		i.putExtras(data);
		c.sendBroadcast(i);
	}

	private void ParseReceiveBuffer(byte[] byteBuffer, int size) {
		String data = new String(byteBuffer, 0, size);
		if (DEBUG)
			Log.d(TAG, "data:" + data);
		// string data = Encoding.ASCII.GetString(byteBuffer, 0, size);//C#
		int lineEndIndex = 0;

		// Check whether data from client has more than one line of
		// information, where each line of information ends with "CRLF"
		// ("\r\n"). If so break data into different lines and process
		// separately.
		do {
			lineEndIndex = data.indexOf("\r\n");
			if (DEBUG)
				Log.d(TAG, "lineEndIndex:" + lineEndIndex);
			if (lineEndIndex != -1) {
				m_oneLineBuf = m_oneLineBuf.append(data, 0, lineEndIndex + 2);
				ProcessData(m_oneLineBuf.toString());
				m_oneLineBuf.delete(0, m_oneLineBuf.length());
				int endIndex = lineEndIndex + 2 + data.length() - lineEndIndex
						- 2;
				// data = data.substring(lineEndIndex + 2, data.length()
				// - lineEndIndex - 2);//C#
				data = data.substring(lineEndIndex + 2, data.length());
			} else {
				// Just append to the existing buffer.
				m_oneLineBuf = m_oneLineBuf.append(data);
			}
		} while (lineEndIndex != -1);
	}

	private void ProcessData(String oneLine) {
		if (DEBUG)
			Log.d(TAG, "oneLine:" + oneLine);
		if (oneLine.toLowerCase().contains(Constants.PRESS.toLowerCase())) {
			UpdateNotification();
		}

	}

	protected void StartLogging() {

		if (Session.isStarted()) {
			Log.d(TAG, "Session already started, ignoring");
			return;
		}

		try {
			startForeground(NOTIFICATION_ID, new Notification());
		} catch (Exception ex) {
			Log.d(TAG, "Could not start GPSLoggingService in foreground. ", ex);
		}

		Session.setStarted(true);

		ShowNotification();
	}

	private void ShowNotification() {

		Intent stopLoggingIntent = new Intent(this, BackgroundService.class);
		stopLoggingIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
		// stopLoggingIntent
		// .putExtra(Constants.ACTION.STOPFOREGROUND_ACTION, true);
		PendingIntent piStop = PendingIntent.getService(this, 0,
				stopLoggingIntent, 0);

		Intent annotateIntent = new Intent(this, MainActivity.class);
		annotateIntent.setAction("com.mendhak.gpslogger.NOTIFICATION_BUTTON");
		PendingIntent piAnnotate = PendingIntent.getActivity(this, 0,
				annotateIntent, 0);

		// What happens when the notification item is clicked
		Intent contentIntent = new Intent(this, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addNextIntent(contentIntent);

		PendingIntent pending = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		String contentText = "App still running ";
		long notificationTime = System.currentTimeMillis();

		if (nfc == null) {
			nfc = new NotificationCompat.Builder(getApplicationContext())
					.setSmallIcon(R.drawable.ic_launcher)
					.setLargeIcon(
							BitmapFactory.decodeResource(getResources(),
									R.drawable.ic_launcher))
					.setPriority(Notification.PRIORITY_MAX)
					.setContentTitle(contentText).setOngoing(true)
					.setContentIntent(pending);

			nfc.addAction(R.drawable.ic_power_white_24dp, "Chú thích",
					piAnnotate).addAction(
					android.R.drawable.ic_menu_close_clear_cancel, "Stop",
					piStop);

		}

		// nfc.setContentTitle(contentText);
		// nfc.setContentText(getString(R.string.app_name));
		// nfc.setWhen(notificationTime);
		// nfc.setNumber(1);

		NotificationCompat.BigTextStyle bigtextStyle = new NotificationCompat.BigTextStyle();
		bigtextStyle.setBigContentTitle(getString(R.string.app_name));
		bigtextStyle.bigText(contentText);
		nfc.setStyle(bigtextStyle);
		// nfc.setWhen(notificationTime);

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, nfc.build());
	}

	private void UpdateNotification() {
		if (nfc != null) {
			++numMessages;
			
			Calendar c = Calendar.getInstance(TimeZone.getDefault());		
			SimpleDateFormat dd = new SimpleDateFormat("EEE", Locale.ENGLISH);
			SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
			SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
			String formattedDay = dd.format(c.getTime());
			String formattedDate = df.format(c.getTime());
			String formattedTime = tf.format(c.getTime());
			String title = " Notification made";
			String titles = " Notifications made";
			String time_press = " time\n on ";
			String times_press = " times\n on ";
			String title_set = (numMessages > 1) ? titles : title;
			String time_press_set = (numMessages > 1) ? times_press
					: time_press;
			String text_notify = "You have pressed the switch "
					+ Integer.toString(numMessages) + time_press_set
					+ formattedDay + ", " + formattedDate + " at "
					+ formattedTime;
			String title_notify = Integer.toString(numMessages) + title_set;
			Log.d(TAG, title_notify);
			long notificationTime = System.currentTimeMillis();

			NotificationCompat.BigTextStyle bigtextStyle = new NotificationCompat.BigTextStyle();
			bigtextStyle.setBigContentTitle(title_notify);
			bigtextStyle.bigText(text_notify);
			// bigtextStyle.setSummaryText("bigtextStyle");

			// NotificationCompat.InboxStyle inboxStyle = new
			// NotificationCompat.InboxStyle();
			// inboxStyle.setBigContentTitle(title);
			// inboxStyle.addLine("You have pressed the switch 3 times");
			// inboxStyle.addLine("on" + formattedDate + " at " +
			// formattedTime);
			// inboxStyle.addLine("");
			// inboxStyle.setSummaryText("inboxStyle");

			nfc.setStyle(bigtextStyle);
			nfc.setWhen(notificationTime);
			nfc.setNumber(numMessages);
			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.notify(NOTIFICATION_ID, nfc.build());
		}

	}

	public void StopLogging() {
		Session.setStarted(false);
		stopForeground(true);
		RemoveNotification();
		stopSelf();

	}

	/**
	 * Hides the notification icon in the status bar if it's visible.
	 */
	private void RemoveNotification() {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.DATA_SEND_ACTION_INTENT);
		registerReceiver(dataSendBroadcast, filter);
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
			if (DEBUG)
				Log.i(TAG, "Received Start Foreground Intent ");
			StartLogging();

			if (!Session.isConnected()) {
				// To do
				Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT)
						.show();
				if (DEBUG)
					Log.d(TAG, "onStartCommand() " + intent + " " + flags + " "
							+ startId);
				mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

				if (mBluetoothAdapter != null) {
					if (intent
							.hasExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)) {
						String address = intent.getExtras().getString(
								DeviceListActivity.EXTRA_DEVICE_ADDRESS);
						boolean secure = intent.getExtras().getBoolean(
								Constants.EXTRA_DEVICE_SECUR);
						if (DEBUG)
							Log.d(TAG, "Address: " + address);
						// Get the BluetoothDevice object
						BluetoothDevice device = mBluetoothAdapter
								.getRemoteDevice(address);
						// Attempt to connect to the device
						connect(device, secure);
					} else {
						stopSelf();
					}
				} else {
					if (DEBUG_TOAST)
						Toast.makeText(this,
								"Bluetooth is not available, Stop server",
								Toast.LENGTH_SHORT).show();
					stop();// stop thread
					stopSelf();
					return 0;
				}
				if (mState == STATE_NONE) {
					start();
				}
			}

		} else if (intent.getAction().equals(
				Constants.ACTION.STOPFOREGROUND_ACTION)) {
			if (DEBUG)
				Log.i(TAG, "Received Stop Foreground Intent");
			StopLogging();
		}

		return Service.START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(dataSendBroadcast);
		stop();
		numMessages = 0;
		if (DEBUG_TOAST)
			Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT)
					.show();
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		Bundle mBundle = new Bundle();

		mBundle.putInt(Constants.DATA_COMUNICATION_WHAT_EXTRA,
				Constants.MESSAGE_STATE_CHANGE);
		mBundle.putInt(Constants.KEY_STATE_EXTRA, state);
		sendBroadcastData(mBundle, getApplicationContext());

	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume()
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_LISTEN);

		// Start the thread to listen on a BluetoothServerSocket
		if (mSecureAcceptThread == null) {
			mSecureAcceptThread = new AcceptThread(true);
			mSecureAcceptThread.start();
		}
		if (mInsecureAcceptThread == null) {
			mInsecureAcceptThread = new AcceptThread(false);
			mInsecureAcceptThread.start();
		}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 * @param secure
	 *            Socket Security type - Secure (true) , Insecure (false)
	 */
	public synchronized void connect(BluetoothDevice device, boolean secure) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device, secure);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device, final String socketType) {
		if (D)
			Log.d(TAG, "connected, Socket Type:" + socketType);

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}
		if (mInsecureAcceptThread != null) {
			mInsecureAcceptThread.cancel();
			mInsecureAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket, socketType);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Bundle mBundle = new Bundle();
		mBundle.putInt(Constants.DATA_COMUNICATION_WHAT_EXTRA,
				Constants.MESSAGE_DEVICE_NAME);
		mBundle.putString(Constants.DEVICE_NAME, device.getName());
		sendBroadcastData(mBundle, getApplicationContext());
		if (DEBUG)
			Log.d(TAG, "Connected to " + device.getName());

		setState(STATE_CONNECTED);
		Session.setConnected(true);
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}

		if (mInsecureAcceptThread != null) {
			mInsecureAcceptThread.cancel();
			mInsecureAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		// Send a failure message back to the Activity

		Bundle mBundle = new Bundle();
		mBundle.putInt(Constants.DATA_COMUNICATION_WHAT_EXTRA,
				Constants.MESSAGE_TOAST);
		mBundle.putString(Constants.TOAST, "Unable to connect device");
		sendBroadcastData(mBundle, getApplicationContext());
		Session.setConnected(false);

		if (DEBUG)
			Log.d(TAG, "Unable to connect device");
		// Start the service over to restart listening mode
		BackgroundService.this.start();
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		// Send a failure message back to the Activity

		Bundle mBundle = new Bundle();
		mBundle.putInt(Constants.DATA_COMUNICATION_WHAT_EXTRA,
				Constants.MESSAGE_TOAST);
		mBundle.putString(Constants.TOAST, "Device connection was lost");
		sendBroadcastData(mBundle, getApplicationContext());
		Session.setConnected(false);
		if (DEBUG)
			Log.d(TAG, "Device connection was lost");

		// Start the service over to restart listening mode
		BackgroundService.this.start();
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		private String mSocketType;

		public AcceptThread(boolean secure) {
			BluetoothServerSocket tmp = null;
			mSocketType = secure ? "Secure" : "Insecure";

			// Create a new listening server socket
			try {
				if (secure) {
					tmp = mAdapter.listenUsingRfcommWithServiceRecord(
							NAME_SECURE, MY_UUID_SECURE);
				} else {
					tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
							NAME_INSECURE, MY_UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D)
				Log.d(TAG, "Socket Type: " + mSocketType
						+ "BEGIN mAcceptThread" + this);
			setName("AcceptThread" + mSocketType);

			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "Socket Type: " + mSocketType
							+ "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BackgroundService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice(),
									mSocketType);
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

		}

		public void cancel() {
			if (D)
				Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket Type" + mSocketType
						+ "close() of server failed", e);
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private String mSocketType;

		public ConnectThread(BluetoothDevice device, boolean secure) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			mSocketType = secure ? "Secure" : "Insecure";

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				if (secure) {
					tmp = device
							.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
				} else {
					tmp = device
							.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() " + mSocketType
							+ " socket during connection failure", e2);
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BackgroundService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice, mSocketType);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect " + mSocketType
						+ " socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "create ConnectedThread: " + socketType);
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);

					// Send the obtained bytes to the UI Activity
					Bundle mBundle = new Bundle();
					mBundle.putInt(Constants.DATA_COMUNICATION_WHAT_EXTRA,
							Constants.MESSAGE_READ);
					mBundle.putInt(Constants.KEY_BYTE_READ_COUNT, bytes);
					mBundle.putByteArray(Constants.KEY_BUFF_READ, buffer);

					ParseReceiveBuffer(buffer, bytes);
					sendBroadcastData(mBundle, getApplicationContext());
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					// Start the service over to restart listening mode
					BackgroundService.this.start();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// Share the sent message back to the UI Activity
				Bundle mBundle = new Bundle();
				mBundle.putInt(Constants.DATA_COMUNICATION_WHAT_EXTRA,
						Constants.MESSAGE_WRITE);

				mBundle.putByteArray(Constants.KEY_BUFF_WRITE, buffer);
				sendBroadcastData(mBundle, getApplicationContext());
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

}
