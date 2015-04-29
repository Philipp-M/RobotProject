package org.uibk.iis.robotprojectapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DistanceMeasurementProvider {

	private static final short LEFT_SENSOR = 2;
	private static final short RIGHT_SENSOR = 3;
	private static final short CENTER_SENSOR = 6;
	public static final double SENSOR_DISTANCE = 5.8;
	public static final double SENSOR_ANGLE = 0.26625205;

	private boolean binaryReading;

	public static interface ChangeEventListener {
		/**
		 * Callback methods to be invoked when the distance changes.
		 *
		 * @param left   the left IR sensor measurement
		 * @param middle the center IR sensor measurement
		 * @param right  the right IR sensor measurement
		 */
		void onDistanceChanged(short left, short center, short right);

		void onDistanceBelowThreshold(short left, short center, short right);
	}

	/**
	 * just a structure that holds every necessary information that a listener requires...
	 */
	private static class ChangeEventListenerContainer {
		public ChangeEventListener changeEventListener;
		public short minDiffForEvent;
		public long cooldownThreshold;
		public int cooldownThresholdCounter;
		public short threshold;
		public boolean belowThreshold;
		private short lastLeftSensorValue;
		private short lastRightSensorValue;
		private short lastCenterSensorValue;

		public ChangeEventListenerContainer(ChangeEventListener changeEventListener, short minDiffForEvent, short threshold,
		                                    long cooldownThreshold) {
			this.changeEventListener = changeEventListener;
			this.minDiffForEvent = minDiffForEvent;
			this.threshold = threshold;
			this.belowThreshold = false;
			this.cooldownThreshold = cooldownThreshold;
			this.cooldownThresholdCounter = 0;
		}
	}

	private List<ChangeEventListenerContainer> changeEventListeners;
	private Timer measurementTimer;
	private short leftSensorValue;
	private short rightSensorValue;
	private short centerSensorValue;
	private long deltaTime;
	private short threshold;

	private static final class InstanceHolder {
		static final DistanceMeasurementProvider INSTANCE = new DistanceMeasurementProvider();
	}

	private DistanceMeasurementProvider() {
		changeEventListeners = new ArrayList<DistanceMeasurementProvider.ChangeEventListenerContainer>();
	}

	public static DistanceMeasurementProvider getInstance() {
		return InstanceHolder.INSTANCE;
	}

	/**
	 * this method has to be called first otherwise the other (listener) methods won't work
	 *
	 * @param deltaTime time between measurements has to be bigger than 115ms because the robot is to slow to answer(or to be exact it just sends
	 *                  quite much overhead(string instead of binary). A fix which has to be flashed on the robot interface is already available
	 *                  which reduces this delay to 35ms(BINARY_READING))
	 */
	public void init(long deltaTime) {
		this.deltaTime = deltaTime;
		this.binaryReading = false;
	}

	/**
	 * a switch for the custom version, that can directly read the binary value
	 */
	public boolean isBinaryReading() {
		return binaryReading;
	}

	public void setBinaryReading(boolean binaryReading) {
		this.binaryReading = binaryReading;
	}

	/**
	 * starts the measurements and listener events,
	 */
	public void start() {
		measurementTimer = new Timer();
		measurementTimer.scheduleAtFixedRate(new MeasurementTimerTask(), 0, deltaTime);
	}

	/**
	 * stops the measurements and listener events
	 */
	public void stop() {
		measurementTimer.cancel();
	}

	/**
	 * registers a listener
	 *
	 * @param changeEventListener the listener to register
	 * @param threshold           below this value a belowThreshold event is sent, used for example as stopping detection
	 * @param minDiffForEvent     minimum difference('delta'-length) between two measurements to trigger an event to the listener
	 */
	public void registerListener(ChangeEventListener changeEventListener, short minDiffForEvent, short threshold, long cooldownThreshold) {
		changeEventListeners.add(new ChangeEventListenerContainer(changeEventListener, minDiffForEvent, threshold, cooldownThreshold));
	}

	/**
	 * unregisters the given listener
	 *
	 * @param changeEventListener
	 */
	public void unregisterListener(ChangeEventListener changeEventListener) {
		for (ChangeEventListenerContainer changeEventListenerContainer : changeEventListeners) {
			if (changeEventListenerContainer.changeEventListener == changeEventListener)
				changeEventListeners.remove(changeEventListenerContainer);
		}
	}

	/**
	 * needed for the text based reply(who ever came to the idea to send text instead of binary over such a slow connection...)
	 *
	 * @param sensorString the string to parse
	 * @throws IllegalArgumentException thrown if the string is of incorrect syntax
	 * @throws NumberFormatException    thrown if the string is of incorrect syntax
	 */
	private void sensorStringParser(String sensorString) throws IllegalArgumentException, NumberFormatException {
		String delims = "\\s+";
		String[] tokens = sensorString.split(delims);
		int pitch = 3;
		if (tokens.length != 12 && tokens.length != 9)
			throw new IllegalArgumentException("Error while parsing string, the number of sensors is not as expected: " + tokens.length);
		if (tokens.length == 9)
			pitch = 0;

		if (tokens[pitch].compareTo("sensor:") != 0)
			throw new IllegalArgumentException("Error while parsing string, the 'magic' bytes at the beginning do not match 'sensor:' : "
					+ tokens[pitch]);
		leftSensorValue = Short.decode(tokens[LEFT_SENSOR + 1 + pitch]);
		rightSensorValue = Short.decode(tokens[RIGHT_SENSOR + 1 + pitch]);
		centerSensorValue = Short.decode(tokens[CENTER_SENSOR + 1 + pitch]);

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

	/**
	 * The timer task that updates the measurements and sends events to the listeners
	 */
	private class MeasurementTimerTask extends TimerTask {
		@Override
		public void run() {
			if (!binaryReading) {
				try {
					if (ComDriver.getInstance().isConnected()) {
						sensorStringParser(ComDriver.getInstance().comReadWrite(new byte[]{'q', '\r', '\n'}));
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else {
				if (ComDriver.getInstance().isConnected()) {
					ArrayList<Byte> sensorData = ComDriver.getInstance().comReadBinWrite(new byte[]{'p', '\r', '\b'});
					leftSensorValue = sensorData.get(LEFT_SENSOR);
					rightSensorValue = sensorData.get(RIGHT_SENSOR);
					centerSensorValue = sensorData.get(CENTER_SENSOR);
					if (leftSensorValue == -1)
						leftSensorValue = 255;
					if (rightSensorValue == -1)
						rightSensorValue = 255;
					if (centerSensorValue == -1)
						centerSensorValue = 255;
				}
			}
			for (ChangeEventListenerContainer cEListenCont : changeEventListeners) {
				if (Math.abs(cEListenCont.lastLeftSensorValue - leftSensorValue) >= cEListenCont.minDiffForEvent
						|| Math.abs(cEListenCont.lastRightSensorValue - rightSensorValue) >= cEListenCont.minDiffForEvent
						|| Math.abs(cEListenCont.lastCenterSensorValue - centerSensorValue) >= cEListenCont.minDiffForEvent) {
					cEListenCont.lastLeftSensorValue = leftSensorValue;
					cEListenCont.lastRightSensorValue = rightSensorValue;
					cEListenCont.lastCenterSensorValue = centerSensorValue;
					cEListenCont.changeEventListener.onDistanceChanged(leftSensorValue, centerSensorValue, rightSensorValue);
				}
				cEListenCont.cooldownThresholdCounter++;
				if (!cEListenCont.belowThreshold
						&& (leftSensorValue <= cEListenCont.threshold || rightSensorValue <= cEListenCont.threshold || centerSensorValue <= cEListenCont.threshold)) {
					cEListenCont.belowThreshold = true;
					cEListenCont.changeEventListener.onDistanceBelowThreshold(leftSensorValue, centerSensorValue, rightSensorValue);
					cEListenCont.cooldownThresholdCounter = 0;
				}

				if (leftSensorValue > threshold && rightSensorValue > cEListenCont.threshold && centerSensorValue > cEListenCont.threshold
						&& cEListenCont.cooldownThresholdCounter * deltaTime > cEListenCont.cooldownThreshold)
					cEListenCont.belowThreshold = false;
			}
		}
	}
}
