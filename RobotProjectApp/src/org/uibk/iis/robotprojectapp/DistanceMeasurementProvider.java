package org.uibk.iis.robotprojectapp;

public class DistanceMeasurementProvider {

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

	private static final short LEFT_SENSOR = 2;
	private static final short RIGHT_SENSOR = 3;
	private static final short CENTER_SENSOR = 6;

	private ChangeEventListener changeEventListener;
	
	private short leftSensorValue;
	private short lastLeftSensorValue;
	private short rightSensorValue;
	private short lastRightSensorValue;
	private short centerSensorValue;
	private short lastCenterSensorValue;
	private short minDiffForEvent;
	private long deltaTime;
	private long lastDispatchedAt;
	private boolean belowThreshold; 
	
	public DistanceMeasurementProvider(short minDiffForEvent, long deltaTime) {
		this.minDiffForEvent = minDiffForEvent;
		this.deltaTime = deltaTime;
		belowThreshold = false;
		lastDispatchedAt = 0;
	}
	
	private void sensorStringParser(String sensorString) throws IllegalArgumentException, NumberFormatException {
		String delims = "[ ]+";
		String[] tokens = sensorString.split(delims);
		if (tokens.length != 9)
			throw new IllegalArgumentException("Error while parsing string, the number of sensors is not as expected: " + tokens.length);
		if (tokens[0].compareTo("sensor:") != 0)
			throw new IllegalArgumentException("Error while parsing string, the 'magic' bytes at the beginning do not match 'sensor:' : "
					+ tokens[0]);
		leftSensorValue = Short.decode(tokens[LEFT_SENSOR+1]);
		rightSensorValue = Short.decode(tokens[RIGHT_SENSOR+1]);
		centerSensorValue = Short.decode(tokens[CENTER_SENSOR+1]);
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
}
