package org.uibk.iis.robotprojectapp.geometry;

import java.util.ArrayList;
import java.util.List;


/**
 * Catmull-Rom Spline implementation slightly modified version from here:
 * http://hawkesy.blogspot.co.at/2010/05/catmull-rom-spline-curve-implementation.html
 */
public class Spline {
	private static class CatmullRomSpline {
		private double p0, p1, p2, p3;

		public CatmullRomSpline(double p0, double p1, double p2, double p3) {
			this.p0 = p0;
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
		}

		public double interpolate(double x) {
			return 0.5 * ((2.0 * p1) + (p2 - p0) * x + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * x * x + (3.0 * p1 - p0 - 3.0 * p2 + p3) * x
					* x * x);
		}

		public double getP0() {
			return p0;
		}

		public void setP0(double p0) {
			this.p0 = p0;
		}

		public double getP1() {
			return p1;
		}

		public void setP1(double p1) {
			this.p1 = p1;
		}

		public double getP2() {
			return p2;
		}

		public void setP2(double p2) {
			this.p2 = p2;
		}

		public double getP3() {
			return p3;
		}

		public void setP3(double p3) {
			this.p3 = p3;
		}
	}

	private static class CatmullRomSpline2D {
		private CatmullRomSpline splineXVals, splineYVals;

		public CatmullRomSpline2D(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
			splineXVals = new CatmullRomSpline(p0.getX(), p1.getX(), p2.getX(), p3.getX());
			splineYVals = new CatmullRomSpline(p0.getY(), p1.getY(), p2.getY(), p3.getY());
		}

		public Point2D interpolate(double t) {
			return new Point2D(splineXVals.interpolate(t), splineYVals.interpolate(t));
		}
	}

	public static List<Point2D> subdividePoints(List<Point2D> points, int subdivisions) {
		if (points.size() < 3)
			return null;
		ArrayList<Point2D> subdividedPoints = new ArrayList<Point2D>(((points.size() - 1) * subdivisions) + 1);

		double increments = 1.0 / (double) subdivisions;

		for (int i = 0; i < points.size() - 1; i++) {
			Point2D p0 = i == 0 ? points.get(i) : points.get(i - 1);
			Point2D p1 = points.get(i);
			Point2D p2 = points.get(i + 1);
			Point2D p3 = (i + 2 == points.size()) ? points.get(i + 1) : points.get(i + 2);

			CatmullRomSpline2D crs = new CatmullRomSpline2D(p0, p1, p2, p3);

			for (int j = 0; j <= subdivisions; j++) {
				subdividedPoints.add(crs.interpolate(j * increments));
			}
		}

		return subdividedPoints;
	}
}
