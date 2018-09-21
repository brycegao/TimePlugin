# TimePlugin
实现JakeWharton的hugo功能，即打印安卓函数执行时间和参数值

用法参考demo工程
在工程build.gradle添加
 classpath 'com.brycegao.timeplugin:timeplugin:1.0.4'
 
在app模块build.gradle
  implementation 'com.brycegao.tpannotation:tpannotation:1.0.2'
apply plugin: 'timeplugin'

在类或方法前添加注解DebugLogger
@DebugLogger
public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    showMsg(1, "this is test");

    findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        startActivity(intent);
      }
    });
  }

  private void showMsg(int i, String msg) {
    try {
      Thread.sleep(100); //仅仅为了测试
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
  }
}

输出：
09-21 19:20:21.987  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onWindowFocusChanged耗时：0毫秒
09-21 19:20:21.987  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onWindowFocusChanged参数：hasFocus:true
09-21 19:20:22.231  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onWindowFocusChanged耗时：0毫秒
09-21 19:20:22.231  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onWindowFocusChanged参数：hasFocus:false
09-21 19:20:27.180  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:showMsg耗时：101毫秒
09-21 19:20:27.180  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:showMsg参数：i:1参数：msg:this is test
09-21 19:20:27.180  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onCreate耗时：166毫秒
09-21 19:20:27.180  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onCreate参数：savedInstanceState:null
09-21 19:20:27.281  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onWindowFocusChanged耗时：0毫秒
09-21 19:20:27.281  3308  3308 D MethodTime: com.byrcegao.tpdemo.MainActivity:onWindowFocusChanged参数：hasFocus:true





