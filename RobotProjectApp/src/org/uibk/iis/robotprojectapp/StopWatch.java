package org.uibk.iis.robotprojectapp;

public class StopWatch {

	private long startTime;
	private long stopTime;
	private boolean stopped;
	public StopWatch() {
		stopped = true;
	}

	public void start() {
		stopped = false;
		startTime = System.currentTimeMillis();
	}

	public void stop() {
		stopped = true;
		stopTime = System.currentTimeMillis();
	}

	public long getTime() {
		if(stopped)
			return stopTime - startTime;
		else
			return System.currentTimeMillis()-startTime;
	}
}