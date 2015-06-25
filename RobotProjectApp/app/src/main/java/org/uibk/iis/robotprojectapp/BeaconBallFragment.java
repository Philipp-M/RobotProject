package org.uibk.iis.robotprojectapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

public class BeaconBallFragment extends Fragment implements RobotMovementManager.ChangeEventListener, BeaconDetector.BeaconDetectorListener, BeaconLocalizer.BeaconLocalizerListener, View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2, WorldPositionCalculator {


	public interface BallDetectorListener {
		void ballDetected(BeaconDetector.WorldObject worldObject);

		void ballNearEnough(BeaconDetector.WorldObject worldObject);
	}

	private static final String TAG = "OCVSample::Activity";
	public static final int COLOR_COUNT = 4;
	public static final int BEACON_COUNT = 4;
	public static final int BALL_COLOR = 3;

	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private Mat homographyMat = null;
	private boolean homographyCalibrated = false;
	private boolean homographyToCalibrate = false;
	private boolean colorsToCalibrate = false;
	private int curColorToCalibrate = 0;
	private Scalar[] mBlobColorRgba;
	private Scalar[] mBlobColorHsv;
	private ColorBlobDetector[] mDetectors;

	private Mat[] mSpectrum;
	private Size SPECTRUM_SIZE;
	private Scalar[] CONTOUR_COLOR;

	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback;
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	private boolean ballDetected = false;
	private boolean transportingBall = false;
	private boolean odometryUpdated = false;
	private boolean detectStopped = false;
	private boolean turnToBall = false;
	private boolean gotBall = false;
	private int detectedBeacon;
	private View rootView;
	private Activity activity;
	private BeaconDetector[] bDs;
	private BeaconLocalizer bL;
	private boolean[] beaconsDetected;
	private boolean started;
	private BallFinderTask2 bFTask;

	public BeaconBallFragment() {
	}

