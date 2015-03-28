package org.uibk.iis.robotprojectapp;

import org.uibk.iis.robotprojectapp.CalibrationTask.Data;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
		SharedPreferences robotPref = PreferenceManager.getDefaultSharedPreferences(context);
		calibrationThread = new Thread(new CalibrationTask((Activity) context, this, (long)robotPref.getInt(
				context.getString(R.string.prefRobotCalibrationTime), 8000), robotPref.getInt(
				context.getString(R.string.prefRobotSlowVelocity), 18), robotPref.getInt(
				context.getString(R.string.prefRobotMedmVelocity), 32), robotPref.getInt(
				context.getString(R.string.prefRobotFastVelocity), 55), CalibrationTask.Type.valueOf(robotPref.getString(
				context.getString(R.string.prefRobotCalibrationType), CalibrationTask.Type.ALL.name()))));
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
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putInt(context.getString(R.string.prefRobotSlowVelocity), result.SLOW_VAL);
		editor.putInt(context.getString(R.string.prefRobotMedmVelocity), result.MEDM_VAL);
		editor.putInt(context.getString(R.string.prefRobotFastVelocity), result.FAST_VAL);
		if (result.LEFT_WHEEL_SLOW != 0)
			editor.putFloat(context.getString(R.string.prefRobotLeftWheelSlow), (float) result.LEFT_WHEEL_SLOW);
		if (result.RIGHT_WHEEL_SLOW != 0)
			editor.putFloat(context.getString(R.string.prefRobotRightWheelSlow), (float) result.RIGHT_WHEEL_SLOW);
		if (result.LEFT_WHEEL_MEDM != 0)
			editor.putFloat(context.getString(R.string.prefRobotLeftWheelMedm), (float) result.LEFT_WHEEL_MEDM);
		if (result.RIGHT_WHEEL_MEDM != 0)
			editor.putFloat(context.getString(R.string.prefRobotRightWheelMedm), (float) result.RIGHT_WHEEL_MEDM);
		if (result.LEFT_WHEEL_FAST != 0)
			editor.putFloat(context.getString(R.string.prefRobotLeftWheelFast), (float) result.LEFT_WHEEL_FAST);
		if (result.RIGHT_WHEEL_FAST != 0)
			editor.putFloat(context.getString(R.string.prefRobotRightWheelFast), (float) result.RIGHT_WHEEL_FAST);
		editor.commit();
		// success Toast(yummy ;) )
		Toast.makeText(context, "Calibration succeeded!", Toast.LENGTH_SHORT).show();
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
