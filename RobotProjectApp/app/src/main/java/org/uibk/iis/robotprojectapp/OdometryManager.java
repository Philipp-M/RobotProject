package org.uibk.iis.robotprojectapp;

import android.content.Context;
import android.util.Log;

public class OdometryManager {
	static final double CALIBRATION_FACTOR_LEFT = 1;
	static final double CALIBRATION_FACTOR_RIGHT = 1;
	private Position pos;
	private CalibrationTask.Data robotSpeeds;

	private static final class InstanceHolder {
		static final OdometryManager INSTANCE = new OdometryManager();
	}

	public static OdometryManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	public synchronized void init(Context context, double x, double y, double theta) {
		robotSpeeds = CalibrationTask.Data.loadSavedData(context);
		pos = new Position(x, y, theta);
	}

	public Position getPosition() {
		return pos;
	}

	public void resetPosition() {
		this.pos = new Position(0, 0, 0);
	}

	public synchronized boolean pivotAngleNonStopping(double theta, CalibrationTask.Type speed) {
		int robotSpeed;
		double robotSpeedCmL;
		double robotSpeedCmR;
		double time;
		boolean rightTurn = theta < 0 ? true : false;

		switch (speed) {
			case MEDM:
				robotSpeed = robotSpeeds.MEDM_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_MEDM;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_MEDM;
				break;
			case FAST:
				robotSpeed = robotSpeeds.FAST_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_FAST;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_FAST;
				break;
			case SLOW:
			default:
				robotSpeed = robotSpeeds.SLOW_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_SLOW;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_SLOW;
				break;
		}
		time = Math.abs(theta) * CalibrationTask.ROBOT_AXLE_LENGTH / robotSpeedCmL;
		if (time > 0.03 && (ComDriver.getInstance().isConnected())) {
			if (rightTurn)
				ComDriver.getInstance().comReadWrite(
						new byte[]{'i', (byte) Math.round(robotSpeed / 2.0),
								(byte) -Math.round((robotSpeed * robotSpeedCmL / robotSpeedCmR) / 2.0), '\r', '\n'});
			else
				ComDriver.getInstance().comReadWrite(
						new byte[]{'i', (byte) -Math.round(robotSpeed / 2.0),
								(byte) Math.round((robotSpeed * robotSpeedCmL / robotSpeedCmR) / 2.0), '\r', '\n'});
			StopWatch sw = new StopWatch();
			sw.start();
			try {
				Thread.sleep((long) (time * 1000.0f));
			} catch (InterruptedException e) {
				time = (double) sw.getTime() / 1000.0;
				theta = (theta > 0 ? 1 : -1) * robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH;
				return false;
			} finally {
				// update current angle
				pos.theta += theta;
			}
		}
		return true;
	}

	/**
	 * turns around the base position(also called pivot)
	 *
	 * @param theta the delta angle to turn around
	 * @param speed can either be SLOW, MEDM or FAST, every other value will be interpreted as SLOW
	 * @throws InterruptedException
	 */
	public synchronized boolean pivotAngle(double theta, CalibrationTask.Type speed) {
		boolean retVal = pivotAngleNonStopping(theta, speed);
		if (ComDriver.getInstance().isConnected())
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
		return retVal;
	}

