package com.example.cameraexample;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import com.example.cameraexample.nativefunction.DsoNdkInterface;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "CameraDebug";
  private Button mStart;
  private Button moveLeft;
  private Button moveRight;
  private Button moveUp;
  private Button moveDown;
  private Button moveForward;
  private Button moveBack;
  private Button rotate;
  private TextureView textureView;
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  private int imageNumber;
  private int recordImageNumber;
  private ImageReader mImageReader;
  // load dso_slam library
  static {
    System.loadLibrary("DSO_SLAM");
  }
  private String test;
  private String cameraId;
  protected CameraDevice cameraDevice;
  protected CameraCaptureSession cameraCaptureSessions;
  protected CameraCaptureSession recordCaptureSessions;
  protected CaptureRequest captureRequest;
  protected CaptureRequest.Builder captureRequestBuilder;
  protected CaptureRequest.Builder recordRequestBuilder;
  private Size imageDimension;
  ImageView imgDealed;
  private ImageReader imageReader;
  Bitmap currentImage;
  private byte[] imageByteArray;
  private File file;
  private static final int INIT_FINISHED = 1;
  private static final int REQUEST_CAMERA_PERMISSION = 200;
  private boolean mFlashSupported;
  private Handler mBackgroundHandler;
  private HandlerThread mBackgroundThread;
  private Size mPreviewSize;
  private Image cameraImage;
  private boolean recordImage;
  private File folder;
  private boolean takeImage;
  private int imageCounter = 0;
  private Bitmap currentBitmap;
  private Bitmap resultImage;
  private float mMaximalFocalLength;
  private int[] currentFrame;
  private Context mContext;
  private SensorManager mSensorManager;
  private Looper mSensorLooper;
  private SensorEventListener mSensorEventListener;
  private Sensor mAccSensor, mGyrSensor;
  private float[] mTmpGyroEvent = new float[3];
  private float[] mTmpAccEvent = new float[3];
  private int logcounter = 0;
  private float[] currentAcc = new float[3];
  private boolean slamStart = false;
  boolean getFrame;
  byte[] Ydata;
  byte[] Udata;
  byte[] Vdata;
  int[] stride;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
    setContentView(R.layout.activity_main);

    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    mContext = getApplicationContext();
    mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
    imgDealed = (ImageView) findViewById(R.id.texture);
    assert textureView != null;
    mStart = (Button) findViewById(R.id.btn_start);
    moveLeft = (Button) findViewById(R.id.btn_left);
    moveRight = (Button) findViewById(R.id.btn_right);
    moveUp = (Button) findViewById(R.id.btn_up);
    moveDown = (Button) findViewById(R.id.btn_down);
    moveForward = (Button) findViewById(R.id.btn_forward);
    moveBack = (Button) findViewById(R.id.btn_back);
    rotate = (Button) findViewById(R.id.btn_rotate);
    imageNumber = 0;
    recordImageNumber = 0;
    recordImage = false;
    takeImage = false;
    getFrame = false;
    imageCounter = 0;
    stride = new int[3];
    mImageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 3);
    imageByteArray = new byte[640 * 480];
    currentFrame = new int[640 * 480];
    Arrays.fill(currentFrame, 0);
    folder = new File(Environment.getExternalStorageDirectory() + File.separator + "calibration");
    if (!folder.exists()) {
      boolean success = folder.mkdir();
      if (!success) {
        Toast.makeText(MainActivity.this, "Cannot create folder", Toast.LENGTH_SHORT).show();
        return;
      }
    }
    setListener();

  }
  private synchronized void setYUV(byte[] mYdata,byte[] mUdata,byte[] mVdata, int[] strideArray)
  {
    Ydata = Arrays.copyOf(mYdata,mYdata.length);
    Udata = Arrays.copyOf(mUdata,mUdata.length);
    Vdata = Arrays.copyOf(mVdata,mVdata.length);
    stride[0] = strideArray[0];
    stride[1] = strideArray[1];
    stride[2] = strideArray[2];

  }
  private synchronized void copyYUV(byte[] mYdata,byte[] mUdata,byte[] mVdata, int[] strideArray)
  {
    mYdata = Arrays.copyOf(Ydata, Ydata.length);
    mUdata = Arrays.copyOf(Udata, Udata.length);
    mVdata = Arrays.copyOf(Vdata, Vdata.length);
    strideArray[0] = stride[0];
    strideArray[1] = stride[1];
    strideArray[2] = stride[2];
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

          myHandler.sendEmptyMessage(INIT_FINISHED);
          drawHandler.sendEmptyMessage(INIT_FINISHED);
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


  Handler drawHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case INIT_FINISHED:
        new Thread(new Runnable() {

          @Override
          public void run() {
            while (true) {
              // TODO Auto-generated method stub
              if (getFrame) {
                int[] drawFrame = new int[640 * 480];
                byte[] data0;
                byte[] data1;
                byte[] data2;
                int[] strideArray = new int[3];


                synchronized (this) {
                  data0 = Arrays.copyOf(Ydata,Ydata.length);
                  data1 = Arrays.copyOf(Udata,Udata.length);
                  data2 = Arrays.copyOf(Vdata,Vdata.length);
                  strideArray = Arrays.copyOf(stride,stride.length);
                }
                DsoNdkInterface.YUV420ToARGB(data0, data1, data2, drawFrame, 640,
                    480, strideArray[0], strideArray[1], strideArray[2], false);
                int[] resultInt;
                if (imageCounter > 50) {
                  resultInt = DsoNdkInterface.drawFrame(drawFrame);
                  resultImage = Bitmap.createBitmap(640, 502, Bitmap.Config.RGB_565);
                  resultImage.setPixels(resultInt, 0, 640, 0, 0, 640, 502);


                  runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      // TODO Auto-generated method stub
                      imgDealed.setImageBitmap(resultImage);
                      // Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.id.img_dealed);
                    }
                  });
                }
                try {
                  Thread.sleep(33);
                } catch (InterruptedException e) {

                }
              }
            }
          }
        }).start();

        break;
      }
      super.handleMessage(msg);
    }

  };
  Handler myHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case INIT_FINISHED:
        Toast.makeText(MainActivity.this, "init has been finished!", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {

          @Override
          public void run() {
            while (true) {

              do {
                cameraImage = mImageReader.acquireLatestImage();

              } while (cameraImage == null);

                //Log.i(TAG, "getImage!!!!");
              if (cameraImage.getFormat() == ImageFormat.YUV_420_888) {

                ByteBuffer bufferY = cameraImage.getPlanes()[0].getBuffer();
                byte[] data0 = new byte[bufferY.remaining()];
                bufferY.get(data0);

                ByteBuffer bufferU = cameraImage.getPlanes()[1].getBuffer();
                byte[] data1 = new byte[bufferU.remaining()];
                bufferU.get(data1);

                ByteBuffer bufferV = cameraImage.getPlanes()[2].getBuffer();
                byte[] data2 = new byte[bufferV.remaining()];
                bufferV.get(data2);
                int yRowStride = cameraImage.getPlanes()[0].getRowStride();
                int uvRowStride = cameraImage.getPlanes()[1].getRowStride();
                int uvPixelStride = cameraImage.getPlanes()[1].getPixelStride();
                int[] strideArray = {yRowStride, uvRowStride, uvPixelStride};

                /*
                 * ByteArrayOutputStream outputbytes = new ByteArrayOutputStream();
                 *
                 *
                 *
                 * try { outputbytes.write(data0); outputbytes.write(data2);
                 * outputbytes.write(data1); } catch (IOException e) { e.printStackTrace(); }
                 * final YuvImage yuvImage = new YuvImage(outputbytes.toByteArray(),
                 * ImageFormat.NV21, cameraImage.getWidth(),cameraImage.getHeight(), null);
                 * ByteArrayOutputStream outBitmap = new ByteArrayOutputStream();
                 *
                 * yuvImage.compressToJpeg(new Rect(0, 0, cameraImage.getWidth(),
                 * cameraImage.getHeight()), 95, outBitmap); currentBitmap =
                 * BitmapFactory.decodeByteArray(outBitmap.toByteArray(), 0, outBitmap.size());
                 */
                int w = cameraImage.getWidth();
                int h = cameraImage.getHeight();
                int[] current_L = new int[w * h];

                synchronized (this) {
                  Ydata = Arrays.copyOf(data0,data0.length);
                  Udata = Arrays.copyOf(data1,data1.length);
                  Vdata = Arrays.copyOf(data2,data2.length);
                  stride = Arrays.copyOf(strideArray,strideArray.length);

                }
                float[] calculateAcc = {0.0f, 0.0f, 0.0f};
                int[] resultInt;
                if (imageCounter > 50) {
                  resultInt = DsoNdkInterface.runDsoSlam(data0, current_L, 640, 480, calculateAcc);

                }

              }
              cameraImage.close();

              imageCounter++;
              getFrame = true;
            }
          }

        }).start();
        break;
      }
      super.handleMessage(msg);
    }
  };

  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice camera) {
      // This is called when the camera is open
      Log.e(TAG, "onOpened");
      cameraDevice = camera;
      createRecord();
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

  protected void createRecord() {
    if (null == cameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }

    try {

      recordRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

      recordRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
      recordRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);
      recordRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      recordRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
      recordRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,new Range<Integer>(60,65));

      if (mMaximalFocalLength > 0) {
        recordRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, mMaximalFocalLength);
      }
      recordRequestBuilder.addTarget(mImageReader.getSurface());
      cameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()),
          new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
              // The camera is already closed
              if (null == cameraDevice) {
                return;
              }
              // When the session is ready, we start displaying the preview.
              recordCaptureSessions = cameraCaptureSession;
              updateRecord();
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
              Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
            }
          }, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }

  }

  private void openCamera(int width, int height) {
    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

    Log.e(TAG, "openCamera");
    try {
      cameraId = manager.getCameraIdList()[0];
      // Add permission for camera and let user grant the permission
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
          && ActivityCompat.checkSelfPermission(this,
              Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(MainActivity.this,
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
      super.onCaptureCompleted(session, request, result);
      try {
        session.capture(recordRequestBuilder.build(), captureListener, mBackgroundHandler);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }
  };

  protected void updateRecord() {
    if (null == cameraDevice) {
      Log.e(TAG, "updatePreview error, return");
    }
    try {
      recordCaptureSessions.setRepeatingRequest(recordRequestBuilder.build(), null, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  // For horizental orientation

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
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
        // close the app
        Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission",
            Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.e(TAG, "onResume");
    startBackgroundThread();
    openCamera(640, 480);

  }

  @Override
  protected void onPause() {
    Log.e(TAG, "onPause");
    // closeCamera();
    stopBackgroundThread();
    super.onPause();
  }

  @Override
  protected  void onDestroy(){

    if(slamStart)
    {
      closeCamera();
      DsoNdkInterface.stopSLAM();
    }
    super.onDestroy();
  }
}
