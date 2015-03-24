package org.uibk.iis.robotprojectapp;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class RobotWASDFragment extends Fragment {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	private TextView textLog;
	private ComDriver comDevice;

	public static RobotWASDFragment newInstance(int sectionNumber) {
		RobotWASDFragment fragment = new RobotWASDFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_robot_wasd, container, false);
		textLog = (TextView) rootView.findViewById(R.id.robot_wasd_textLog);
		// set all the button onClick methods

		((Button) rootView.findViewById(R.id.robot_wasd_buttonW)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonW_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonA)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonA_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonS)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonS_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonD)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonD_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonX)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonX_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonUp)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonUp_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonDown)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonDown_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonPlus)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonPlus_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonMinus)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonMinus_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonLedOn)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonLedOn_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonLedOff)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonLedOff_onClick(v);
			}
		});
		((Button) rootView.findViewById(R.id.robot_wasd_buttonSensor)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonSensor_onClick(v);
			}
		});
		return rootView;
	}

	public void initComDevice(ComDriver comDevice) {
		this.comDevice = comDevice;
	}

	public void logText(String text) {
		if (text.length() > 0) {
			textLog.append("[" + text.length() + "] " + text + "\n");
		}
	}

	// move forward
	public void buttonW_onClick(View v) {
		logText("pressed forward");
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 'w', '\r', '\n' }));
	}

	// turn left
	public void buttonA_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 'a', '\r', '\n' }));
	}

	// stop
	public void buttonS_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 's', '\r', '\n' }));
	}

	// turn right
	public void buttonD_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 'd', '\r', '\n' }));
	}

	// move backward
	public void buttonX_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 'x', '\r', '\n' }));
	}

	// lower bar a few degrees
	public void buttonMinus_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { '-', '\r', '\n' }));
	}

	// rise bar a few degrees
	public void buttonPlus_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { '+', '\r', '\n' }));
	}

	// fixed position for bar (low)
	public void buttonDown_onClick(View v) {
		// robotSetBar((byte) 0);
	}

	// fixed position for bar (high)
	public void buttonUp_onClick(View v) {
		// robotSetBar((byte) 255);
	}

	public void buttonLedOn_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 'r', '\r', '\n' }));
		// robotSetLeds((byte) 255, (byte) 128);
	}

	public void buttonLedOff_onClick(View v) {
		if (comDevice.isConnected())
			logText(comDevice.comReadWrite(new byte[] { 'e', '\r', '\n' }));
		// robotSetLeds((byte) 0, (byte) 0);
	}

	public void buttonSensor_onClick(View v) {
		if (comDevice.isConnected()) {
			ArrayList<Byte> sensorData = comDevice.comReadBinWrite(new byte[] { 'p', '\r', '\b' });
			logText("s0: " + (int) sensorData.get(0) + " s1: " + (int) sensorData.get(1) + " s2: " + (int) sensorData.get(2) + " s3: "
					+ (int) (0xFF & sensorData.get(3)) + " s4: " + (int) sensorData.get(4) + " s5: " + (int) sensorData.get(5) + " s6: "
					+ (int) sensorData.get(6) + " s7: " + (int) sensorData.get(7) + "\n");
		}
	}
}
