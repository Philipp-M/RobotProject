package org.uibk.iis.robotprojectapp;

public class ObstacleDetector implements DistanceMeasurementProvider.ChangeEventListener {

	public interface ChangeEventListener {
		void onLeftObjectDetected(short minDistance, double angle);

		void onRightObjectDetected(short minDistance, double angle);

		void onObjectDetected(short minDistance, double angleL, double angleR);
	}

	private short minThreshold;
	private short maxThreshold;
	private ChangeEventListener changeEventListener;

	public ObstacleDetector(short minThreshold, short MaxThreshold, ChangeEventListener changeEventListener) {
		DistanceMeasurementProvider.getInstance().registerListener(this, (short) 1, minThreshold);
		this.changeEventListener = changeEventListener;
	}

	public ObstacleDetector(ChangeEventListener changeEventListener) {
		this((short) 20, (short) 70, changeEventListener);
	}

	private double calculateLeftAngle(short left, short center) {
		double x = DistanceMeasurementProvider.SENSOR_DISTANCE + Math.sin(DistanceMeasurementProvider.SENSOR_ANGLE) * left;
		double y = Math.cos(DistanceMeasurementProvider.SENSOR_ANGLE) * left - center;
		return Math.acos(y / (Math.sqrt(y * y + x * x))) - Math.PI / 2;
	}

	private double calculateRightAngle(short right, short center) {
		return -calculateLeftAngle(right, center);
	}

	public short getMinThreshold() {
		return minThreshold;
	}

	public void setMinThreshold(short minThreshold) {
		this.minThreshold = minThreshold;
	}

	public short getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(short maxThreshold) {
		this.maxThreshold = maxThreshold;
	}

	@Override
	public void onDistanceChanged(short left, short center, short right) {
		short minDistance = (short) Math.min(Math.min(left, center), right);
		if (left > minThreshold && right > minThreshold && center > minThreshold && left < maxThreshold && right < maxThreshold
				&& center < maxThreshold)
			changeEventListener.onObjectDetected(minDistance, calculateLeftAngle(left, center), calculateRightAngle(right, center));
		else if (left > minThreshold && center > minThreshold && left < maxThreshold && center < maxThreshold)
			changeEventListener.onLeftObjectDetected(minDistance, calculateLeftAngle(left, center));
		else if (right > minThreshold && center > minThreshold && right < maxThreshold && center < maxThreshold)
			changeEventListener.onRightObjectDetected(minDistance, calculateLeftAngle(left, center));
	}

	@Override
	public void onDistanceBelowThreshold(short left, short center, short right) {
	}

}
