package org.uibk.iis.robotprojectapp;

import android.content.Context;

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
		if (time > 0.03) {
			if (rightTurn)
				ComDriver.getInstance().comReadWrite(
						new byte[] { 'i', (byte) Math.round(robotSpeed / 2.0),
								(byte) -Math.round((robotSpeed * robotSpeedCmL / robotSpeedCmR) / 2.0), '\r', '\n' });
			else
				ComDriver.getInstance().comReadWrite(
						new byte[] { 'i', (byte) -Math.round(robotSpeed / 2.0),
								(byte) Math.round((robotSpeed * robotSpeedCmL / robotSpeedCmR) / 2.0), '\r', '\n' });
			StopWatch sw = new StopWatch();
			sw.start();
			try {
				Thread.sleep((long) (time * 1000.0f));
			} catch (InterruptedException e) {
				time = (double) sw.getTime() / 1000.0;
				theta = -robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH;
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
	 * @param theta
	 *            the delta angle to turn around
	 * @param speed
	 *            can either be SLOW, MEDM or FAST, every other value will be
	 *            interpreted as SLOW
	 * @throws InterruptedException
	 */
	public synchronized boolean pivotAngle(double theta, CalibrationTask.Type speed) {
		boolean retVal = pivotAngleNonStopping(theta, speed);
		ComDriver.getInstance().comReadWrite(new byte[] { 'i', (byte) 0, (byte) 0, '\r', '\n' });
		return retVal;
	}

	public synchronized boolean turnAngleNonStopping(double theta, CalibrationTask.Type speed) {
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
			if (time > 0.03) {
				ComDriver.getInstance().comReadWrite(new byte[] { 'i', (byte) robotSpeed, (byte) 0, '\r', '\n' });
				StopWatch sw = new StopWatch();
				sw.start();
				try {
					Thread.sleep((long) (time * 1000.0f));
				} catch (InterruptedException e) {
					time = (double) sw.getTime() / 1000.0;
					return false;
				} finally {
					// update current angle and position
					pos.x += -CalibrationTask.ROBOT_AXLE_LENGTH / 2.0
							* (Math.sin(-robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.sin(pos.theta));
					pos.y += CalibrationTask.ROBOT_AXLE_LENGTH / 2.0
							* (Math.cos(-robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.cos(pos.theta));
					pos.theta += -robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH;
				}

			}
		} else {
			time = Math.abs(theta) * CalibrationTask.ROBOT_AXLE_LENGTH / robotSpeedCmR;
			if (time > 0.03) {
				ComDriver.getInstance().comReadWrite(new byte[] { 'i', (byte) 0, (byte) robotSpeed, '\r', '\n' });
				StopWatch sw = new StopWatch();
				sw.start();
				try {
					Thread.sleep((long) (time * 1000.0f));
				} catch (InterruptedException e) {
					time = (double) sw.getTime() / 1000.0;
					return false;
				} finally {
					// update current angle and position
					pos.x += +CalibrationTask.ROBOT_AXLE_LENGTH / 2.0
							* (Math.sin(robotSpeedCmR * time / CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.sin(pos.theta));
					pos.y += -CalibrationTask.ROBOT_AXLE_LENGTH / 2.0
							* (Math.cos(robotSpeedCmR * time / CalibrationTask.ROBOT_AXLE_LENGTH + pos.theta) - Math.cos(pos.theta));
					pos.theta += -robotSpeedCmL * time / CalibrationTask.ROBOT_AXLE_LENGTH;
				}
			}
		}
		return true;
	}

	/**
	 * turns around an angle(one wheel, so it changes the base position -
	 * caution !)
	 * 
	 * @param theta
	 *            the delta value to turn around
	 * @param speed
	 *            can either be SLOW, MEDM or FAST, every other value will be
	 *            interpreted as SLOW
	 * @throws InterruptedException
	 */
	public synchronized boolean turnAngle(double theta, CalibrationTask.Type speed) {
		boolean retVal = turnAngleNonStopping(theta, speed);
		ComDriver.getInstance().comReadWrite(new byte[] { 'i', (byte) 0, (byte) 0, '\r', '\n' });
		return retVal;
	}

	public synchronized boolean driveForwardNonStopping(double distance, CalibrationTask.Type speed) {
		double robotSpeed;
		double robotSpeedCmL;
		double robotSpeedCmR;
		double time;

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
		if (time > 0.03) {
			ComDriver.getInstance().comReadWrite(
					new byte[] { 'i', (byte) robotSpeed, (byte) (robotSpeed * robotSpeedCmL / robotSpeedCmR), '\r', '\n' });
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

	public synchronized void driveForward(double distance, CalibrationTask.Type speed) {
		driveForward(distance, speed);
		ComDriver.getInstance().comReadWrite(new byte[] { 'i', (byte) 0, (byte) 0, '\r', '\n' });
	}
	
	public synchronized void stop() {
		ComDriver.getInstance().comReadWrite(new byte[] { 'i', (byte) 0, (byte) 0, '\r', '\n' });
	}

	/**
	 * drives to a given position and turns to the given angle
	 * 
	 * @param x
	 *            the new 'x' position in cartesian coordinates
	 * @param y
	 *            the new 'y' position in cartesian coordinates
	 * @param newTheta
	 *            the new absolute angle (counterclockwise) based on the x-axis
	 * @param speed
	 *            can either be SLOW, MEDM or FAST, every other value will be
	 *            interpreted as SLOW
	 * @throws InterruptedException
	 * @return if the execution went well 'true' else false
	 */
	public synchronized boolean driveStraightTo(double x, double y, double newTheta, CalibrationTask.Type speed) {
		double xDiff = x - pos.x;
		double yDiff = y - pos.y;
		double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		if (distance > 0.2) {
			double tmpTheta1 = (Math.asin(yDiff / distance) - pos.theta);
			double tmpTheta2 = (newTheta - Math.asin(yDiff / distance));
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
	}
}
