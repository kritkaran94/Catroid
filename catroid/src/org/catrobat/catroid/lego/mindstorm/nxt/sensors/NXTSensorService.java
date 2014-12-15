/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2014 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.lego.mindstorm.nxt.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import org.catrobat.catroid.common.CatrobatService;
import org.catrobat.catroid.common.ServiceProvider;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.lego.mindstorm.MindstormConnection;
import org.catrobat.catroid.ui.SettingsActivity;
import org.catrobat.catroid.utils.Stopwatch;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NXTSensorService implements CatrobatService, SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = NXTSensorService.class.getSimpleName();

    private SensorRegistry sensorRegistry;
	private NXTSensorFactory sensorFactory;

	private SharedPreferences preferences;

	private PausableScheduledThreadPoolExecutor sensorScheduler;

	private static final int SENSOR_UPDATER_THREAD_COUNT = 2;

	public NXTSensorService(Context context, MindstormConnection connection) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.registerOnSharedPreferenceChangeListener(this);

		sensorRegistry = new SensorRegistry();
		sensorFactory = new NXTSensorFactory(connection);

        sensorScheduler = new PausableScheduledThreadPoolExecutor(SENSOR_UPDATER_THREAD_COUNT);
	}

	public void pauseSensorUpdate() {
		sensorScheduler.pause();
	}

	public void resumeSensorUpdate() {
		sensorScheduler.resume();
	}

    public void destroy() {
        sensorScheduler.shutdown();
		preferences.unregisterOnSharedPreferenceChangeListener(this);
	}

	public NXTSensor createSensor1() {
		Integer sensorType = ServiceProvider.getService(CatrobatService.SENSOR_SERVICE).getMappedSensor(Sensors.NXT_SENSOR_1);// preferences.getString(SettingsActivity.NXT_SENSOR_1, "");
        return  createSensor(sensorType, 0);
	}

	public NXTSensor createSensor2() {
		Integer sensorType = ServiceProvider.getService(CatrobatService.SENSOR_SERVICE).getMappedSensor(Sensors.NXT_SENSOR_2); // preferences.getString(SettingsActivity.NXT_SENSOR_2, "");
        return  createSensor(sensorType, 1);
	}

	public NXTSensor createSensor3() {
		Integer sensorType = ServiceProvider.getService(CatrobatService.SENSOR_SERVICE).getMappedSensor(Sensors.NXT_SENSOR_3); // preferences.getString(SettingsActivity.NXT_SENSOR_3, "");
        return  createSensor(sensorType, 2);
	}

	public NXTSensor createSensor4() {
		Integer sensorType = ServiceProvider.getService(CatrobatService.SENSOR_SERVICE).getMappedSensor(Sensors.NXT_SENSOR_4); // preferences.getString(SettingsActivity.NXT_SENSOR_4, "");
        return  createSensor(sensorType, 3);
	}

	private NXTSensor createSensor(Integer sensorType, int port) {

		if (sensorFactory.isSensorAssigned(sensorType) == false) {
			sensorRegistry.remove(port);
			return null;
		}

		NXTSensor sensor = sensorFactory.create(sensorType, port);
		sensorRegistry.add(sensor);

        return sensor;
	}



	List<OnSensorChangedListener> sensorChangedListeners = new LinkedList<OnSensorChangedListener>();

	public void registerOnSensorChangedListener(OnSensorChangedListener listener) {
		sensorChangedListeners.add(listener);
	}

	private boolean isChangedPreferenceASensorPreference(String preference) {
		return (preference.equals(SettingsActivity.NXT_SENSOR_1) ||
				preference.equals(SettingsActivity.NXT_SENSOR_2) ||
				preference.equals(SettingsActivity.NXT_SENSOR_3) ||
				preference.equals(SettingsActivity.NXT_SENSOR_4));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preference) {

		if (!isChangedPreferenceASensorPreference(preference)) {
			return;
		}

		for (OnSensorChangedListener listener : sensorChangedListeners) {
			if (listener != null) {
				listener.onSensorChanged();
			}
		}
	}

	public interface OnSensorChangedListener {
		public void onSensorChanged();
	}

	private static class SensorValueUpdater implements Runnable {
		private NXTSensor sensor;

		public SensorValueUpdater(NXTSensor sensor) {
			this.sensor = sensor;
		}

		@Override
		public void run() {
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			sensor.updateLastSensorValue();
			Log.d(TAG, String.format("Time for %s sensor: %d ms | Value: %d", sensor.getName(),
					stopwatch.getElapsedMilliseconds(), sensor.getLastSensorValue()));
		}
	}

	private class SensorRegistry {

		private class SensorTuple {

			public ScheduledFuture scheduledFuture;
			public NXTSensor sensor;

			public SensorTuple(ScheduledFuture scheduledFuture, NXTSensor sensor) {
				this.scheduledFuture = scheduledFuture;
				this.sensor = sensor;
			}
		}

		private SparseArray<SensorTuple> registeredSensors = new SparseArray<SensorTuple>();
		private static final int INITIAL_DELAY = 500;

		public synchronized void add(NXTSensor sensor) {
			remove(sensor.getConnectedPort());
			ScheduledFuture scheduledFuture = sensorScheduler.scheduleWithFixedDelay(new SensorValueUpdater(sensor),
					INITIAL_DELAY, sensor.getUpdateInterval(), TimeUnit.MILLISECONDS);

			registeredSensors.put(sensor.getConnectedPort(), new SensorTuple(scheduledFuture, sensor));
		}

		public synchronized void remove(NXTSensor sensor) {
			int port = sensor.getConnectedPort();
			remove(port);
		}

		public synchronized void remove(int port) {
			SensorTuple tuple = registeredSensors.get(port);
			if (tuple != null) {
				tuple.scheduledFuture.cancel(false);

			}
			registeredSensors.remove(port);
		}
	}
}
