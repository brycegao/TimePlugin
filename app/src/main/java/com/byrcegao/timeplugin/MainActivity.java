package com.byrcegao.timeplugin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.byrcegao.tpannotation.DebugLogger;

@DebugLogger
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    showMsg("this is a test", 100);
  }

  private void showMsg(String msg, int i) {
    try {
      Thread.sleep(100); //仅仅为了测试
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