	public synchronized boolean turnAngleNonStopping(double theta, CalibrationTask.Type speed, boolean reverse) {
		double robotSpeed;
		double robotSpeedCmL;
		double robotSpeedCmR;
		double time;
		boolean leftWheel = theta < 0 ? true : false;
		switch (speed) {
			case MEDM:
				robotSpeed = robotSpeeds.MEDM_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_MEDM;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_MEDM;
				break;
			case FAST:
				robotSpeed = robotSpeeds.FAST_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_FAST;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_FAST;
				break;
			case SLOW:
			default:
				robotSpeed = robotSpeeds.SLOW_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_SLOW;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_SLOW;
				break;
		}
		if (leftWheel) {
			time = Math.abs(theta) * CalibrationTask.ROBOT_AXLE_LENGTH / robotSpeedCmL;
			if (time > 0.03 && (ComDriver.getInstance().isConnected())) {
				ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) ((reverse ? -1 : 1) * robotSpeed), (byte) 0, '\r', '\n'});
				StopWatch sw = new StopWatch();
				sw.start();
				try {
					Thread.sleep((long) (time * 1000.0f));
				} catch (InterruptedException e) {
					time = (double) sw.getTime() / 1000.0;
					return false;
				} finally {
					// update current angle and position
					pos.x += (reverse ? 1 : -1)
							* (-CalibrationTask.ROBOT_AXLE_LENGTH / 2.0 * (Math.sin(-robotSpeedCmL * time
							/ CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.sin(pos.theta)));
					pos.y += (reverse ? 1 : -1)
							* (CalibrationTask.ROBOT_AXLE_LENGTH / 2.0 * (Math.cos(-robotSpeedCmL * time
							/ CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.cos(pos.theta)));
					pos.theta += (reverse ? 1 : -1) * (-robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH);
				}

			}
		} else {
			time = Math.abs(theta) * CalibrationTask.ROBOT_AXLE_LENGTH / robotSpeedCmR;
			if (time > 0.03 && (ComDriver.getInstance().isConnected())) {
				ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) ((reverse ? -1 : 1) * robotSpeed), '\r', '\n'});
				StopWatch sw = new StopWatch();
				sw.start();
				try {
					Thread.sleep((long) (time * 1000.0f));
				} catch (InterruptedException e) {
					time = (double) sw.getTime() / 1000.0;
					return false;
				} finally {
					// update current angle and position
					pos.x += (reverse ? 1 : -1) * CalibrationTask.ROBOT_AXLE_LENGTH / 2.0
							* (Math.sin(robotSpeedCmR * time / CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.sin(pos.theta));
					pos.y += (reverse ? 1 : -1)
							* (-CalibrationTask.ROBOT_AXLE_LENGTH / 2.0 * (Math.cos(robotSpeedCmR * time
							/ CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.cos(pos.theta)));
					pos.theta += (reverse ? 1 : -1) * (-robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH);
				}
			}
		}
		return true;
	}

	/**
	 * turns around an angle(one wheel, so it changes the base position - caution !)
	 *
	 * @param theta the delta value to turn around
	 * @param speed can either be SLOW, MEDM or FAST, every other value will be interpreted as SLOW
	 * @throws InterruptedException
	 */
	public synchronized boolean turnAngle(double theta, CalibrationTask.Type speed, boolean reverse) {
		boolean retVal = turnAngleNonStopping(theta, speed, reverse);
		ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
		return retVal;
	}

	public synchronized boolean driveForwardNonStopping(double distance, CalibrationTask.Type speed, boolean reverse) {
		double robotSpeed;
		double robotSpeedCmL;
		double robotSpeedCmR;
		double time;
		distance = Math.abs(distance);
		switch (speed) {
			case MEDM:
				robotSpeed = robotSpeeds.MEDM_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_MEDM;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_MEDM;
				break;
			case FAST:
				robotSpeed = robotSpeeds.FAST_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_FAST;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_FAST;
				break;
			case SLOW:
			default:
				robotSpeed = robotSpeeds.SLOW_VAL;
				robotSpeedCmL = robotSpeeds.LEFT_WHEEL_SLOW;
				robotSpeedCmR = robotSpeeds.RIGHT_WHEEL_SLOW;
				break;
		}
		time = distance / robotSpeedCmL;
		distance = reverse ? -distance : distance;
		if (time > 0.03 && (ComDriver.getInstance().isConnected())) {
			if (!reverse)
				ComDriver.getInstance().comReadWrite(
						new byte[]{'i', (byte) robotSpeed, (byte) (robotSpeed * robotSpeedCmL / robotSpeedCmR), '\r', '\n'});
			else
				ComDriver.getInstance().comReadWrite(
						new byte[]{'i', (byte) -robotSpeed, (byte) -(robotSpeed * robotSpeedCmL / robotSpeedCmR), '\r', '\n'});
			StopWatch sw = new StopWatch();
			sw.start();
			try {
				Thread.sleep((long) (time * 1000.0f));
			} catch (InterruptedException e) {
				distance = (double) sw.getTime() / 1000.0 * robotSpeedCmL;
				return false;
			} finally {
				// update current position
				pos.x += Math.cos(pos.theta) * distance;
				pos.y += Math.sin(pos.theta) * distance;
			}
		}
		return true;
	}

	public synchronized boolean driveForwardNonStopping(double distance, CalibrationTask.Type speed) {
		return driveForwardNonStopping(distance, speed, false);
	}

	public synchronized boolean driveBackwardsNonStopping(double distance, CalibrationTask.Type speed) {
		return driveForwardNonStopping(distance, speed, true);
	}

	public synchronized void driveForward(double distance, CalibrationTask.Type speed) {
		driveForwardNonStopping(distance, speed, false);
		if (ComDriver.getInstance().isConnected())
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
	}

	public synchronized void driveBackwards(double distance, CalibrationTask.Type speed) {
		driveForwardNonStopping(distance, speed, true);
		if (ComDriver.getInstance().isConnected())
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
	}

	public synchronized void stop() {
		if (ComDriver.getInstance().isConnected())
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
	}

	/**
	 * drives to a given position and turns to the given angle
	 *
	 * @param x        the new 'x' position in cartesian coordinates
	 * @param y        the new 'y' position in cartesian coordinates
	 * @param newTheta the new absolute angle (counterclockwise) based on the x-axis
	 * @param speed    can either be SLOW, MEDM or FAST, every other value will be interpreted as SLOW
	 * @return if the execution went well 'true' else false
	 * @throws InterruptedException
	 */
	public synchronized boolean driveStraightTo(double x, double y, double newTheta, CalibrationTask.Type speed) {
		double xDiff = x - pos.x;
		double yDiff = y - pos.y;
		double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		if (distance > 2) {
			double tmpTheta = Math.asin(yDiff / distance);
			double tmpTheta1;
			double tmpTheta2;
			if (xDiff < 0 && yDiff > 0)
				tmpTheta = Math.toRadians(180) - tmpTheta;
			if (xDiff < 0 && yDiff < 0)
				tmpTheta = -Math.toRadians(180) - tmpTheta;
			tmpTheta1 = tmpTheta - pos.theta;
			tmpTheta2 = newTheta - tmpTheta;

			// following is possible due to "short-circuit-evaluation"
			return (!Thread.interrupted() && pivotAngleNonStopping(tmpTheta1, speed) && !Thread.interrupted()
					&& driveForwardNonStopping(distance, speed) && !Thread.interrupted() && pivotAngle(tmpTheta2, speed));
		} else {
			return (pivotAngle(newTheta - pos.theta, speed));
		}
	}

	public static class Position {
		public Position(double x, double y, double theta) {
			this.x = x;
			this.y = y;
			this.theta = theta;
		}

		private double x;
		private double y;
		private double theta;

		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}

		public double getTheta() {
			return theta;
		}

		public void setTheta(double theta) {
			this.theta = theta;
		}

		public boolean equals(Position pos, double epsilon) {
			if (this.x + epsilon > pos.x && this.x - epsilon < pos.x && this.x + epsilon > pos.y && this.y - epsilon < pos.y
					&& this.theta + epsilon * 0.1 > pos.theta && this.theta - epsilon * 0.1 < pos.theta)
				return true;
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(theta);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(x);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(y);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Position other = (Position) obj;
			return equals(other, 0.5);
		}
	}
}
