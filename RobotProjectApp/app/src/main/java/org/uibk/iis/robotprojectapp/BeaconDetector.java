package org.uibk.iis.robotprojectapp;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.uibk.iis.robotprojectapp.WorldPositionCalculator;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BeaconDetector {

	//	private static final double CAMERA_FIELD_OF_VIEW = 1.0155659339644644474;
	public static final double CAMERA_FIELD_OF_VIEW = 1.15055659339644644474;
	private static final double BEACON_COLOR_HEIGHT = 10;
	private static final double VERTICAL_THRESHOLD = 3;

	public interface BeaconDetectorListener {
		void onBeaconDetected(double x, double y, int beaconNum);
	}

	private int upperColor;
	private int lowerColor;
	private int beaconNum;
	List<MatOfPoint>[] contours;
	List<WorldObject>[] worldObjects;
	BeaconDetectorListener listener;

	public BeaconDetector(int upperColor, int lowerColor, BeaconDetectorListener listener, int beaconNum) {
		this.upperColor = upperColor;
		this.lowerColor = lowerColor;
		this.listener = listener;
		this.beaconNum = beaconNum;
	}

	public void detectBeacon(List<WorldObject>[] worldObjects) {
		List<WorldObject> upperObjects = worldObjects[upperColor];
		List<WorldObject> lowerObjects = worldObjects[lowerColor];
		List<Map.Entry<Integer, Integer>> possibleObjects = new ArrayList<>();

		for (WorldObject woU : upperObjects) {
			for (WorldObject woL : lowerObjects) {
				double disToL = woL.worldPos.length();
				double disToU = woU.worldPos.length();
				double radiusCalculated = disToL * 2.0 * Math.tan(CAMERA_FIELD_OF_VIEW / 2) * woL.iR * 0.5;
//				double radiusCalculated = disToL * Math.tan(CAMERA_FIELD_OF_VIEW * woL.iR/2);
				//Log.d("Homography", "radius: " + radiusCalculated);
				if (disToL < disToU && radiusCalculated < 0.5 * 1.5 * BEACON_COLOR_HEIGHT && radiusCalculated > 0.5 * 0.75 * BEACON_COLOR_HEIGHT) {
					//double dis = disToL * Math.tan(CAMERA_FIELD_OF_VIEW * getDistanceTo(woL.iC, woU.iC) / 2);
					double disX = disToL * 2.0 * Math.tan(CAMERA_FIELD_OF_VIEW / 2.0) * Math.abs(woL.iC.x - woU.iC.x) * 0.5;
					double disY = disToL * 2.0 * Math.tan(CAMERA_FIELD_OF_VIEW / 2.0) * Math.abs(woL.iC.y - woU.iC.y) * 0.5;
					double dis = Math.sqrt(disX*disX + disY*disY);

					Log.d("Homography", "the distance between the colors is: " + dis + ", disX: " + disX);
					Log.d("Homography", "the position of the beacon is: " + woL.worldPos.x + ", " + woL.worldPos.y);


					if (dis <= 1.2 * BEACON_COLOR_HEIGHT && dis >= 0.83 * BEACON_COLOR_HEIGHT && disX < VERTICAL_THRESHOLD) {
						//upperObjects.remove(woU);
						//lowerObjects.remove(woL);
						listener.onBeaconDetected(woL.worldPos.x, woL.worldPos.y, beaconNum);
						Log.d("Homography", "Beacon detected!");
					}
				} else
					Log.d("Homography", "the lower is above the upper!");
			}
		}

	}

	public static List<WorldObject>[] calculateWorldObjects(List<MatOfPoint>[] contours, WorldPositionCalculator wpcalc, int width, int height) {

		List<WorldObject>[] wObjects = new List[contours.length];

		for (int i = 0; i < contours.length; i++) {
			wObjects[i] = new ArrayList<>();
			for (MatOfPoint c : contours[i]) {
				Point centroid = new Point(0, 0);
				org.opencv.core.Point lowestPoint = new Point(0, 0);
				// get centroid
				for (org.opencv.core.Point p : c.toArray()) {
					centroid.x += p.x;
					centroid.y += p.y;
					if (p.y > lowestPoint.y)
						lowestPoint = p;
				}
				centroid.x /= c.toArray().length;
				centroid.y /= c.toArray().length;
				lowestPoint.x = centroid.x;
				// get the furthest point from the centroid
				double sqDis = 0;
				for (org.opencv.core.Point p : c.toArray()) {
					double sqDisTmp = (centroid.x - p.x) * (centroid.x - p.x) + (centroid.y - p.y) * (centroid.y - p.y);
					if (sqDisTmp > sqDis)
						sqDis = sqDisTmp;
				}
				double radius = Math.sqrt(sqDis);
				org.opencv.core.Point oldPoint = wpcalc.getCoordinatesOfImagePoint(lowestPoint);
				Point newPoint = new Point(oldPoint.x, oldPoint.y);
				wObjects[i].add(new WorldObject(new Point(centroid.x / width - 0.5, centroid.y / height - 0.5), 2.0 * radius / width, newPoint));
			}
		}
		return wObjects;
	}

	public static class WorldObject {
		public Point iC;
		public double iR;
		public Point worldPos;

		public WorldObject(Point iC, double iR, Point worldPos) {
			this.iC = iC;
			this.iR = iR;
			this.worldPos = worldPos;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			WorldObject that = (WorldObject) o;

			if (Double.compare(that.iR, iR) != 0) return false;
			if (iC != null ? !iC.equals(that.iC) : that.iC != null) return false;
			return !(worldPos != null ? !worldPos.equals(that.worldPos) : that.worldPos != null);

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = iC != null ? iC.hashCode() : 0;
			temp = Double.doubleToLongBits(iR);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			result = 31 * result + (worldPos != null ? worldPos.hashCode() : 0);
			return result;
		}
	}
}
