package org.uibk.iis.robotprojectapp;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class DistanceMeasurementProvider {

	private static final short LEFT_SENSOR = 2;
	private static final short RIGHT_SENSOR = 3;
	private static final short CENTER_SENSOR = 6;
	/**
	 * a switch for the custom version, that can directly read the binary value
	 */
	private static final boolean BINARY_READING = false;

	public static interface ChangeEventListener {
		/**
		 * Callback method to be invoked when the distance changes.
		 * 
		 * @param left
		 *            the left IR sensor measurement
		 * @param middle
		 *            the center IR sensor measurement
		 * @param right
		 *            the right IR sensor measurement
		 */
		void onDistanceChanged(short left, short center, short right);

		void onDistanceUnderThreshold(short left, short center, short right);
	}

	private ChangeEventListener changeEventListener;
	private Timer measurementTimer;
	private short leftSensorValue;
	private short lastLeftSensorValue;
	private short rightSensorValue;
	private short lastRightSensorValue;
	private short centerSensorValue;
	private short lastCenterSensorValue;
	private short minDiffForEvent;
	private long deltaTime;
	private short threshold;
	private boolean belowThreshold;
	
	
	public DistanceMeasurementProvider(short minDiffForEvent, long deltaTime, short threshold) {
		this.minDiffForEvent = minDiffForEvent;
		this.deltaTime = deltaTime;
		this.threshold = threshold;
		belowThreshold = false;
	}
	public void start() {
		measurementTimer = new Timer();
		measurementTimer.scheduleAtFixedRate(new MeasurementTimerTask(), 0, deltaTime);
	}
	public void stop() {
		measurementTimer.cancel();
	}
	private void sensorStringParser(String sensorString) throws IllegalArgumentException, NumberFormatException {
		String delims = "[ ]+";
		String[] tokens = sensorString.split(delims);
		if (tokens.length != 9)
			throw new IllegalArgumentException("Error while parsing string, the number of sensors is not as expected: " + tokens.length);
		if (tokens[0].compareTo("sensor:") != 0)
			throw new IllegalArgumentException("Error while parsing string, the 'magic' bytes at the beginning do not match 'sensor:' : "
					+ tokens[0]);
		leftSensorValue = Short.decode(tokens[LEFT_SENSOR + 1]);
		rightSensorValue = Short.decode(tokens[RIGHT_SENSOR + 1]);
		centerSensorValue = Short.decode(tokens[CENTER_SENSOR + 1]);
	}

	public short getLeftSensorValue() {
		return leftSensorValue;
	}

	public short getRightSensorValue() {
		return rightSensorValue;
	}

	public short getCenterSensorValue() {
		return centerSensorValue;
	}

	private class MeasurementTimerTask extends TimerTask {
		@Override
		public void run() {
			if(!BINARY_READING) {
				sensorStringParser(ComDriver.getInstance().comReadWrite(new byte[] { 'q', '\r', '\n' }));
			} else {
				ArrayList<Byte> sensorData = ComDriver.getInstance().comReadBinWrite(new byte[] { 'p', '\r', '\b' });
				leftSensorValue = sensorData.get(LEFT_SENSOR);
				rightSensorValue = sensorData.get(RIGHT_SENSOR);
				centerSensorValue = sensorData.get(CENTER_SENSOR);
			}
			if(Math.abs(lastLeftSensorValue -leftSensorValue) >= minDiffForEvent || Math.abs(lastRightSensorValue -rightSensorValue) >= minDiffForEvent || Math.abs(lastCenterSensorValue -centerSensorValue) >= minDiffForEvent ) {
				lastLeftSensorValue = leftSensorValue;
				lastRightSensorValue = rightSensorValue;
				lastCenterSensorValue = centerSensorValue;
				if(changeEventListener != null) {
					changeEventListener.onDistanceChanged(leftSensorValue, centerSensorValue, rightSensorValue);
				}
			}
			if(changeEventListener != null && !belowThreshold && (leftSensorValue <= threshold || rightSensorValue <= threshold || centerSensorValue <= threshold)) {
				belowThreshold = true;
				changeEventListener.onDistanceUnderThreshold(leftSensorValue, centerSensorValue, rightSensorValue);
			}
			if((leftSensorValue > threshold && rightSensorValue > threshold && centerSensorValue <= threshold))
				belowThreshold = false;
		}
	}
}
