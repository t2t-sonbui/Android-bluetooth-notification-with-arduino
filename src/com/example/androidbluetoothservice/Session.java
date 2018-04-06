/*
 *    This file is part of GPSLogger for Android.
 *
 *    GPSLogger for Android is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    GPSLogger for Android is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.androidbluetoothservice;

import android.app.Application;
import android.location.Location;

public class Session extends Application {

	// ---------------------------------------------------
	// Session values - updated as the app runs
	// ---------------------------------------------------
	private static boolean isStarted;
	private static boolean isConnected;

	public static boolean isConnected() {
		return isConnected;
	}

	public static void setConnected(boolean isConnected) {
		Session.isConnected = isConnected;
	}

	/**
	 * @return whether logging has started
	 */
	public static boolean isStarted() {
		return isStarted;
	}

	/**
	 * @param isStarted
	 *            set whether logging has started
	 */
	public static void setStarted(boolean isStarted) {
		Session.isStarted = isStarted;
	}

}