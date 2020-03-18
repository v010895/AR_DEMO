package com.example.cameraexample;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
public class MainActivity extends AppCompatActivity {
  ARFragment mARFragment;
  // load tracking library
  static {
    System.loadLibrary("SLAM");
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
    setContentView(R.layout.activity_main);
    mARFragment = new ARFragment();
    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    if (null == savedInstanceState) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.container, mARFragment)
          .commit();
    }

  }

}
