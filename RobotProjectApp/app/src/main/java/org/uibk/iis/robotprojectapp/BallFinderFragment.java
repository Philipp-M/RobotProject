package org.uibk.iis.robotprojectapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class BallFinderFragment extends Fragment implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";

	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private Scalar mBlobColorRgba;
	private Scalar mBlobColorHsv;
	private ColorBlobDetector mDetector;
	private Mat mSpectrum;
	private Size SPECTRUM_SIZE;
	private Scalar CONTOUR_COLOR;

	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback;
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

	public static BallFinderFragment newInstance(int sectionNumber) {
		BallFinderFragment fragment = new BallFinderFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_robot_ball_finder, container, false);
		// textLog = (TextView) rootView.findV                <action android:name="android.intent.action.MAIN" />iewById(R.id.robot_obstacle_avoidance_textLog);
		this.rootView = rootView;
		this.isSearching = false;
		mLoaderCallback = new BaseLoaderCallback(rootView.getContext()) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
					case LoaderCallbackInterface.SUCCESS: {
						Log.i(TAG, "OpenCV loaded successfully");
						mOpenCvCameraView.enableView();
						mOpenCvCameraView.setOnTouchListener(BallFinderFragment.this);
					}
					break;
					default: {
						super.onManagerConnected(status);
					}
					break;
				}
			}
		};
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		//setContentView(R.layout.color_blob_detection_surface_view);

		mOpenCvCameraView = (CameraBridgeViewBase) rootView.findViewById(R.id.ball_finder_detection_camera_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
		//mOpenCvCameraView.setMaxFrameSize(1920, 1080);
		mOpenCvCameraView.enableFpsMeter();
		return rootView;
	}


	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (rootView != null)
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, rootView.getContext(), mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mDetector = new ColorBlobDetector();
		mSpectrum = new Mat();
		mBlobColorRgba = new Scalar(255);
		mBlobColorHsv = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	public boolean onTouch(View v, MotionEvent event) {
		int cols = mRgba.cols();
		int rows = mRgba.rows();

		int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
		int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

		int x = (int) event.getX() - xOffset;
		int y = (int) event.getY() - yOffset;

		Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

		if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

		Rect touchedRect = new Rect();

		touchedRect.x = (x > 4) ? x - 4 : 0;
		touchedRect.y = (y > 4) ? y - 4 : 0;

		touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
		touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

		Mat touchedRegionRgba = mRgba.submat(touchedRect);

		Mat touchedRegionHsv = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

		// Calculate average color of touched region
		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width * touchedRect.height;
		for (int i = 0; i < mBlobColorHsv.val.length; i++)
			mBlobColorHsv.val[i] /= pointCount;

		mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

		Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
				", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

		mDetector.setHsvColor(mBlobColorHsv);

		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

		mIsColorSelected = true;

		touchedRegionRgba.release();
		touchedRegionHsv.release();

		return false; // don't need subsequent touch events
	}

	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();

		if (mIsColorSelected) {
			mDetector.process(mRgba);
			List<MatOfPoint> contours = mDetector.getContours();
			Log.e(TAG, "Contours count: " + contours.size());
			Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
			if (contours.size() == 1) {
				Point centroid = new Point(0, 0);
				// get centroid
				for (Point p : contours.get(0).toArray()) {
					centroid.x += p.x;
					centroid.y += p.y;
				}
				centroid.x /= contours.get(0).toArray().length;
				centroid.y /= contours.get(0).toArray().length;
				// get the furthest point from the centroid
				double sqDis = 0;
				for (Point p : contours.get(0).toArray()) {
					double sqDisTmp = (centroid.x - p.x) * (centroid.x - p.x) + (centroid.y - p.y) * (centroid.y - p.y);
					if (sqDisTmp > sqDis)
						sqDis = sqDisTmp;
				}
				Core.circle(mRgba, centroid, (int) Math.sqrt(sqDis), CONTOUR_COLOR);
			}
			Mat colorLabel = mRgba.submat(4, 68, 4, 68);
			colorLabel.setTo(mBlobColorRgba);

			Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
			mSpectrum.copyTo(spectrumLabel);
		}

		return mRgba;
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

		return new Scalar(pointMatRgba.get(0, 0));
	}

	public void logText(String text) {
		if (text.length() > 0) {
			textLog.append("[" + text.length() + "] " + text + "\n");
		}
	}
}

