package com.example.cameraexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.SparseIntArray;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.File;
import java.util.Arrays;
import com.example.cameraexample.nativefunction.DsoNdkInterface;
import android.support.v4.app.Fragment;

public class ARFragment extends Fragment implements ActivityCompat.OnRequestPermissionsResultCallback{
  private static final String TAG = "CameraDebug";
  private Button mStart;
  private Button moveLeft;
  private Button moveRight;
  private Button moveUp;
  private Button moveDown;
  private Button moveForward;
  private Button moveBack;
  private Button rotate;
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  private ImageReader mImageReader;
  private String cameraId;
  protected CameraDevice cameraDevice;
  protected CameraCaptureSession captureSession;
  protected CaptureRequest.Builder captureRequestBuilder;
  private SurfaceView imgDealed;
  private ImageReader imageReader;
  private byte[] imageByteArray;
  private File file;
  private static final int INIT_FINISHED = 1;
  private static final int REQUEST_CAMERA_PERMISSION = 200;
  private Handler mBackgroundHandler;
  private HandlerThread mBackgroundThread;
  private File folder;
  private float mMaximalFocalLength;
  private int[] currentFrame;
  private Context mContext;
  private boolean slamStart = false;
  boolean getFrame;
  private Canvas testCanvas;
  private SurfaceHolder mSurfaceHolder;
  byte[] Ydata;
  byte[] Udata;
  byte[] Vdata;
  int[] stride;
  private static final String FRAGMENT_DIALOG = "dialog";
  DrawFrameThread mDrawFrameThread;
  CameraThread mCameraThread;
  private GLRenderer myGLRenderer;
  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.arfragment, container, false);

  }
  public static ARFragment newInstance(){
    return new ARFragment();
  }
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mContext = this.getActivity();

    mImageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
    imgDealed = (SurfaceView) view.findViewById(R.id.texture);

    mSurfaceHolder = imgDealed.getHolder();
    mSurfaceHolder.addCallback(frameCallback);
    mDrawFrameThread = new DrawFrameThread(mSurfaceHolder);
    mCameraThread = new CameraThread(mImageReader, mDrawFrameThread);
    mStart = (Button) view.findViewById(R.id.btn_start);
    moveLeft = (Button) view.findViewById(R.id.btn_left);
    moveRight = (Button) view.findViewById(R.id.btn_right);
    moveUp = (Button) view.findViewById(R.id.btn_up);
    moveDown = (Button) view.findViewById(R.id.btn_down);
    moveForward = (Button) view.findViewById(R.id.btn_forward);
    moveBack = (Button) view.findViewById(R.id.btn_back);
    rotate = (Button) view.findViewById(R.id.btn_rotate);
    getFrame = false;
    stride = new int[3];
    requestCameraPermission();
    imageByteArray = new byte[640 * 480];
    currentFrame = new int[640 * 480];
    Arrays.fill(currentFrame, 0);
    folder = new File(Environment.getExternalStorageDirectory() + File.separator + "calibration");
    if (!folder.exists()) {
      boolean success = folder.mkdir();
      if (!success) {
        Toast.makeText(getActivity().getApplicationContext(), "Cannot create folder", Toast.LENGTH_SHORT).show();
        return;
      }
    }
    setListener();
    GLSurfaceView myGLView = (GLSurfaceView) view.findViewById(R.id.myGLSurfcae);
    myGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    myGLView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    myGLRenderer = new GLRenderer();
    Rect tempRect = mSurfaceHolder.getSurfaceFrame();
    //myGLRenderer.setVidDim(tempRect.width(),tempRect.height());
    myGLView.setRenderer(myGLRenderer);


    //getActivity().addContentView(myGLView, new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
    //    WindowManager.LayoutParams.WRAP_CONTENT));
    myGLView.setZOrderMediaOverlay(true);


    //myHandler.sendEmptyMessage(INIT_FINISHED);
    //drawHandler.sendEmptyMessage(INIT_FINISHED);


  }
  private void requestCameraPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext,Manifest.permission.CAMERA)) {
      new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
    } else {
      requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }
  }
  public static class ConfirmationDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Fragment parent = getParentFragment();
      return new AlertDialog.Builder(getActivity())
          .setMessage(R.string.request_permission)
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                  REQUEST_CAMERA_PERMISSION);
            }
          })
          .setNegativeButton(android.R.string.cancel,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  Activity activity = parent.getActivity();
                  if (activity != null) {
                    activity.finish();
                  }
                }
              })
          .create();
    }
  }
  public void setListener()
  {
    mStart.setOnClickListener(buttonListener);
    moveLeft.setOnClickListener(buttonListener);
    moveRight.setOnClickListener(buttonListener);
    moveUp.setOnClickListener(buttonListener);
    moveDown.setOnClickListener(buttonListener);
    moveForward.setOnClickListener(buttonListener);
    moveBack.setOnClickListener(buttonListener);
    rotate.setOnClickListener(buttonListener);
  }
  public View.OnClickListener buttonListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      switch(v.getId())
      {
        case R.id.btn_start:
          DsoNdkInterface.initSystemWithParameters("/sdcard/calibration/camera.txt");
          mCameraThread.start();
          slamStart = true;
          break;
        case R.id.btn_left:
          DsoNdkInterface.move(1);
          break;
        case R.id.btn_right:
          DsoNdkInterface.move(2);
          break;
        case R.id.btn_up:
          DsoNdkInterface.move(3);
          break;
        case R.id.btn_down:
          DsoNdkInterface.move(4);
          break;
        case R.id.btn_forward:
          DsoNdkInterface.move(5);
          break;
        case R.id.btn_back:
          DsoNdkInterface.move(6);
          break;
        case R.id.btn_rotate:
          DsoNdkInterface.move(7);
          break;

      }
    }
  };


  private SurfaceHolder.Callback frameCallback = new SurfaceHolder.Callback() {
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
          mDrawFrameThread.start();
          //myGLRenderer.setRenderCube(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
  };

  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice camera) {
      // This is called when the camera is open
      Log.e(TAG, "onOpened");
      cameraDevice = camera;
      createCapture();
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
      cameraDevice.close();
    }

    @Override
    public void onError(CameraDevice camera, int error) {
      cameraDevice.close();
      cameraDevice = null;
    }
  };

  protected void startBackgroundThread() {
    mBackgroundThread = new HandlerThread("Camera Background");
    mBackgroundThread.start();
    mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
  }

  protected void stopBackgroundThread() {
    mBackgroundThread.quitSafely();
    try {
      mBackgroundThread.join();
      mBackgroundThread = null;
      mBackgroundHandler = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  protected void createCapture() {
    if (null == cameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }

    try {

      captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

      captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
      captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);
      captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
      captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,new Range<Integer>(60,65));

      if (mMaximalFocalLength > 0) {
        captureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, mMaximalFocalLength);
      }
      captureRequestBuilder.addTarget(mImageReader.getSurface());
      cameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()),
          new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
              // The camera is already closed
              if (null == cameraDevice) {
                return;
              }
              // When the session is ready, we start displaying the preview.
              captureSession = cameraCaptureSession;
              updateCapture();
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
              Toast.makeText(getActivity().getApplicationContext(), "Configuration change", Toast.LENGTH_SHORT).show();
            }
          }, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }

  }
  private void openCamera(int width, int height) {
    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

    Log.e(TAG, "openCamera");
    try {
      cameraId = manager.getCameraIdList()[0];
      // Add permission for camera and let user grant the permission
      if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
          && ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(getActivity(),
            new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE },
            REQUEST_CAMERA_PERMISSION);
        return;
      }
      CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
      float[] focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
      if (focalLengths != null && focalLengths.length > 0) {
        float maxFocalLength = 0;
        for (float focalLength : focalLengths) {
          if (focalLength > maxFocalLength) {
            maxFocalLength = focalLength;
          }
        }

        mMaximalFocalLength = maxFocalLength;
      }
      manager.openCamera(cameraId, stateCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
    Log.e(TAG, "openCamera!!");
  }
  final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      Long expTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
      Long timeStamp  = result.get(CaptureResult.SENSOR_FRAME_DURATION);
      Log.i("TAG",String.format("Exposure Time %d",expTime));
      super.onCaptureCompleted(session, request, result);
      try {
        session.capture(captureRequestBuilder.build(), captureListener, mBackgroundHandler);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }
  };
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        ErrorDialog.newInstance(getString(R.string.request_permission))
            .show(getChildFragmentManager(), FRAGMENT_DIALOG);
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }
  public static class ErrorDialog extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    public static ErrorDialog newInstance(String message) {
      ErrorDialog dialog = new ErrorDialog();
      Bundle args = new Bundle();
      args.putString(ARG_MESSAGE, message);
      dialog.setArguments(args);
      return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Activity activity = getActivity();
      return new AlertDialog.Builder(activity)
          .setMessage(getArguments().getString(ARG_MESSAGE))
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              activity.finish();
            }
          })
          .create();
    }

  }
  protected void updateCapture() {
    if (null == cameraDevice) {
      Log.e(TAG, "updatePreview error, return");
    }
    try {
      captureSession.setRepeatingRequest(captureRequestBuilder.build(), captureListener, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  private void closeCamera() {
    if (null != cameraDevice) {
      cameraDevice.close();
      cameraDevice = null;
    }
    if (null != imageReader) {
      imageReader.close();
      imageReader = null;
    }
  }
  @Override
  public void onResume() {

    Log.e(TAG, "onResume");

    startBackgroundThread();
    openCamera(640, 480);
    super.onResume();
  }
  @Override
  public void onPause() {
    Log.e(TAG, "onPause");
     closeCamera();
    stopBackgroundThread();
    super.onPause();
  }

  @Override
  public void onDestroy() {

    if(slamStart)
    {
      closeCamera();
      DsoNdkInterface.stopSLAM();
    }
    super.onDestroy();
  }
}
