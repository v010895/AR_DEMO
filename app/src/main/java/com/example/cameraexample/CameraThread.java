package com.example.cameraexample;
import android.content.Context;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.example.cameraexample.nativefunction.DsoNdkInterface;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraThread extends Thread{
  private Context mContext;
  private static final String TAG = "CameraThread";
  private static final int REQUEST_CAMERA_PERMISSION=200;
  private String cameraId;
  private float mMaximalFocalLength;
  protected CameraDevice cameraDevice;
  protected CaptureRequest.Builder captureRequestBuilder;
  protected CameraCaptureSession captureSession;
  private ImageReader mImageReader;
  private Handler mBackgroundHandler;
  private HandlerThread mBackgroundThread;
  private Image cameraImage;
  private Looper cameraThreadLooper;
  boolean getFrame;
  private DrawFrameThread mDrawFrameThread;
  private byte[] Ydata , Udata, Vdata;
  private int[] stride;

  public CameraThread(ImageReader _reader, DrawFrameThread _drawFrameThread)
  {
    mImageReader = _reader;
    mDrawFrameThread = _drawFrameThread;
    getFrame =false;

  }

  @Override
  public void run() {
    while (true) {
      do {
        cameraImage = mImageReader.acquireLatestImage();


      } while (cameraImage == null);
      Long timestamp = cameraImage.getTimestamp();
      getFrame = true;
      //Log.i(TAG, String.format("Image timestamp %d",timestamp));
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

        int w = cameraImage.getWidth();
        int h = cameraImage.getHeight();
        int[] current_L = new int[w * h];

        Ydata = Arrays.copyOf(data0, data0.length);
        Udata = Arrays.copyOf(data1, data1.length);
        Vdata = Arrays.copyOf(data2, data2.length);
        stride = Arrays.copyOf(strideArray, strideArray.length);
        mDrawFrameThread.setYUVData(Ydata, Udata, Vdata, stride);

        float[] calculateAcc = {0.0f, 0.0f, 0.0f};
        int[] resultInt;
        {
          resultInt = DsoNdkInterface.runDsoSlam(data0, current_L, 640, 480, calculateAcc);
        }

      }
      cameraImage.close();
      mDrawFrameThread.setGetFrame(getFrame);
    }
  }
}



