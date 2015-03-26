package org.uibk.iis.robotprojectapp;

import org.uibk.iis.robotprojectapp.CalibrationTask.Data;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class CalibrationDialog extends ProgressDialog implements CalibrationTask.Callback {
	Context context;
	Thread calibrationThread;

	public CalibrationDialog(Context context) {
		super(context);
		this.context = context;
		setProgress(0);
		setMax(100);
		setProgressStyle(STYLE_HORIZONTAL);
		setProgressNumberFormat(null);
		SharedPreferences robotPref = context.getSharedPreferences(context.getString(R.string.preferences_robot), Context.MODE_PRIVATE);
		calibrationThread = new Thread(new CalibrationTask((Activity) context, this, robotPref.getLong(
				context.getString(R.string.preferences_robot_calibration_time), 8000), robotPref.getInt(
				context.getString(R.string.preferences_robot_slow_velocity), 18), robotPref.getInt(
				context.getString(R.string.preferences_robot_medm_velocity), 32), robotPref.getInt(
				context.getString(R.string.preferences_robot_fast_velocity), 55), CalibrationTask.Type.valueOf(robotPref.getString(
				context.getString(R.string.preferences_robot_calibration_type), CalibrationTask.Type.ALL.name()))));
		calibrationThread.start();
		setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				calibrationThread.interrupt();
				try {
					calibrationThread.join();
				} catch (InterruptedException e) {
				}
				dialog.dismiss();
			}
		});
	}

	@Override
	public void onProgressUpdated(int progress) {
		setProgress(progress);
	}

	@Override
	public void onFinishedCalibration(Data result) {
		SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_robot), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putInt(context.getString(R.string.preferences_robot_slow_velocity), result.SLOW_VAL);
		editor.putInt(context.getString(R.string.preferences_robot_medm_velocity), result.MEDM_VAL);
		editor.putInt(context.getString(R.string.preferences_robot_fast_velocity), result.FAST_VAL);
		editor.putFloat(context.getString(R.string.preferences_robot_left_wheel_slow), (float) result.LEFT_WHEEL_SLOW);
		editor.putFloat(context.getString(R.string.preferences_robot_right_wheel_slow), (float) result.RIGHT_WHEEL_SLOW);
		editor.putFloat(context.getString(R.string.preferences_robot_left_wheel_medm), (float) result.LEFT_WHEEL_MEDM);
		editor.putFloat(context.getString(R.string.preferences_robot_right_wheel_medm), (float) result.RIGHT_WHEEL_MEDM);
		editor.putFloat(context.getString(R.string.preferences_robot_left_wheel_fast), (float) result.LEFT_WHEEL_FAST);
		editor.putFloat(context.getString(R.string.preferences_robot_right_wheel_fast), (float) result.RIGHT_WHEEL_FAST);
		// for debugging...
		Log.d(context.getString(R.string.preferences_robot_slow_velocity), "" + result.SLOW_VAL);
		Log.d(context.getString(R.string.preferences_robot_medm_velocity), "" + result.MEDM_VAL);
		Log.d(context.getString(R.string.preferences_robot_fast_velocity), "" + result.FAST_VAL);
		Log.d(context.getString(R.string.preferences_robot_left_wheel_slow), "" + result.LEFT_WHEEL_SLOW);
		Log.d(context.getString(R.string.preferences_robot_right_wheel_slow), "" + result.RIGHT_WHEEL_SLOW);
		Log.d(context.getString(R.string.preferences_robot_left_wheel_medm), "" + result.LEFT_WHEEL_MEDM);
		Log.d(context.getString(R.string.preferences_robot_right_wheel_medm), "" + result.RIGHT_WHEEL_MEDM);
		Log.d(context.getString(R.string.preferences_robot_left_wheel_fast), "" + result.LEFT_WHEEL_FAST);
		Log.d(context.getString(R.string.preferences_robot_right_wheel_fast), "" + result.RIGHT_WHEEL_FAST);
		editor.commit();
		//success Toast(yummy ;) )
		Toast.makeText(context, "Calibration was a success!", Toast.LENGTH_SHORT).show();
		try {
			calibrationThread.join();
		} catch (InterruptedException e) {
		}
		// close the dialog...
		dismiss();
	}

	@Override
	public void onCanceledSuccessfully(boolean val) {
		if (val == false)
			Toast.makeText(context, "failed while calibrating, please try again!", Toast.LENGTH_LONG).show();
		dismiss();
	}

	@Override
	public void onProgressState(String progressState) {
		setMessage(progressState);
	}
}
