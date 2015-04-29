package org.uibk.iis.robotprojectapp;

import org.uibk.iis.robotprojectapp.bearing.BearingToNorthProvider;

import android.content.Context;

public class BearingToNorthSingleton {
	private BearingToNorthProvider bearingToNorthProvider;

	// Singleton for easy access, only allowed to use in one Activity
	// it also has to be called in the Activity(Main) thread first
	// the methods start and stop mustn't be called by any other thread but the Main Thread.
	private static final class InstanceHolder {
		static final BearingToNorthSingleton INSTANCE = new BearingToNorthSingleton();
	}

	private BearingToNorthSingleton() {
	}

	public static BearingToNorthSingleton getInstance() {
		return InstanceHolder.INSTANCE;
	}

	// this method has to be called first otherwise the other methods won't work
	public void init(Context context) {
		bearingToNorthProvider = new BearingToNorthProvider(context, 17, 0.5, 50, false);
	}

	public double getBearing() {
		if (bearingToNorthProvider != null)
			return bearingToNorthProvider.getBearing();
		return 0;
	}

	public void start() {
		if (bearingToNorthProvider != null)
			bearingToNorthProvider.start();
	}

	public void stop() {
		if (bearingToNorthProvider != null)
			bearingToNorthProvider.stop();
	}
	// if more methods are needed just implement them like above...

}
