package com.byrcegao.tpdemo;

import android.app.Activity;
import android.os.Bundle;
import com.brycegao.tpannotation.DebugLogger;

public class SecondActivity extends Activity {

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_second);

    doSomething(1);

  }

  @DebugLogger
  private void doSomething(int i) {
    try {
      Thread.sleep(100); //仅仅为了测试
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
