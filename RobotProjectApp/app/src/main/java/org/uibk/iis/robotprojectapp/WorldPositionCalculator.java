package org.uibk.iis.robotprojectapp;

import org.opencv.core.Point;

public interface WorldPositionCalculator {
	Point getCoordinatesOfImagePoint(Point point);
}
