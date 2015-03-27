package org.uibk.iis.robotprojectapp;

import android.app.Activity;
import java.util.AbstractMap;
import java.util.Map;
import java.lang.Runnable;

public class CalibrationTask implements Runnable {

	public static final long BEARING_SAMPLING_TIME = 100;
	public static final double BEARING_THRESHOLD_ANGLE = 110.0;
	public static final long BEARING_MEASURING_BREAK = 2500;
	// has to be measured more precisely
	public static final double ROBOT_AXLE_LENGTH = 18.7;

	/**
	 * enum for calibration decision: to calibrate all modes take ALL, for the
	 * appropriate speed modes choose either SLOW, MEDM or FAST, to calibrate a
	 * single mode, see the rest...
	 */
	public enum Type {
		ALL, SLOW, MEDM, FAST, LEFT_WHEEL_SLOW, RIGHT_WHEEL_SLOW, LEFT_WHEEL_MEDM, RIGHT_WHEEL_MEDM, LEFT_WHEEL_FAST, RIGHT_WHEEL_FAST,
	}

	/**
	 * Data structure for calibration, will be returned by any calibration
	 * function
	 */
	public static class Data {
		public final double LEFT_WHEEL_SLOW;
		public final double RIGHT_WHEEL_SLOW;
		public final double LEFT_WHEEL_MEDM;
		public final double RIGHT_WHEEL_MEDM;
		public final double LEFT_WHEEL_FAST;
		public final double RIGHT_WHEEL_FAST;
		public final int SLOW_VAL;
		public final int MEDM_VAL;
		public final int FAST_VAL;

		public Data(double LEFT_WHEEL_SLOW, double RIGHT_WHEEL_SLOW, double LEFT_WHEEL_MEDM, double RIGHT_WHEEL_MEDM,
				double LEFT_WHEEL_FAST, double RIGHT_WHEEL_FAST, int SLOW_VAL, int MEDM_VAL, int FAST_VAL) {
			this.LEFT_WHEEL_SLOW = LEFT_WHEEL_SLOW;
			this.RIGHT_WHEEL_SLOW = RIGHT_WHEEL_SLOW;
			this.LEFT_WHEEL_MEDM = LEFT_WHEEL_MEDM;
			this.RIGHT_WHEEL_MEDM = RIGHT_WHEEL_MEDM;
			this.LEFT_WHEEL_FAST = LEFT_WHEEL_FAST;
			this.RIGHT_WHEEL_FAST = RIGHT_WHEEL_FAST;
			this.SLOW_VAL = SLOW_VAL;
			this.MEDM_VAL = MEDM_VAL;
			this.FAST_VAL = FAST_VAL;
		}

		/**
		 * ---not needed anymore--- multiplies two Calibration datas, The speed
		 * values have to be equal in both datas. An IllegalArgumentException is
		 * thrown otherwise
		 * 
		 * @param d1
		 *            first data
		 * @param d2
		 *            second data
		 * @return the product of both
		 */
		public static Data multiply(Data d1, Data d2) throws IllegalArgumentException {
			if (d1.SLOW_VAL != d2.SLOW_VAL || d1.MEDM_VAL != d2.MEDM_VAL || d1.FAST_VAL != d2.FAST_VAL)
				throw new IllegalArgumentException("different calibration speed values!");

			return new Data(d1.LEFT_WHEEL_SLOW * d2.LEFT_WHEEL_SLOW, d1.RIGHT_WHEEL_SLOW * d2.RIGHT_WHEEL_SLOW, d1.LEFT_WHEEL_MEDM
					* d2.LEFT_WHEEL_MEDM, d1.RIGHT_WHEEL_MEDM * d2.RIGHT_WHEEL_MEDM, d1.LEFT_WHEEL_FAST * d2.LEFT_WHEEL_FAST,
					d1.RIGHT_WHEEL_FAST * d2.RIGHT_WHEEL_FAST, d1.SLOW_VAL, d1.MEDM_VAL, d1.FAST_VAL);
		}

