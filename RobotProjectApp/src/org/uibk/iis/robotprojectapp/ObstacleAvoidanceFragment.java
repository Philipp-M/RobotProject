package org.uibk.iis.robotprojectapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class ObstacleAvoidanceFragment extends Fragment implements SimpleSimpleObstacleAvoidance.ChangeEventListener {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	private TextView textLog;
	private boolean isSearching;
	private View rootView;
	private SimpleSimpleObstacleAvoidance simpleObstacleAvoidance;

	public static ObstacleAvoidanceFragment newInstance(int sectionNumber) {
		ObstacleAvoidanceFragment fragment = new ObstacleAvoidanceFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_robot_obstacle_avoidance, container, false);
		textLog = (TextView) rootView.findViewById(R.id.robot_obstacle_avoidance_textLog);
		this.rootView = rootView;
		this.isSearching = false;
		// set all the button onClick methods

		((Button) rootView.findViewById(R.id.robot_obstacle_avoidance_buttonStart)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonStart_onClick(v);
			}
		});

		return rootView;
	}

	public void logText(String text) {
		if (text.length() > 0) {
			textLog.append("[" + text.length() + "] " + text + "\n");
		}
	}

	// move forward
	public void buttonStart_onClick(View v) {
		if (!isSearching) {
			final EditText xEdit = (EditText) rootView.findViewById(R.id.robot_obstacle_avoidance_to_x);
			final EditText yEdit = (EditText) rootView.findViewById(R.id.robot_obstacle_avoidance_to_y);
			final EditText thetaEdit = (EditText) rootView.findViewById(R.id.robot_obstacle_avoidance_to_theta);
			double x;
			double y;
			double theta;
			try {
				x = Double.parseDouble(xEdit.getText().toString());
			} catch (NumberFormatException e) {
				x = 0;
			}
			try {
				y = Double.parseDouble(yEdit.getText().toString());
			} catch (NumberFormatException e) {
				y = 0;
			}
			try {
				theta = Double.parseDouble(thetaEdit.getText().toString());
			} catch (NumberFormatException e) {
				theta = 0;
			}
			simpleObstacleAvoidance = new SimpleSimpleObstacleAvoidance(SimpleSimpleObstacleAvoidance.Dir.RANDOM,
					new OdometryManager.Position(x, y, Math.toRadians(theta)), this);
		}
	}

	@Override
	public void onFoundTarget() {
		isSearching = false;

		Handler mainHandler = new Handler(rootView.getContext().getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				OdometryManager.Position pos = OdometryManager.getInstance().getPosition();
				logText("finished the obstacle avoidance and found the target!");
				logText("new Position: " + "x: " + pos.getX() + " y: " + pos.getY() + " angle: " + Math.toDegrees(pos.getTheta()));
			}
		});

	}
}
