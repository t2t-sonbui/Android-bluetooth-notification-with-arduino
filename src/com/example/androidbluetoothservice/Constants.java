package com.example.androidbluetoothservice;

/**
 * Created by Son Bui on 09/08/2015.
 */
public class Constants {

	public static final String DATA_RECEIVED_ACTION_INTENT = "DATA_RECEIVED_ACTION_INTENT";
	public static final String DATA_RECEIVED_ACTION_INTENT_EXTRA = "info.laptrinhpic.bluetooth.DATA_RECEIVED";
	public static final String DATA_COMUNICATION_WHAT_EXTRA = "info.laptrinhpic.bluetooth.DATA_COMUNICATION_WHAT";
	public static final String KEY_STATE_EXTRA = "info.laptrinhpic.bluetooth.KEY_STATE_EXTRA";
	public static final String KEY_DEVICE_NAME_EXTRA = "info.laptrinhpic.bluetooth.KEY_DEVICE_NAME_EXTRA";
	public static final String KEY_BYTE_READ_COUNT = "info.laptrinhpic.bluetooth.KEY_BYTE_READ_COUNT";
	public static final String KEY_BUFF_READ = "info.laptrinhpic.bluetooth.KEY_BUFF_READ";
	public static final String KEY_BUFF_WRITE = "info.laptrinhpic.bluetooth.KEY_BUFF_WRITE";

	public static final String DATA_SEND_ACTION_INTENT = "DATA_SEND_ACTION_INTENT";
	public static final String DATA_SEND_ACTION_EXTRA = "info.laptrinhpic.bluetooth.DATA_SEND";
	public static final int REQUEST_CODE_MANAGER_DEVICE = 113;

	//
	public static String EXTRA_DEVICE_SECUR = "EXTRA_DEVICE_SECUR";

	public interface ACTION {
		public static String MAIN_ACTION = "foregroundservice.action.main";
		public static String PREV_ACTION = "foregroundservice.action.prev";
		public static String PLAY_ACTION = "foregroundservice.action.play";
		public static String NEXT_ACTION = "foregroundservice.action.next";
		public static String STARTFOREGROUND_ACTION = "info.laptrinhpic.bluetooth.action.startforeground";
		public static String STOPFOREGROUND_ACTION = "info.laptrinhpic.bluetooth.action.stopforeground";
	}

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	//
	public static final String PRESS = "PRESS";
}