		/**
		 * adds two Calibration datas, The speed values have to be equal in both
		 * datas. An IllegalArgumentException is thrown otherwise
		 * 
		 * @param d1
		 *            first data
		 * @param d2
		 *            second data
		 * @return the product of both
		 */
		public static Data add(Data d1, Data d2) throws IllegalArgumentException {
			if (d1.SLOW_VAL != d2.SLOW_VAL || d1.MEDM_VAL != d2.MEDM_VAL || d1.FAST_VAL != d2.FAST_VAL)
				throw new IllegalArgumentException("different calibration speed values!");

			return new Data(d1.LEFT_WHEEL_SLOW + d2.LEFT_WHEEL_SLOW, d1.RIGHT_WHEEL_SLOW + d2.RIGHT_WHEEL_SLOW, d1.LEFT_WHEEL_MEDM
					+ d2.LEFT_WHEEL_MEDM, d1.RIGHT_WHEEL_MEDM + d2.RIGHT_WHEEL_MEDM, d1.LEFT_WHEEL_FAST + d2.LEFT_WHEEL_FAST,
					d1.RIGHT_WHEEL_FAST + d2.RIGHT_WHEEL_FAST, d1.SLOW_VAL, d1.MEDM_VAL, d1.FAST_VAL);
		}
	}

	/**
	 * A Callback Observer for notifying the host who started calibrating
	 */
	public interface Callback {
		void onProgressUpdated(int progress);

		void onProgressState(String progressState);

		void onFinishedCalibration(Data result);

		void onCanceledSuccessfully(boolean val);
	}

	private int slowVal;
	private int medmVal;
	private int fastVal;
	private long time;
	private StopWatch stopwatch;
	private long estimatedTime;
	private Type type;
	private Callback callbackHandler;
	private Activity activity;

	/**
	 * 
	 * @param activity
	 *            the main Activity which has the UI Thread
	 * @param callbackHandler
	 *            the callback handler which is notified about the progress that
	 *            is made during calibration
	 * @param time
	 *            the time in which every single calibration type is made (the
	 *            single types like LEFT_WHEEL_SLOW, RIGHT_WHEEL_MEDM or
	 *            LEFT_WHEEL_FAST)
	 * @param slowVal
	 *            the slow velocity
	 * @param medmVal
	 *            the medium velocity
	 * @param fastVal
	 *            the fast velocity
	 * @param type
	 *            the type of calibration
	 */
	public CalibrationTask(Activity activity, Callback callbackHandler, long time, int slowVal, int medmVal, int fastVal, Type type) {
		this.time = time;
		this.type = type;
		this.callbackHandler = callbackHandler;
		this.activity = activity;
		stopwatch = new StopWatch();
		if (slowVal < 1)
			this.slowVal = 1;
		else if (slowVal > 127)
			this.slowVal = 127;
		else
			this.slowVal = slowVal;
		if (medmVal < 1)
			this.medmVal = 1;
		else if (medmVal > 127)
			this.medmVal = 127;
		else
			this.medmVal = medmVal;
		if (fastVal < 1)
			this.fastVal = 1;
		else if (fastVal > 127)
			this.fastVal = 127;
		else
			this.fastVal = fastVal;
		// sort velocities just for safety
		if (this.slowVal < this.medmVal) {
			if (this.medmVal > this.fastVal) {
				if (this.slowVal < this.fastVal) {
					int tmp = this.medmVal;
					this.medmVal = this.fastVal;
					this.fastVal = tmp;
				} else {
					int tmp = this.medmVal;
					int tmp2 = this.fastVal;
					this.medmVal = this.slowVal;
					this.fastVal = tmp;
					this.slowVal = tmp2;
				}
			}
		} else {
			if (this.slowVal < this.fastVal) {
				int tmp = this.medmVal;
				this.medmVal = this.slowVal;
				this.slowVal = tmp;
			} else {
				if (this.medmVal < this.fastVal) {
					int tmp = this.medmVal;
					int tmp2 = this.fastVal;
					this.fastVal = this.slowVal;
					this.slowVal = tmp;
					this.medmVal = tmp2;
				} else {
					int tmp = this.fastVal;
					this.fastVal = this.slowVal;
					this.slowVal = tmp;
				}
			}
		}
	}

