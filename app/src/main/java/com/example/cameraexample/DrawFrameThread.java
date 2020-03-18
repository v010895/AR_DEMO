package com.example.cameraexample;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.Image;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import com.example.cameraexample.nativefunction.DsoNdkInterface;
import java.util.Arrays;

public class DrawFrameThread extends Thread {
  private static final String TAG="DrawFrameThread";
  private SurfaceHolder mSurfaceHolder;
  private byte[] Ydata, Udata, Vdata;
  private int[] stride;
  private Canvas surfaceCanvas;
  private Looper DrawThreadLooper;
  boolean getFrame;
  public DrawFrameThread(SurfaceHolder _SurfaceHolder) {
    mSurfaceHolder = _SurfaceHolder;
    getFrame = false;
  }

  public void setGetFrame(boolean _getFrame)
  {
    getFrame = _getFrame;
  }
  public synchronized void setYUVData(byte[] _Ydata, byte[] _Udata, byte[] _Vdata, int[] _stride) {
    Ydata = _Ydata;
    Udata = _Udata;
    Vdata = _Vdata;
    stride = _stride;
  }

  public synchronized void getYUVData(byte[] outYdata, byte[] outUdata, byte[] outVdata, int[] outStride) {
    outYdata = Ydata;
    outUdata = Udata;
    outVdata = Vdata;
    outStride = stride;
  }

  @Override
  public void run() {
    while (true) {
      try {
        if (null != mSurfaceHolder) {
          surfaceCanvas = mSurfaceHolder.lockCanvas();
          if (getFrame) {
            int[] drawFrame = new int[640 * 480];

            int[] strideArray;
            byte[] outYdata;
            byte[] outUdata;
            byte[] outVdata;
            synchronized (this)
            {
              outYdata = Arrays.copyOf(Ydata,Ydata.length);
              outUdata = Arrays.copyOf(Udata,Udata.length);
              outVdata = Arrays.copyOf(Vdata,Vdata.length);
              strideArray = Arrays.copyOf(stride, stride.length);
            }
            DsoNdkInterface.YUV420ToARGB(outYdata, outUdata, outVdata, drawFrame, 640,
                480, strideArray[0], strideArray[1], strideArray[2], false);
            int[] resultInt;
            resultInt = DsoNdkInterface.drawFrame(drawFrame);
            Bitmap resultImage = Bitmap.createBitmap(640, 502, Bitmap.Config.RGB_565);
            resultImage.setPixels(resultInt, 0, 640, 0, 0, 640, 502);
            //Rect srcRect = new Rect(0,0,,testCanvas.getHeight());
            Rect dstRect = new Rect(0, 0, surfaceCanvas.getWidth(), surfaceCanvas.getHeight());
            surfaceCanvas.drawBitmap(resultImage, null, dstRect, null);
          } else {
            Thread.sleep(33);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (null != surfaceCanvas) {
          mSurfaceHolder.unlockCanvasAndPost(surfaceCanvas);
        }
      }

    }
  }
}
