package org.uibk.iis.robotprojectapp;

import android.hardware.SensorManager;

public class Calibrator {
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
		public final double SLOW_VAL;
		public final double MEDM_VAL;
		public final int FAST_VAL;

		public Data(double LEFT_WHEEL_SLOW, double RIGHT_WHEEL_SLOW, double LEFT_WHEEL_MEDM, double RIGHT_WHEEL_MEDM,
				double LEFT_WHEEL_FAST, double RIGHT_WHEEL_FAST, double SLOW_VAL, double MEDM_VAL, int FAST_VAL) {
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
		 * multiplies two Calibration datas, The speed values have to be equal
		 * in both datas. An IllegalArgumentException is thrown otherwise
		 * 
		 * @param d1
		 *            first data
		 * @param d2
		 *            second data
		 * @return the product of both
		 */
		public static Data multiply(Data d1, Data d2) {
			if (d1.SLOW_VAL != d2.SLOW_VAL || d1.MEDM_VAL != d2.MEDM_VAL || d1.FAST_VAL != d2.FAST_VAL)
				throw new IllegalArgumentException("different calibration speed values!");

			return new Data(d1.LEFT_WHEEL_SLOW * d2.LEFT_WHEEL_SLOW, d1.RIGHT_WHEEL_SLOW * d2.RIGHT_WHEEL_SLOW, d1.LEFT_WHEEL_MEDM
					* d2.LEFT_WHEEL_MEDM, d1.RIGHT_WHEEL_MEDM * d2.RIGHT_WHEEL_MEDM, d1.LEFT_WHEEL_FAST * d2.LEFT_WHEEL_FAST,
					d1.RIGHT_WHEEL_FAST * d2.RIGHT_WHEEL_FAST, d1.SLOW_VAL, d1.MEDM_VAL, d1.FAST_VAL);
		}
	}

	private SensorManager sensorManager;
	private int slowVal;
	private int medmVal;
	private int fastVal;

	public Calibrator(SensorManager sensorManager, int slowVal, int medmVal, int fastVal) {
		this.sensorManager = sensorManager;
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
					this.fastVal = this.medmVal;
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

	public Data calibrate(final Type type) {
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
			return Data.multiply(calibrateLeftSlow(), calibrateRightSlow());
		case MEDM:
			return Data.multiply(calibrateLeftMedm(), calibrateRightMedm());
		case FAST:
			return Data.multiply(calibrateLeftFast(), calibrateRightFast());
		case ALL:
		default:
			return Data.multiply(
					Data.multiply(Data.multiply(calibrateLeftSlow(), calibrateRightSlow()),
							Data.multiply(calibrateLeftMedm(), calibrateRightMedm())),
					Data.multiply(calibrateLeftFast(), calibrateRightFast()));

		}
	}

	private Data calibrateLeftSlow() {

	}

	private Data calibrateRightSlow() {

	}

	private Data calibrateLeftMedm() {

	}

	private Data calibrateRightMedm() {

	}

	private Data calibrateLeftFast() {

	}

	private Data calibrateRightFast() {

	}

}