	@SuppressLint("ValidFragment")
	public BeaconBallFragment(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static BeaconBallFragment newInstance(int sectionNumber, Activity activity) {
		BeaconBallFragment fragment = new BeaconBallFragment(activity);
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onBeaconDetected(double x, double y, int beaconNum) {
		double angle1 = Math.atan2(x, y);
		Log.d("bla1", "" + angle1);
		if (started && !beaconsDetected[beaconNum] && detectedBeacon < 2) {
			if (detectedBeacon == 1 && !detectStopped && !RobotMovementManager.getInstance().isInterrupted()) {
				Log.d("BeaconLocal", "2nd beacon detect start stopping!");
				RobotMovementManager.getInstance().interruptRequest();
			} else if (detectedBeacon == 1 && detectStopped) {
				/*if(RobotMovementManager.getInstance().isWorking()) {
					Log.d("blabla", "still working...");
					return;
				}
				double angle = Math.atan2(x,y);
				Log.d("bla", "" + angle);
				if(angle < Math.toRadians(-5)) {
					RobotMovementManager.getInstance().addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.PIVOT, 0, Math.toRadians(50)));
					return;
				} else if(angle > Math.toRadians(5)) {
					RobotMovementManager.getInstance().addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.PIVOT, 0, Math.toRadians(-50)));
					return;
				}*/
				Log.d("BeaconLocal", "2nd beacon detected start calibrating!");
				bL.setBeacon(beaconNum, new org.uibk.iis.robotprojectapp.Point(x, y));
				detectedBeacon++;
				beaconsDetected[beaconNum] = true;
			} else if (!beaconsDetected[beaconNum]) {
				Log.d("BeaconLocal", "1st beacon detected start calibrating!");
				bL.setBeacon(beaconNum, new org.uibk.iis.robotprojectapp.Point(x, y));
				detectedBeacon++;
				beaconsDetected[beaconNum] = true;
			}

		}
	}

	@Override
	public void onFinishedExecution() {
		//dirty
		if (!detectStopped) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			detectStopped = true;
		}
		if (transportingBall) {
			bFTask.armUp();
			transportingBall = false;
			reset();
			buttonStart_onClick(null);
		}
	}

	@Override
	public void onLocalized(org.uibk.iis.robotprojectapp.Point p, final double angle) {
		final org.uibk.iis.robotprojectapp.Point pt = new org.uibk.iis.robotprojectapp.Point(p.x, p.y);
		Log.d("BeaconLocal", "the calculated Position is: " + pt.x + ", " + pt.y + ", ang: " + angle);
		OdometryManager.getInstance().updateOdometry(pt.x, pt.y, angle);
		RobotMovementManager.getInstance().addCommand(new RobotMovementManager.Command(RobotMovementManager.Commands.DRIVE_STRAIGHT_TO, 0, 0, 0, 0));
		odometryUpdated = true;
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(activity, "the calculated Position is: " + pt.x + ", " + pt.y + ", ang: " + Math.toDegrees(angle), Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_robot_beacon_ball, container, false);
		// textLog = (TextView) rootView.findV                <action android:name="android.intent.action.MAIN" />iewById(R.id.robot_obstacle_avoidance_textLog);
		this.rootView = rootView;
		this.ballDetected = false;

		rootView.findViewById(R.id.robot_beacon_ball_buttonStart).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d("Ballfinder: ", "started the Ball finder!");
				buttonStart_onClick(v);
			}
		});

		rootView.findViewById(R.id.robot_beacon_ball_buttonReset).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				RobotMovementManager.getInstance().interruptRequestBlocking();
				reset();
			}
		});
		rootView.findViewById(R.id.robot_beacon_ball_buttonCalibrateHomography).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				buttonCalibrateHomography_onClick(v);
			}
		});

		rootView.findViewById(R.id.robot_beacon_ball_buttonCalibrateColors).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				buttonCalibrateColors_onClick(v);
			}
		});
		mLoaderCallback = new BaseLoaderCallback(rootView.getContext()) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
					case LoaderCallbackInterface.SUCCESS: {
						Log.i(TAG, "OpenCV loaded successfully");
						mOpenCvCameraView.enableView();
						mOpenCvCameraView.setOnTouchListener(BeaconBallFragment.this);
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

	public void reset() {
		bL = new BeaconLocalizer(BeaconBallFragment.this);
		detectStopped = false;
		detectedBeacon = 0;
		started = false;
		for (int i = 0; i < BEACON_COUNT; i++)
			beaconsDetected[i] = false;
	}

	public void buttonCalibrateHomography_onClick(View v) {
		homographyToCalibrate = true;
	}

	public void buttonCalibrateColors_onClick(View v) {
		colorsToCalibrate = true;
		curColorToCalibrate = 0;
	}

	public void buttonStart_onClick(View v) {
		final EditText xEdit = (EditText) rootView.findViewById(R.id.robot_beacon_ball_to_x);
		final EditText yEdit = (EditText) rootView.findViewById(R.id.robot_beacon_ball_to_y);
		final EditText thetaEdit = (EditText) rootView.findViewById(R.id.robot_beacon_ball_to_theta);
		final EditText xBallEdit = (EditText) rootView.findViewById(R.id.robot_beacon_ball_ball_to_x);
		final EditText yBallEdit = (EditText) rootView.findViewById(R.id.robot_beacon_ball_ball_to_y);
		double x;
		double y;
		double theta;
		double xBall;
		double yBall;
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
		try {
			xBall = Double.parseDouble(xBallEdit.getText().toString());
		} catch (NumberFormatException e) {
			xBall = 0;
		}
		try {
			yBall = Double.parseDouble(yBallEdit.getText().toString());
		} catch (NumberFormatException e) {
			yBall = 0;
		}
		if (bFTask != null)
			bFTask.reset();
		bFTask = new BallFinderTask2(x, y, theta, xBall, yBall);
		started = true;
		ballDetected = false;
		odometryUpdated = false;
		bFTask.searchBall();
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
		started = false;
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		// COLOR_COUNT different colors
		mDetectors = new ColorBlobDetector[COLOR_COUNT];
		for (int i = 0; i < COLOR_COUNT; i++)
			mDetectors[i] = new ColorBlobDetector();
		mSpectrum = new Mat[COLOR_COUNT];
		mBlobColorRgba = new Scalar[COLOR_COUNT];
		mBlobColorHsv = new Scalar[COLOR_COUNT];
		for (int i = 0; i < COLOR_COUNT; i++) {
			mSpectrum[i] = new Mat();
			mBlobColorRgba[i] = new Scalar(255);
			mBlobColorHsv[i] = new Scalar(255);
		}
		SPECTRUM_SIZE = new Size(200, 20);
		CONTOUR_COLOR = new Scalar[COLOR_COUNT];
		CONTOUR_COLOR[0] = new Scalar(0, 0, 255, 255);
		CONTOUR_COLOR[1] = new Scalar(255, 255, 0, 255);
		CONTOUR_COLOR[2] = new Scalar(255, 0, 0, 255);
		CONTOUR_COLOR[3] = new Scalar(255, 255, 255, 255);
		bDs = new BeaconDetector[BEACON_COUNT];
		bDs[0] = new BeaconDetector(0, 1, this, 0);
		bDs[1] = new BeaconDetector(1, 2, this, 1);
		bDs[2] = new BeaconDetector(2, 1, this, 2);
		bDs[3] = new BeaconDetector(1, 0, this, 3);
//		bDs[0] = new BeaconDetector(0, 1, this, 0);
//		bDs[1] = new BeaconDetector(2, 3, this, 1);
//		bDs[2] = new BeaconDetector(0, 2, this, 2);
//		bDs[3] = new BeaconDetector(2, 1, this, 3);
//		bDs[4] = new BeaconDetector(1, 0, this, 4);
//		bDs[5] = new BeaconDetector(3, 2, this, 5);
//		bDs[6] = new BeaconDetector(2, 0, this, 6);
//		bDs[7] = new BeaconDetector(1, 3, this, 7);
		bL = new BeaconLocalizer(this);
		beaconsDetected = new boolean[BEACON_COUNT];
		for (int i = 0; i < BEACON_COUNT; i++)
			beaconsDetected[i] = false;
		RobotMovementManager.getInstance().registerListener(this);
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (colorsToCalibrate) {
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
			mBlobColorHsv[curColorToCalibrate] = Core.sumElems(touchedRegionHsv);
			int pointCount = touchedRect.width * touchedRect.height;
			for (int i = 0; i < mBlobColorHsv[curColorToCalibrate].val.length; i++)
				mBlobColorHsv[curColorToCalibrate].val[i] /= pointCount;

			mBlobColorRgba[curColorToCalibrate] = converScalarHsv2Rgba(mBlobColorHsv[curColorToCalibrate]);

			Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba[curColorToCalibrate].val[0] + ", " + mBlobColorRgba[curColorToCalibrate].val[1] +
					", " + mBlobColorRgba[curColorToCalibrate].val[2] + ", " + mBlobColorRgba[curColorToCalibrate].val[3] + ")");

			mDetectors[curColorToCalibrate].setHsvColor(mBlobColorHsv[curColorToCalibrate]);

			Imgproc.resize(mDetectors[curColorToCalibrate].getSpectrum(), mSpectrum[curColorToCalibrate], SPECTRUM_SIZE);

			mIsColorSelected = true;

			touchedRegionRgba.release();
			touchedRegionHsv.release();
			curColorToCalibrate++;
			if (curColorToCalibrate >= COLOR_COUNT) {
				colorsToCalibrate = false;
				curColorToCalibrate = 0;
			}
		}
		return false; // don't need subsequent touch events
	}

	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		if (homographyToCalibrate)
			getHomographyMatrix(mRgba);
		if (mIsColorSelected) {
			List<MatOfPoint>[] contours = new List[COLOR_COUNT];
			for (int i = 0; i < COLOR_COUNT; i++) {
				mDetectors[i].process(mRgba);
				contours[i] = mDetectors[i].getContours();
				Imgproc.drawContours(mRgba, contours[i], -1, CONTOUR_COLOR[i]);
			}
			List<BeaconDetector.WorldObject>[] woList = BeaconDetector.calculateWorldObjects(contours, this, mRgba.width(), mRgba.height());
			for (int i = 0; i < BEACON_COUNT; i++)
				bDs[i].detectBeacon(woList);
			if (odometryUpdated && false) {
				for (BeaconDetector.WorldObject wo : woList[BALL_COLOR]) {
					double radiusCalculated = wo.worldPos.length() * 2.0 * Math.tan(BeaconDetector.CAMERA_FIELD_OF_VIEW / 2) * wo.iR * 0.5;
					if (!ballDetected) {
						if (radiusCalculated <= BallFinderTask2.BALL_RADIUS * 1.4 && radiusCalculated >= BallFinderTask2.BALL_RADIUS * 0.7) {
							ballDetected = true;
							bFTask.ballDetected(wo);
						}
					}
					if ((wo.worldPos.length() < 17 && radiusCalculated <= BallFinderTask2.BALL_RADIUS * 1.4 && radiusCalculated >= BallFinderTask2.BALL_RADIUS * 0.7)) {
						bFTask.ballNearEnough(wo);
						transportingBall = true;
					}
				}
			}
			for (int i = 0; i < COLOR_COUNT; i++) {
				Mat colorLabel = mRgba.submat(4 + 24 * i, (i + 1) * 24, 4, 24);
				colorLabel.setTo(mBlobColorRgba[i]);
				Mat spectrumLabel = mRgba.submat(4 + i * (4 + mSpectrum[i].rows()), (i + 1) * (4 + mSpectrum[i].rows()), 32, 32 + mSpectrum[i].cols());
				mSpectrum[i].copyTo(spectrumLabel);
			}
		}

		return mRgba;
	}

	public void getHomographyMatrix(Mat mRgba) {
		final Size mPatternSize = new Size(6, 9); // number of inner corners in the used chessboard pattern
		float x = -48.0f; // coordinates of first detected inner corner on chessboard
		float y = 309.0f;
		float delta = 12.0f; // size of a single square edge in chessboard
		LinkedList<Point> PointList = new LinkedList<Point>();

		// Define real-world coordinates for given chessboard pattern:
		for (int i = 0; i < mPatternSize.height; i++) {
			y = 309.0f;
			for (int j = 0; j < mPatternSize.width; j++) {
				PointList.addLast(new Point(x, y));
				y += delta;
			}
			x += delta;
		}
		MatOfPoint2f RealWorldC = new MatOfPoint2f();
		RealWorldC.fromList(PointList);

		// Detect inner corners of chessboard pattern from image:
		Mat gray = new Mat();
		Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2GRAY); // convert image to grayscale
		MatOfPoint2f mCorners = new MatOfPoint2f();
		boolean mPatternWasFound = Calib3d.findChessboardCorners(gray, mPatternSize, mCorners);

		// Calculate homography:
		if (mPatternWasFound) {
			Calib3d.drawChessboardCorners(mRgba, mPatternSize, mCorners, mPatternWasFound); //for visualization
			homographyMat = Calib3d.findHomography(mCorners, RealWorldC);
			Log.d("Homography", "Calibrated!");
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity, "successfully created homography matrix!", Toast.LENGTH_LONG).show();
				}
			});
			homographyToCalibrate = false;
			homographyCalibrated = true;
		} else {
			Log.d("Homography", "not Calibrated!");
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity, "failed to create a homography matrix!", Toast.LENGTH_SHORT).show();
				}
			});
			homographyToCalibrate = false;
		}
	}

	@Override
	public Point getCoordinatesOfImagePoint(Point point) {
		Mat src = new Mat(1, 1, CvType.CV_32FC2);
		Mat dest = new Mat(1, 1, CvType.CV_32FC2);
		src.put(0, 0, new double[]{point.x, point.y}); // ps is a point in image coordinates
		if (homographyMat != null)
			Core.perspectiveTransform(src, dest, homographyMat); //homography is your homography matrix
		Point pMil = new Point(dest.get(0, 0)[0], dest.get(0, 0)[1]);
		pMil.x /= 10.0;
		pMil.y /= 10.0;
		return pMil;
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

		return new Scalar(pointMatRgba.get(0, 0));
	}
}
