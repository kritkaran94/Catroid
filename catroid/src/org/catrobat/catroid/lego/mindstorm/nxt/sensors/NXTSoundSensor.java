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

import org.catrobat.catroid.lego.mindstorm.MindstormConnection;

public class NXTSoundSensor extends NXTSensor {

	private static final String TAG = NXTLightSensor.class.getSimpleName();
	private static final int DEFAULT_VALUE = 0;

	public NXTSoundSensor(int port, MindstormConnection connection) {
		super(port, NXTSensorType.SOUND_DBA, NXTSensorMode.Percent, connection);
		lastValidValue = DEFAULT_VALUE;
	}

	@Override
	public int getValue() {
		return getScaledValue();
	}
}
