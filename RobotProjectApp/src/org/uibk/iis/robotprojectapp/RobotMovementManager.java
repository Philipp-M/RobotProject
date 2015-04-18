package org.uibk.iis.robotprojectapp;

import java.util.LinkedList;
import java.util.Queue;

public class RobotMovementManager {
	/**
	 * Command identifiers
	 *
	 * Following describes a list of commands in the following order:
	 *
	 * COMMAND ARGUMENT1(comment) ARGUMENT2 ... #optionally Comment
	 **************************************************************
	 * Currently following Commands are supported:
	 **************************************************************
	 * TURN_NON_STOPPING SPEED(0, 1 or 2) ANGLE(in radian)
	 *
	 * TURN SPEED(0, 1 or 2) ANGLE(in radian)
	 *
	 * PIVOT_NON_STOPPING SPEED(0, 1 or 2) ANGLE(in radian)
	 *
	 * PIVOT SPEED(0, 1 or 2) ANGLE(in radian)
	 *
	 * FORWARD SPEED(0, 1 or 2) DISTANCE(in centimeters)
	 *
	 * FORWARD_NON_STOPPING SPEED(0, 1 or 2) DISTANCE(in centimeters)
	 *
	 * DRIVE_STRAIGHT_TO SPEED(0, 1 or 2) X(absolute coordinates in cm) Y(absolute coordinates in cm) ANGLE(in radian)
	 *
	 * STOP
	 **************************************************************
	 */
	public enum Commands {
		TURN_NON_STOPPING, TURN, PIVOT_NON_STOPPING, PIVOT, FORWARD_NON_STOPPING, FORWARD, DRIVE_STRAIGHT_TO, STOP
	}

	private Thread commandLoopThread;
	private boolean interruptRequest;
	private Queue<Command> commandQueue;

	/**** Singleton stuff ****/
	private static final class InstanceHolder {
		static final RobotMovementManager INSTANCE = new RobotMovementManager();
	}

	private RobotMovementManager() {
		commandQueue = new LinkedList<RobotMovementManager.Command>();
	}

	public static RobotMovementManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	/**
	 * starts the thread, that handles all of the commands that are committed to the robot
	 */
	public void start() {
		if (commandLoopThread == null || !commandLoopThread.isAlive()) {
			commandLoopThread = new Thread(new CommandLoop());
			commandLoopThread.start();
		}
	}

	/**
	 * stops the thread
	 */
	public void stop() {
		if (commandLoopThread != null) {
			commandLoopThread.interrupt();
			try {
				commandLoopThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * checks if an interrupt request was sent
	 *
	 * @return true if it was interrupted, false otherwise
	 */
	public boolean isInterrupted() {
		return interruptRequest;
	}

	/**
	 * sends an interrupt request
	 */
	public synchronized void interruptRequest() {
		interruptRequest = true;
	}

	/**
	 * sends an interrupt request and waits until it was handled
	 */
	public synchronized void interruptRequestBlocking() {
		interruptRequest();
		synchronized (commandLoopThread) {
			try {
				commandLoopThread.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * adds a command to the queue
	 *
	 * @param command
	 *            the command to be added
	 */
	public synchronized void addCommand(Command command) {
		commandQueue.add(command);
	}

	/**
	 * The Command looper thread
	 *
	 * handles all of the commands and the interrupt requests see also
	 *
	 * @RobotMovementManger.Commands
	 *
	 */
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
							notifyAll();
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

	/**
	 * Command
	 *
	 * is controlled with an enum(RobotMovementManager.Commands) of command identifiers optionally some Commands need some arguments (until
	 * now only numeric values are required so double was chosen as argument type
	 *
	 */
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
