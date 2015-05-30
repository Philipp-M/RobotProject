package org.uibk.iis.robotprojectapp;

import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

public class BallFinderTask implements BallFinderFragment.BallDetectorListener, RobotMovementManager.ChangeEventListener {

	public enum Dir {
		CLOCKWISE, COUNTERCLOCKWISE, NONE
	}

	private static final double BALL_RADIUS = 6.3;
	private static final double CAMERA_FIELD_OF_VIEW = 1.0155659339644644474;
	private boolean ballCatched;
	private boolean ballDetected;
	boolean ballTracing = false;
	private boolean started;
	private Dir lastDirection;
	private RobotMovementManager rMIns;

	// finish coordinates
	private double xFin;
	private double yFin;
	private double aFin;
	private double xBall;
	private double yBall;

	public BallFinderTask(double xFin, double yFin, double aFin, double xBall, double yBall) {
		lastDirection = Dir.NONE;
		ballCatched = false;
		started = false;
		ballDetected = false;

		this.xFin = xFin;
		this.yFin = yFin;
		this.aFin = aFin;
		this.xBall = xBall;
		this.yBall = yBall;

		rMIns = RobotMovementManager.getInstance();
	}

	public void start() {
		started = true;
	}

	public void stop() {
		started = false;
	}

	public void reset() {
		ballCatched = false;
		started = false;
		ballDetected = false;
		OdometryManager.getInstance().resetPosition();
		lastDirection = Dir.NONE;
	}

	private double calculateBallDistance(double radius) {
		return (BALL_RADIUS / 2.0) / Math.tan(CAMERA_FIELD_OF_VIEW * radius / 2);
	}

	private double calculateBallAngle(double x) {
		return x * 0.5 * CAMERA_FIELD_OF_VIEW;
	}

	private void turnToBall(double x) {
		rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.PIVOT, 0, -calculateBallAngle(x)));
	}

	private void armDown() {
		rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.ARM_DOWN));
	}

	private void armUp() {
		rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.ARM_UP));
	}

	private void driveTowardsBall(double distance) {
		rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.FORWARD, 0, distance * 1.5));
	}

	public void searchBall() {
		if (lastDirection == Dir.COUNTERCLOCKWISE) {
			lastDirection = Dir.CLOCKWISE;
			rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.PIVOT, 0, -2 * Math.PI));
		} else {
			lastDirection = Dir.COUNTERCLOCKWISE;
			rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.PIVOT, 0, 2 * Math.PI));
		}
	}

	@Override
	public void ballDetected() {
		if (started) {
			if (!ballCatched) {
				rMIns.interruptRequestBlocking();
				ballDetected = true;
			}
		}
	}

	@Override
	public void ballLost() {
		if (started) {
			if (!ballCatched) {
				ballDetected = false;
				ballTracing = false;
				//ballDetected = true;
				rMIns.interruptRequestBlocking();
				searchBall();
			}
		}
	}

	@Override
	public void ballChanged(double x, double y, double radius) {
		if (started) {
			if (!ballCatched) {
				double distance = calculateBallDistance(radius);
				if (ballDetected) {
					turnToBall(x);
					//turnToBall(x);
					ballDetected = false;
					ballTracing = true;
				}
				if (!ballDetected && !ballTracing) {
					ballTracing = true;
					turnToBall(x);
					driveTowardsBall(distance);
				}
				// the distance threshold value has to be adjusted, 25-38 seem to be good values
				if (distance <= 32) {
					rMIns.interruptRequestBlocking();
					ballCatched = true;
					ballTracing = false;
					turnToBall(x);
					armDown();
					rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.DRIVE_STRAIGHT_TO, 0, xBall, yBall));
					armUp();
					rMIns.addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.DRIVE_STRAIGHT_TO, 0, xFin, yFin, aFin));

				}
			}
		}
	}

	@Override
	public void onFinishedExecution() {
		if(!ballDetected)
			ballTracing = false;
		if (ballCatched) {
			started = false;
			ballCatched = false;
			ballDetected = false;
			ballTracing = false;
			lastDirection = Dir.NONE;
		}
	}
}
