package com.example.fish;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SCREEN_RECORD = 100;
    public static ImageHadle imageHadle;
    public static Context context;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public static int width,height,dpi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        context = this;
        if (!OpenCVLoader.initDebug()) {
            throw new RuntimeException("OpenCV 初始化失败！");
        }
        //无障碍
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请先开启无障碍权限！", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }
        // 开启悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                //checkAccessBackHome();
            }
        }
        //鲁平创权限
        requestScreenRecordPermission();


        Button btnResetPos = findViewById(R.id.ResetPos);
        // 设置点击事件
        btnResetPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAllPointData();
            }
        });


    }
    private void resetAllPointData() {
        // 1. 清空内存中的坐标变量
        MainFunction.hookPoint = null;
        MainFunction.fishStaPoint = null;
        MainFunction.cGLinepoint = null;

        // 2. 恢复标记位，让悬浮窗下次重新进入扫描模式
        FloatWindow.xmlState = false;

        // 3. 清空本地XML存储的坐标（覆盖写入0值）
        StrogeXml.writeTwoPoint(this, 0, 0, 0, 0);

        // 可选：弹出Toast提示
        android.widget.Toast.makeText(this, "坐标已重置，下次运行将重新扫描", android.widget.Toast.LENGTH_SHORT).show();
    }
    private void requestScreenRecordPermission() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN_RECORD);
    }

    // 接收用户授权结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCREEN_RECORD) {
            if (resultCode == RESULT_OK) {

                Toast.makeText(this, "录屏权限获取成功！", Toast.LENGTH_SHORT).show();
                Log.d("ScreenCapture", "✅ 录屏权限授权成功，通道已初始化");
                ScreenRecordService.setAuthData(resultCode, data);

                // 权限获取后
                // 启动前台服务，服务内部自动init
                Intent serviceIntent = new Intent(this, ScreenRecordService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                // 在这里初始化 ImageHadle
                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getRealMetrics(metrics); // ✅真实全屏分辨率
                width = metrics.widthPixels;   // 真正宽度1080
                height = metrics.heightPixels; // 真正高度2408
                dpi = metrics.densityDpi;//480
               // Toast.makeText(this, String.valueOf(width)+"    " +String.valueOf(height) +"  " +String.valueOf(dpi) , Toast.LENGTH_LONG).show();

                imageHadle = new ImageHadle(width, height, dpi);
                // 延时弹出悬浮窗，等待实例赋值完成
                new android.os.Handler().postDelayed(() -> {
                    FloatWindow floatWindow = new FloatWindow(MainActivity.this);
                    floatWindow.show();
                }, 300);
                //imageHadle.init(); // 启动截图
            } else {
                Toast.makeText(this, "拒绝了权限", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private boolean isAccessibilityEnabled() { //无障碍
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : list) {
            if (info.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }
    // 重写返回键，正常退出
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }



}