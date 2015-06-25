package org.uibk.iis.robotprojectapp;


import android.util.Log;

public class BeaconLocalizer {
	public interface BeaconLocalizerListener {
		void onLocalized(Point p, double angle);
	}

	private Point[] beacons;
	private int beaconNum1;
	private int beaconNum2;
	private int beaconAngle;
	private Point beaconToDetect1;
	private Point beaconToDetect2;
	private BeaconLocalizer.BeaconLocalizerListener listener;
	private boolean finished;

	public BeaconLocalizer(BeaconLocalizer.BeaconLocalizerListener listener) {
		this.listener = listener;
		finished = false;
		beaconToDetect1 = null;
		beaconToDetect2 = null;
		beaconNum1 = -1;
		beaconNum2 = -1;
		beaconAngle = 0;
		beacons = new Point[8];
		beacons[0] = new Point(-125, 125);
//		beacons[0] = new Point(-90, -45);
		beacons[1] = new Point(125, 125);
		beacons[2] = new Point(125, -125);
		beacons[3] = new Point(-125, -125);
//		beacons[3] = new Point(125, 0);
//		beacons[4] = new Point(125, -125);
//		beacons[5] = new Point(0, -125);
//		beacons[6] = new Point(-125, -125);
//		beacons[7] = new Point(-125, 0);
//		beacons[0] = new Point(-125, 125);
////		beacons[0] = new Point(-90, -45);
//		beacons[1] = new Point(0, 125);
//		beacons[2] = new Point(125, 125);
//		beacons[3] = new Point(125, 0);
//		beacons[4] = new Point(125, -125);
//		beacons[5] = new Point(0, -125);
//		beacons[6] = new Point(-125, -125);
//		beacons[7] = new Point(-125, 0);
////		beacons[5] = new Point(-90, 45);
	}

	public void setBeacon(int num, Point pos) {
		Log.d("BeaconLocal", "beaconLocalization: beaconPos: " + pos.x + ", " + pos.y);
		if (beaconNum1 == -1) {
			beaconNum1 = num;
			beaconToDetect1 = pos;
		} else {
			beaconNum2 = num;
			beaconToDetect2 = pos;
			// start calculation;
			Point[] intPoints = intersectionCircles(beacons[beaconNum1], beaconToDetect1.length(), beacons[beaconNum2], beaconToDetect2.length());
			Point resultingPosition = null;
			if (intPoints == null)
				throw new RuntimeException("there was no intersection between the beacons, which should be impossible!");
			if (intPoints.length == 1)
				resultingPosition = intPoints[0];
			else if (intPoints.length == 2) {
				if (intPoints[0].x < -125 || intPoints[0].x > 125 || intPoints[0].y < -125 || intPoints[0].y > 125)
					resultingPosition = intPoints[1];
				else
					resultingPosition = intPoints[0];
			}
			double deltaAngle = Math.asin(beaconToDetect2.x / beaconToDetect2.length());
			double absLength = resultingPosition.distanceTo(beacons[beaconNum2]);
			double beaconAngle = Math.atan2(beacons[beaconNum2].y - resultingPosition.y, beacons[beaconNum2].x - resultingPosition.x);
//			double beaconAngle = Math.asin((resultingPosition.x - beacons[beaconNum2].x) / absLength);
			double angle = beaconAngle - deltaAngle;

			finished = true;
			listener.onLocalized(resultingPosition, angle);

		}

	}

	public static Point[] intersectionCircles(Point a, double ra, Point b, double rb) {
		Point diff = new Point();
		diff.x = b.x - a.x;
		diff.y = b.y - a.y;
		double diffLength = Math.sqrt(diff.x * diff.x + diff.y * diff.y);
		if (diffLength == 0) // same center
			return null;
		double x = (ra * ra + diffLength * diffLength - rb * rb) / (2 * diffLength);
		double y = ra * ra - x * x;

		if (y < 0) // no intersection
			return null;
		if (y > 0)
			y = Math.sqrt(y);
		// compute unit vectors
		double ex0 = diff.x / diffLength;
		double ex1 = diff.y / diffLength;
		double ey0 = -ex1;
		double ey1 = ex0;

		double q1x = a.x + x * ex0;
		double q1y = a.y + x * ex1;

		if (y == 0) { // one touch point
			Point[] retVal = new Point[1];
			retVal[0] = new Point(q1x, q1y);
			return retVal;
		}

		// two intersections
		double q2x = q1x - y * ey0;
		double q2y = q1y - y * ey1;
		q1x += y * ey0;
		q1y += y * ey1;

		Point[] retVal = new Point[2];
		retVal[0] = new Point(q1x, q1y);
		retVal[1] = new Point(q2x, q2y);
		return retVal;
	}
}
