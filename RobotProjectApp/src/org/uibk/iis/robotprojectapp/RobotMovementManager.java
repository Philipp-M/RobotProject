package org.uibk.iis.robotprojectapp;

import java.util.LinkedList;
import java.util.Queue;

public class RobotMovementManager {
	public enum Commands {
		TURN_NON_STOPPING, TURN, PIVOT_NON_STOPPING, PIVOT, FORWARD_NON_STOPPING, FORWARD, DRIVE_STRAIGHT_TO, STOP
	}

	private Thread commandLoopThread;
	private boolean interruptRequest;
	private Queue<Command> commandQueue;

	// private Thread commandAddInterruptThread;

	private static final class InstanceHolder {
		static final RobotMovementManager INSTANCE = new RobotMovementManager();
	}

	private RobotMovementManager() {
		commandQueue = new LinkedList<RobotMovementManager.Command>();
	}

	public static RobotMovementManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	public void start() {
		if (commandLoopThread == null || !commandLoopThread.isAlive()) {
			commandLoopThread = new Thread(new CommandLoop());
			commandLoopThread.start();
		}
	}

	public void stop() {
		if (commandLoopThread != null) {
			commandLoopThread.interrupt();
			try {
				commandLoopThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public boolean isInterrupted() {
		return interruptRequest;
	}

	public synchronized void interruptRequest() {
		interruptRequest = true;
	}

	public synchronized void addCommand(Command command) {
		commandQueue.add(command);
	}

	private class CommandLoop implements Runnable {

		private Thread currentCommand;

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(5);
					synchronized (RobotMovementManager.getInstance()) {
						if (isInterrupted()) {
							currentCommand.interrupt();
							currentCommand.join();
							OdometryManager.getInstance().stop();
							interruptRequest = false;
							currentCommand = null;
						} else if (currentCommand == null && !commandQueue.isEmpty()) {
							currentCommand = new Thread(commandQueue.remove());
							currentCommand.start();
						} else if (currentCommand != null && !currentCommand.isAlive()) {
							currentCommand.join();

							if (commandQueue.isEmpty())
								currentCommand = null;
							else {
								currentCommand = new Thread(commandQueue.remove());
								currentCommand.start();
							}
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}

	}

	public class Command implements Runnable {
		private Commands command;
		private double[] arguments;

		public Command(Commands command, double... arguments) {
			this.command = command;
			this.arguments = arguments;
		}

		@Override
		public void run() {
			OdometryManager om = OdometryManager.getInstance();
			switch (command) {
			case DRIVE_STRAIGHT_TO:
				if (arguments.length >= 4 && arguments[0] == 0)
					om.driveStraightTo(arguments[1], arguments[2], arguments[3], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 4 && arguments[0] == 1)
					om.driveStraightTo(arguments[1], arguments[2], arguments[3], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 4 && arguments[0] == 2)
					om.driveStraightTo(arguments[1], arguments[2], arguments[3], CalibrationTask.Type.FAST);
				break;
			case FORWARD:
				if (arguments.length >= 2 && arguments[0] == 0)
					om.driveForward(arguments[1], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 2 && arguments[0] == 1)
					om.driveForward(arguments[1], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 2 && arguments[0] == 2)
					om.driveForward(arguments[1], CalibrationTask.Type.FAST);
				break;
			case FORWARD_NON_STOPPING:
				if (arguments.length >= 2 && arguments[0] == 0)
					om.driveForwardNonStopping(arguments[1], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 2 && arguments[0] == 1)
					om.driveForwardNonStopping(arguments[1], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 2 && arguments[0] == 2)
					om.driveForwardNonStopping(arguments[1], CalibrationTask.Type.FAST);
				break;
			case PIVOT:
				if (arguments.length >= 2 && arguments[0] == 0)
					om.pivotAngle(arguments[1], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 2 && arguments[0] == 1)
					om.pivotAngle(arguments[1], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 2 && arguments[0] == 2)
					om.pivotAngle(arguments[1], CalibrationTask.Type.FAST);
				break;
			case PIVOT_NON_STOPPING:
				if (arguments.length >= 2 && arguments[0] == 0)
					om.pivotAngleNonStopping(arguments[1], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 2 && arguments[0] == 1)
					om.pivotAngleNonStopping(arguments[1], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 2 && arguments[0] == 2)
					om.pivotAngleNonStopping(arguments[1], CalibrationTask.Type.FAST);
				break;
			case TURN:
				if (arguments.length >= 2 && arguments[0] == 0)
					om.turnAngle(arguments[1], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 2 && arguments[0] == 1)
					om.turnAngle(arguments[1], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 2 && arguments[0] == 2)
					om.turnAngle(arguments[1], CalibrationTask.Type.FAST);
				break;
			case TURN_NON_STOPPING:
				if (arguments.length >= 2 && arguments[0] == 0)
					om.turnAngleNonStopping(arguments[1], CalibrationTask.Type.SLOW);
				else if (arguments.length >= 2 && arguments[0] == 1)
					om.turnAngleNonStopping(arguments[1], CalibrationTask.Type.MEDM);
				else if (arguments.length >= 2 && arguments[0] == 2)
					om.turnAngleNonStopping(arguments[1], CalibrationTask.Type.FAST);
				break;
			case STOP:
				om.stop();
			}
		}
	}
}
