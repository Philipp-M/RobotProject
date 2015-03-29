package org.uibk.iis.robotprojectapp;

import android.content.SharedPreferences;
import android.os.Bundle;
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

public class QuadDriverFragment extends Fragment {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	private TextView textLog;
	private double distancePerEdge;
	private short robotSpeed;
	private float robotSpeedCmL;
	private float robotSpeedCmR;

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
		// get Robot speed from the preferences
		robotSpeed = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getInt(
				rootView.getContext().getString(R.string.prefRobotSlowVelocity), 18);
		robotSpeedCmL = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
				rootView.getContext().getString(R.string.prefRobotLeftWheelSlow), 6.0f);
		robotSpeedCmR = (short) PreferenceManager.getDefaultSharedPreferences(rootView.getContext()).getFloat(
				rootView.getContext().getString(R.string.prefRobotRightWheelSlow), 6.0f);
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
			//logText(ComDriver.getInstance().comReadWrite(new byte[] { 'w', '\r', '\n' }));
		}
	}
}
