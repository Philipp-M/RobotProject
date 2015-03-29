package org.uibk.iis.robotprojectapp.geometry;

/**
 * since Java's generic-numeric abilities are ... somehow ... limited, a
 * hardcoded Version with doubles is used here..
 *
 */
public class Point2D {
	private double x;
	private double y;

	public Point2D(double x, double y) {
		this.x = x;
		this.x = y;
	}

	public Point2D() {
		this(0.0, 0.0);
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public Point2D add(Point2D p) {
		return new Point2D(getX() + p.getX(), getY() + p.getY());
	}

	public Point2D subtract(Point2D p) {
		return new Point2D(getX() - p.getX(), getY() - p.getY());
	}

	public Point2D multiply(Point2D p) {
		return new Point2D(getX() * p.getX(), getY() * p.getY());
	}

	public double dot(Point2D p) {
		return getX() * p.getX() + getY() * p.getY();
	}

}
