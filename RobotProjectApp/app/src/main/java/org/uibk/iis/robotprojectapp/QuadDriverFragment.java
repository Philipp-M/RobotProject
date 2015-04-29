package org.uibk.iis.robotprojectapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;

public class QuadDriverFragment extends Fragment implements QuadDriverListener {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";
	private Context context;
	private TextView textLog;
	private Thread quadDriverJob;
	private double distancePerEdge;
	private short robotSpeed;
	private float robotSpeedCmL;
	private float robotSpeedCmR;

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static QuadDriverFragment newInstance(int sectionNumber) {
		QuadDriverFragment fragment = new QuadDriverFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.fragment_robot_quad_drive, container, false);
		textLog = (TextView) rootView.findViewById(R.id.robot_quad_drive_textLog);
		context = rootView.getContext();
		// get Robot speed from the preferences
		robotSpeed = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getInt(
				rootView.getContext().getString(R.string.prefRobotSlowVelocity), 18);
		robotSpeedCmL = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
				rootView.getContext().getString(R.string.prefRobotLeftWheelSlow), 8.2f);
		robotSpeedCmR = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
				rootView.getContext().getString(R.string.prefRobotRightWheelSlow), 8.2f);
		distancePerEdge = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
				rootView.getContext().getString(R.string.prefRobotQuadDriveDistance), 20.0f);
		Spinner spinner = (Spinner) rootView.findViewById(R.id.robot_quad_drive_speed_spinner);
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(rootView.getContext(), R.array.robot_quad_drive_speed_array,
				android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if (pos == 0) { // Slow...
					robotSpeed = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getInt(
							rootView.getContext().getString(R.string.prefRobotSlowVelocity), 18);
					robotSpeedCmL = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
							rootView.getContext().getString(R.string.prefRobotLeftWheelSlow), 8.2f);
					robotSpeedCmR = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
							rootView.getContext().getString(R.string.prefRobotRightWheelSlow), 8.2f);
				} else if (pos == 1) { // Medium...
					robotSpeed = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getInt(
							rootView.getContext().getString(R.string.prefRobotMedmVelocity), 32);
					robotSpeedCmL = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
							rootView.getContext().getString(R.string.prefRobotLeftWheelMedm), 14.6f);
					robotSpeedCmR = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
							rootView.getContext().getString(R.string.prefRobotRightWheelMedm), 14.6f);
				} else if (pos == 2) { // Fast...
					robotSpeed = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getInt(
							rootView.getContext().getString(R.string.prefRobotFastVelocity), 55);
					robotSpeedCmL = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
							rootView.getContext().getString(R.string.prefRobotLeftWheelFast), 25.5f);
					robotSpeedCmR = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
							rootView.getContext().getString(R.string.prefRobotRightWheelFast), 25.5f);
				}
				logText("Robot speed was set to: " + robotSpeed + ", left Wheel: " + robotSpeedCmL + "cm/s, right Wheel: " + robotSpeedCmR
						+ "cm/s");
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});
		// setup of the textfield to edit the distance to drive each edge
		final EditText editText = (EditText) rootView.findViewById(R.id.robot_quad_drive_distance_to_drive);
		editText.setText(""
				+ PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
				rootView.getContext().getString(R.string.prefRobotQuadDriveDistance), 20.0f));
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					handled = true;
					distancePerEdge = Double.parseDouble(editText.getText().toString());
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).edit();
					editor.putFloat(rootView.getContext().getString(R.string.prefRobotQuadDriveDistance), (float) distancePerEdge);
					editor.commit();
					logText("set the distance to drive each edge to: " + distancePerEdge + "cm");
				}
				return handled;
			}
		});

		// set all the button onClick methods

		((Button) rootView.findViewById(R.id.robot_quad_drive_buttonStart)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonStartQuadDrive_onClick(v);
			}
		});
		return rootView;
	}

	public void logText(String text) {
		if (text.length() > 0) {
			textLog.append("[" + text.length() + "] " + text + "\n");
		}
	}

	// main loop
	public void buttonStartQuadDrive_onClick(View v) {

		if (ComDriver.getInstance().isConnected()) {
			logText("starting the Quad Drive...");
			quadDriverJob = new Thread(new QuadDriveRunnable(distancePerEdge, robotSpeed, robotSpeedCmL, robotSpeedCmR, this));
			quadDriverJob.start();
		}
	}

	private static class QuadDriveRunnable implements Runnable {
		private short robotSpeed;
		private float robotSpeedCmL;
		private float robotSpeedCmR;
		private double distancePerEdge;
		private QuadDriverListener listener;

		public QuadDriveRunnable(double distancePerEdge, short robotSpeed, float robotSpeedCmL, float robotSpeedCmR,
		                         QuadDriverListener listener) {
			this.robotSpeed = robotSpeed;
			this.robotSpeedCmL = robotSpeedCmL;
			this.robotSpeedCmR = robotSpeedCmR;
			this.listener = listener;
			this.distancePerEdge = distancePerEdge;
		}

		private void driveForward() throws InterruptedException {
			ComDriver cm = ComDriver.getInstance();

			float timeL = (float) (distancePerEdge / (double) robotSpeedCmL);
			float timeR = (float) (distancePerEdge / (double) robotSpeedCmR);
			cm.comReadWrite(new byte[]{'i', (byte) robotSpeed, (byte) ((float) robotSpeed * timeR / timeL), '\r', '\n'});
			Thread.sleep((long) (timeL * 1000.0f));
			cm.comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});

		}

		private void turnRight90Degrees() throws InterruptedException {
			double wheelDistance = Math.PI / 2.0 * CalibrationTask.ROBOT_AXLE_LENGTH;
			float time = (float) wheelDistance / robotSpeedCmL;
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) robotSpeed, (byte) 0, '\r', '\n'});
			Thread.sleep((long) (time * 1000.0f));
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
		}

		@SuppressWarnings("unused")
		private void turnLeft90Degrees() throws InterruptedException {
			double wheelDistance = Math.PI / 2.0 * CalibrationTask.ROBOT_AXLE_LENGTH;
			float time = (float) wheelDistance / robotSpeedCmR;
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) robotSpeed, '\r', '\n'});
			Thread.sleep((long) (time * 1000.0f));
			ComDriver.getInstance().comReadWrite(new byte[]{'i', (byte) 0, (byte) 0, '\r', '\n'});
		}

		@Override
		public void run() {
			ComDriver cm = ComDriver.getInstance();
			if (cm.isConnected()) {
				try {
					// ************* The interesting Stuff is here ;)
					// *************
					listener.onUpdate("driving first edge");
					driveForward();
					listener.onUpdate("turning the first time");
					turnRight90Degrees();
					listener.onUpdate("driving second edge");
					driveForward();
					listener.onUpdate("turning the second time");
					turnRight90Degrees();
					listener.onUpdate("driving third edge");
					driveForward();
					listener.onUpdate("turning the third time");
					turnRight90Degrees();
					listener.onUpdate("driving last edge");
					driveForward();
					listener.onUpdate("turning the last time");
					turnRight90Degrees();
				} catch (InterruptedException e) {
					e.printStackTrace();
					listener.onFinished();
				}
			}
			listener.onFinished();
		}
	}

	@Override
	public void onUpdate(final String message) {
		Handler mainHandler = new Handler(context.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				logText(message);
			}
		});
	}

	@Override
	public void onFinished() {
		Handler mainHandler = new Handler(context.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					quadDriverJob.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
