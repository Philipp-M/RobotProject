package org.uibk.iis.robotprojectapp;

/**
 * Created by philm on 5/31/15.
 */
public class Point extends org.opencv.core.Point {
	public Point() {
	}

	public Point(double[] vals) {
		super(vals);
	}

	public Point(double x, double y) {
		super(x,y);
	}

	public double length() {
		return Math.sqrt(x * x + y * y);
	}
	public double lengthSqr() {
		return x * x + y * y;
	}
	public double distanceTo(Point p) {
		return Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y));
	}
	public void add(Point p) {
		this.x += p.x;
		this.y += p.y;
	}
	public void subtract(Point p) {
		this.x -= p.x;
		this.y -= p.y;
	}

}
