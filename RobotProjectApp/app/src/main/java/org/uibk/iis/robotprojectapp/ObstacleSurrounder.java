package org.uibk.iis.robotprojectapp;

public class ObstacleSurrounder {

	public interface PositionListener {
		void onPositionUpdated(OdometryManager.Position pos);

	}

	public enum Dir {
		LEFT, RIGHT
	}

	private Dir direction;
	private int distanceToObject;

	public ObstacleSurrounder(Dir direction, int distanceToObject) {
		this.direction = direction;
		this.distanceToObject = distanceToObject;
	}

}
