package com.example.cameraexample.nativefunction;

public class DsoNdkInterface {
    public static native void initSystemWithParameters(String calibrationPath);
    public static native int[] runDsoSlam(byte[] data_L, int[] currentFrame, int width, int height, float[] currentAcc);
    public native static void glesInit();
    public native static void glesRender();
    public native static void move(int motion);
    public native static int[] drawFrame(int[] currentFrame);
    public native static void glesResize(int width, int height);
    public static native void trackWithIMU(int sensorType, float[] sensorData, long timestamp);
    public static native void YUV420ToARGB(byte[] y, byte[] u, byte[] v, int[] output,
                                        int width, int height, int yRowStride, int uvRowStride,
                                        int uvPixelStride, boolean halfSize);


}