	/**
	 * calculates the estimated time for the whole calibration
	 * 
	 * @return the estimated time
	 */
	private long calculateEstimatedTime() {
		switch (type) {
		case LEFT_WHEEL_SLOW:
		case RIGHT_WHEEL_SLOW:
		case LEFT_WHEEL_MEDM:
		case RIGHT_WHEEL_MEDM:
		case LEFT_WHEEL_FAST:
		case RIGHT_WHEEL_FAST:
			return time + BEARING_MEASURING_BREAK + 200; // extra time needed
															// for measurement
		case SLOW:
		case MEDM:
		case FAST:
			return 2 * (time + BEARING_MEASURING_BREAK + 200); // extra time
																// needed for
																// measurement
		case ALL:
		default:
			return 6 * (time + BEARING_MEASURING_BREAK + 200); // extra time
																// needed for
																// measurement
		}
	}

	/**
	 * starts the calibration for the given type
	 * 
	 * @return the resulting calibration data
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the bearing measurement went wrong(to large
	 *             delta value between two measurements), happens mostly if the
	 *             speed was set to high(>80)
	 */
	public Data calibrate() throws InterruptedException, IllegalArgumentException {
		Data calLeftSlow, calRightSlow, calLeftMedm, calRightMedm, calLeftFast, calRightFast;
		// wait a little for the remaining vibrations caused by the touch
		Thread.sleep(200);
		switch (type) {
		case LEFT_WHEEL_SLOW:
			return calibrateLeftSlow();
		case RIGHT_WHEEL_SLOW:
			return calibrateRightSlow();
		case LEFT_WHEEL_MEDM:
			return calibrateLeftMedm();
		case RIGHT_WHEEL_MEDM:
			return calibrateRightMedm();
		case LEFT_WHEEL_FAST:
			return calibrateLeftFast();
		case RIGHT_WHEEL_FAST:
			return calibrateRightFast();
		case SLOW:
			calLeftSlow = calibrateLeftSlow();
			calRightSlow = calibrateRightSlow();
			if (calLeftSlow == null || calRightSlow == null)
				return null;
			else
				return Data.add(calLeftSlow, calRightSlow);
		case MEDM:
			calLeftMedm = calibrateLeftSlow();
			calRightMedm = calibrateRightSlow();
			if (calLeftMedm == null || calRightMedm == null)
				return null;
			else
				return Data.add(calLeftMedm, calRightMedm);
		case FAST:
			calLeftFast = calibrateLeftFast();
			calRightFast = calibrateRightFast();
			if (calLeftFast == null || calRightFast == null)
				return null;
			else
				return Data.add(calLeftFast, calRightFast);
		case ALL:
		default:
			calLeftSlow = calibrateLeftSlow();
			calRightSlow = calibrateRightSlow();
			calLeftMedm = calibrateLeftMedm();
			calRightMedm = calibrateRightMedm();
			calLeftFast = calibrateLeftFast();
			calRightFast = calibrateRightFast();

			if (calLeftSlow == null || calRightSlow == null || calLeftMedm == null || calRightMedm == null || calLeftFast == null
					|| calRightFast == null)
				return null;
			else
				return Data.add(Data.add(Data.add(calLeftSlow, calRightSlow), Data.add(calLeftMedm, calRightMedm)),
						Data.add(calLeftFast, calRightFast));
		}
	}

