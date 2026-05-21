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
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
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
                int width = metrics.widthPixels;   // 真正宽度
                int height = metrics.heightPixels; // 真正高度
                int dpi = metrics.densityDpi;
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