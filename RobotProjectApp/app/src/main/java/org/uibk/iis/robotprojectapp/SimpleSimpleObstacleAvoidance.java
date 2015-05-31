package org.uibk.iis.robotprojectapp;

import org.uibk.iis.robotprojectapp.OdometryManager.Position;
import org.uibk.iis.robotprojectapp.RobotMovementManager.Command;
import org.uibk.iis.robotprojectapp.RobotMovementManager.Commands;

import android.util.Log;

public class SimpleSimpleObstacleAvoidance implements DistanceMeasurementProvider.ChangeEventListener,
		RobotMovementManager.ChangeEventListener {

	public interface ChangeEventListener {
		void onFoundTarget();
	}

	public enum Dir {
		CLOCKWISE, COUNTERCLOCKWISE, RANDOM
	}

	;

	private Dir direction;
	private OdometryManager.Position target;
	private ChangeEventListener changeEventListener;
	private boolean isInterrupted;

	public SimpleSimpleObstacleAvoidance(Dir direction, OdometryManager.Position target, ChangeEventListener changeEventListener) {
		this.direction = direction;
		this.target = target;
		this.isInterrupted = false;
		DistanceMeasurementProvider.getInstance().registerListener(this, (short) 1, (short) 07, 1300);
		RobotMovementManager.getInstance().registerListener(this);
		this.changeEventListener = changeEventListener;
		RobotMovementManager.getInstance().addCommand(
				new RobotMovementManager.Command(RobotMovementManager.Commands.DRIVE_STRAIGHT_TO, 0, target.getX(), target.getY(), target
						.getTheta()));
	}

	@Override
	public void onDistanceChanged(short left, short center, short right) {
	}

	@Override
	public void onDistanceBelowThreshold(short left, short center, short right) {
		isInterrupted = true;
		RobotMovementManager.getInstance().interruptRequestBlocking();
		synchronized (RobotMovementManager.getInstance()) {
			RobotMovementManager.getInstance().addCommand(new Command(Commands.BACKWARDS_NON_STOPPING, 0, 10));
			RobotMovementManager.getInstance().addCommand(
					new Command(Commands.PIVOT_NON_STOPPING, 0, ((direction == Dir.CLOCKWISE) ? 1 : (direction == Dir.RANDOM) ? (Math
							.random() < 0.5 ? -1 : 1) : -1) * Math.toRadians(90), 1));
			RobotMovementManager.getInstance().addCommand(new Command(Commands.FORWARD, 0, 30));
		}
	}

	@Override
	public void onFinishedExecution() {
		synchronized (RobotMovementManager.getInstance()) {
			if (OdometryManager.getInstance().getPosition().equals(target)) {
				if (changeEventListener != null)
					changeEventListener.onFoundTarget();
				DistanceMeasurementProvider.getInstance().unregisterListener(this);
				RobotMovementManager.getInstance().unregisterListener(this);

			} else {
				if (isInterrupted)
					isInterrupted = false;
				else
					RobotMovementManager.getInstance().addCommand(
							new Command(Commands.DRIVE_STRAIGHT_TO, 0, target.getX(), target.getY(), target.getTheta()));

			}
		}
	}
}
