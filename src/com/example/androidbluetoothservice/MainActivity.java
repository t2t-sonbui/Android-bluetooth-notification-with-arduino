package com.example.androidbluetoothservice;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import co.vismart.app.bluetoothpk.BluetoothStatusCallback;

public class MainActivity extends AppCompatActivity implements
		BluetoothStatusCallback {
	Toolbar toolbar;

	@Override
	public void onBackPressed() {

		new AlertDialog.Builder(this)

				.setMessage(
						getResources().getString(R.string.exit_dialog_message))
				.setPositiveButton(
						getResources().getString(R.string.exit_dialog_yes),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								finish();
							}

						})
				.setNegativeButton(
						getResources().getString(R.string.exit_dialog_no), null)
				.show();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.activity_main);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (toolbar != null) {

			toolbar.setSubtitle("Select a device to connect");

		}
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new ManagerServiceFragment()).commit();
			// .add(R.id.container, new MainFragment()).commit();
		}
	}

	@Override
	public void UppdateStatus(CharSequence str) {
		if (toolbar != null) {

			toolbar.setSubtitle(str);

		}

	}

	@Override
	public void UppdateStatus(int resId) {
		if (toolbar != null) {

			toolbar.setSubtitle(resId);

		}

	}

}