	/**
	 * calculates the delta Value of two given angles, both angles have to lie
	 * within the BEARING_THRESHOLD_ANGLE, otherwise incorrect results would
	 * appear(the direction is not decidable)
	 * 
	 * @param lastAngle
	 *            in degrees
	 * @param newAngle
	 *            in degrees
	 * @return the delta value between the two angles in degrees
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private double getDeltaAngle(double lastAngle, double newAngle) throws IllegalArgumentException {
		double diff = newAngle - lastAngle;
		if (Math.abs(diff) < BEARING_THRESHOLD_ANGLE)
			return diff;
		if (lastAngle > newAngle) {
			diff = (360.0 + newAngle) - lastAngle;
			if (Math.abs(diff) < BEARING_THRESHOLD_ANGLE)
				return diff;
			else
				throw new IllegalArgumentException("difference between last and new angle is greater than the threshold: " + diff + ">"
						+ BEARING_THRESHOLD_ANGLE + ", lastAngle: " + lastAngle + ", newAngle: " + newAngle);
		}
		if (newAngle > lastAngle) {
			diff = newAngle - (lastAngle + 360.0);
			if (Math.abs(diff) < BEARING_THRESHOLD_ANGLE)
				return diff;
			else
				throw new IllegalArgumentException("difference between last and new angle is greater than the threshold: " + diff + ">"
						+ BEARING_THRESHOLD_ANGLE + ", lastAngle: " + lastAngle + ", newAngle: " + newAngle);

		}
		return 0;
	}

	private AbstractMap.SimpleEntry<Double, Long> measureTotalAngle(byte leftVal, byte rightVal) throws InterruptedException,
			IllegalArgumentException {
		double lastBearing = BearingToNorthSingleton.getInstance().getBearing() + 180.0;
		double turnAngle = 0;
		StopWatch sw = new StopWatch();
		// start wheel(s) with given speed:
		if (ComDriver.getInstance().isConnected())
			ComDriver.getInstance().comReadWrite(new byte[] { 'i', leftVal, rightVal, '\r', '\n' });
		sw.start();
		while (sw.getTime() < time) {
			Thread.sleep(BEARING_SAMPLING_TIME);
			// Update the progressBar
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					callbackHandler.onProgressUpdated((int) (100 * stopwatch.getTime() / estimatedTime));
				}
			});
			double currentBearing = BearingToNorthSingleton.getInstance().getBearing() + 180.0;
			double deltaBearing = getDeltaAngle(lastBearing, currentBearing);

			lastBearing = currentBearing;
			turnAngle += deltaBearing;
		}
		// stop wheel(s)
		if (ComDriver.getInstance().isConnected())
			ComDriver.getInstance().comReadWrite(new byte[] { 'i', 0, 0, '\r', '\n' });
		sw.stop();
		// sleep a little bit to get a better final value
		Thread.sleep(BEARING_MEASURING_BREAK);
		return new AbstractMap.SimpleEntry<Double, Long>((turnAngle + getDeltaAngle(lastBearing, BearingToNorthSingleton.getInstance()
				.getBearing() + 180.0)), sw.getTime());

	}

	/**
	 * calibrates the left wheel with slow velocity
	 * 
	 * @return speed in centimeters per second
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private Data calibrateLeftSlow() throws InterruptedException, IllegalArgumentException {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				callbackHandler.onProgressState("calibrating left wheel with slow speed: " + slowVal);
			}
		});
		Map.Entry<Double, Long> angleTimePair = measureTotalAngle((byte) slowVal, (byte) 0);
		double distance = angleTimePair.getKey() / 360.0 * Math.PI * ROBOT_AXLE_LENGTH;
		// return centimeter per second
		return new Data(Math.abs(distance / ((double) angleTimePair.getValue() / 1000.0)), 0, 0, 0, 0, 0, slowVal, medmVal, fastVal);
	}

	/**
	 * calibrates the right wheel with slow velocity
	 * 
	 * @return speed in centimeters per second
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private Data calibrateRightSlow() throws IllegalArgumentException, InterruptedException {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				callbackHandler.onProgressState("calibrating right wheel with slow speed: " + slowVal);
			}
		});
		Map.Entry<Double, Long> angleTimePair = measureTotalAngle((byte) 0, (byte) slowVal);
		double distance = angleTimePair.getKey() / 360.0 * Math.PI * ROBOT_AXLE_LENGTH;
		// return centimeter per second
		return new Data(0, Math.abs(distance / ((double) angleTimePair.getValue() / 1000.0)), 0, 0, 0, 0, slowVal, medmVal, fastVal);
	}

	/**
	 * calibrates the left wheel with medium velocity
	 * 
	 * @return speed in centimeters per second
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private Data calibrateLeftMedm() throws IllegalArgumentException, InterruptedException {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				callbackHandler.onProgressState("calibrating left wheel with medium speed: " + medmVal);
			}
		});
		Map.Entry<Double, Long> angleTimePair = measureTotalAngle((byte) medmVal, (byte) 0);
		double distance = angleTimePair.getKey() / 360.0 * Math.PI * ROBOT_AXLE_LENGTH;
		// return centimeter per second
		return new Data(0, 0, Math.abs(distance / ((double) angleTimePair.getValue() / 1000.0)), 0, 0, 0, slowVal, medmVal, fastVal);

	}

	/**
	 * calibrates the right wheel with medium velocity
	 * 
	 * @return speed in centimeters per second
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private Data calibrateRightMedm() throws IllegalArgumentException, InterruptedException {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				callbackHandler.onProgressState("calibrating right wheel with medium speed: " + medmVal);
			}
		});
		Map.Entry<Double, Long> angleTimePair = measureTotalAngle((byte) 0, (byte) medmVal);
		double distance = angleTimePair.getKey() / 360.0 * Math.PI * ROBOT_AXLE_LENGTH;
		// return centimeter per second
		return new Data(0, 0, 0, Math.abs(distance / ((double) angleTimePair.getValue() / 1000.0)), 0, 0, slowVal, medmVal, fastVal);
	}

	/**
	 * calibrates the left wheel with fast velocity
	 * 
	 * @return speed in centimeters per second
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private Data calibrateLeftFast() throws IllegalArgumentException, InterruptedException {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				callbackHandler.onProgressState("calibrating left wheel with fast speed: " + fastVal);
			}
		});
		Map.Entry<Double, Long> angleTimePair = measureTotalAngle((byte) fastVal, (byte) 0);
		double distance = angleTimePair.getKey() / 360.0 * Math.PI * ROBOT_AXLE_LENGTH;
		// return centimeter per second
		return new Data(0, 0, 0, 0, Math.abs(distance / ((double) angleTimePair.getValue() / 1000.0)), 0, slowVal, medmVal, fastVal);
	}

	/**
	 * calibrates the right wheel with fast velocity
	 * 
	 * @return speed in centimeters per second
	 * @throws InterruptedException
	 *             is thrown when an interruption by mainly the host is made
	 * @throws IllegalArgumentException
	 *             is thrown when the BEARING_THRESHOLD_ANGLE is exceeded
	 */
	private Data calibrateRightFast() throws IllegalArgumentException, InterruptedException {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				callbackHandler.onProgressState("calibrating left wheel with fast speed: " + fastVal);
			}
		});
		Map.Entry<Double, Long> angleTimePair = measureTotalAngle((byte) 0, (byte) fastVal);
		double distance = angleTimePair.getKey() / 360.0 * Math.PI * ROBOT_AXLE_LENGTH;
		// return centimeter per second
		return new Data(0, 0, 0, 0, 0, Math.abs(distance / ((double) angleTimePair.getValue() / 1000.0)), slowVal, medmVal, fastVal);
	}

	/**
	 * Main loop
	 */
	@Override
	public void run() {
		estimatedTime = calculateEstimatedTime();
		stopwatch.start();
		
		try {
			final Data result = calibrate();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
			callbackHandler.onFinishedCalibration(result);
				}
			});
		} catch (IllegalArgumentException e) {
			ComDriver.getInstance().comReadWrite(new byte[] { 'i', 0, 0, '\r', '\n' });
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					callbackHandler
							.onProgressState("internal error while turning, the compass used for calibration served unexpected results!");
					callbackHandler.onCanceledSuccessfully(false);
				}
			});
			e.printStackTrace();
		} catch (InterruptedException e) {
			ComDriver.getInstance().comReadWrite(new byte[] { 'i', 0, 0, '\r', '\n' });
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					callbackHandler.onCanceledSuccessfully(true);
				}
			});
		}
		stopwatch.stop();
	}

}
