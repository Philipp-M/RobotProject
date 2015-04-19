package org.uibk.iis.robotprojectapp;

public class ObstacleDetector implements DistanceMeasurementProvider.ChangeEventListener {

	public interface ChangeEventListener {
		void onLeftObstacleDetected(short minDistance, double angle);

		void onRightObstacleDetected(short minDistance, double angle);

		void onObstacleDetected(short minDistance, double angleL, double angleR);

		void onObstacleDetected(ObstacleDetector.Detection sensor, short minDistance);
		
		void onObstacleDisappeared();
	}

	public enum Detection {
		NONE, LEFT_SENS, RIGHT_SENS, CENTER_SENS, LEFT_ANGLE, RIGHT_ANGLE, BOTH
	}

	private short minThreshold;
	private short maxThreshold;
	private ChangeEventListener changeEventListener;
	private Detection lastDetection;

	public ObstacleDetector(short minThreshold, short MaxThreshold, ChangeEventListener changeEventListener) {
		DistanceMeasurementProvider.getInstance().registerListener(this, (short) 1, minThreshold);
		this.changeEventListener = changeEventListener;
		lastDetection = Detection.NONE;
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
				&& center < maxThreshold && lastDetection != Detection.BOTH) {
			changeEventListener.onObstacleDetected(minDistance, calculateLeftAngle(left, center), calculateRightAngle(right, center));
			lastDetection = Detection.BOTH;
		}
		else if (left > minThreshold && center > minThreshold && left < maxThreshold && center < maxThreshold
				&& lastDetection != Detection.LEFT_ANGLE) {
			changeEventListener.onLeftObstacleDetected(minDistance, calculateLeftAngle(left, center));
			lastDetection = Detection.LEFT_ANGLE;
		}
		else if (right > minThreshold && center > minThreshold && right < maxThreshold && center < maxThreshold
				&& lastDetection != Detection.RIGHT_ANGLE) {
			changeEventListener.onRightObstacleDetected(minDistance, calculateRightAngle(right, center));
			lastDetection = Detection.RIGHT_ANGLE;
		}
		else if ((left > minThreshold && left < maxThreshold) || (right > minThreshold && right < maxThreshold)
				|| (center > minThreshold && center < maxThreshold)) {
			ObstacleDetector.Detection sensor;
			if (left <= right) {
				if (center <= left)
					sensor = ObstacleDetector.Detection.CENTER_SENS;
				else
					sensor = ObstacleDetector.Detection.LEFT_SENS;
			} else {
				if (center <= right)
					sensor = ObstacleDetector.Detection.CENTER_SENS;
				else
					sensor = ObstacleDetector.Detection.RIGHT_SENS;
			}
			if ((sensor == ObstacleDetector.Detection.LEFT_SENS && lastDetection != Detection.LEFT_SENS)
					|| (sensor == ObstacleDetector.Detection.RIGHT_SENS && lastDetection != Detection.RIGHT_SENS)
					|| (sensor == ObstacleDetector.Detection.CENTER_SENS && lastDetection != Detection.CENTER_SENS)) {
				changeEventListener.onObstacleDetected(sensor, minDistance);
				if(sensor == ObstacleDetector.Detection.RIGHT_SENS)
					lastDetection = Detection.LEFT_SENS;
				else if(sensor == ObstacleDetector.Detection.RIGHT_SENS)
					lastDetection = Detection.RIGHT_SENS;
				else
					lastDetection = Detection.CENTER_SENS;
			}
		} else if(left > maxThreshold && right > maxThreshold && center > maxThreshold && lastDetection != Detection.NONE) {
			changeEventListener.onObstacleDisappeared();
			lastDetection = Detection.NONE;
		}
	}

	@Override
	public void onDistanceBelowThreshold(short left, short center, short right) {
	}

}
