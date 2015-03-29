package org.uibk.iis.robotprojectapp;

public class Odometry {
	static final double CALIBRATION_FACTOR_LEFT = 1;
	static final double CALIBRATION_FACTOR_RIGHT = 1;
	private Position pos;

	public Odometry(double x, double y, double theta) {
		super();
		pos = new Position(x, y, theta);
	}

	public double getX() {
		return pos.getX();
	}

	public double getY() {
		return pos.getY();
	}

	public double getTheta() {
		return pos.getTheta();
	}
	public Position getPosition() {
		return pos;
	}

	public void driveStraightTo(double x, double y, double newTheta) {

	}
	//public drivePoints()
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
