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
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
    private static boolean mScreenRecordAuth = false;
    public static int width,height,dpi;

    public static boolean bigAre = false;
    // 权限状态文本控件
    private TextView tvPermissionStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // 绑定权限文本控件
        tvPermissionStatus = findViewById(R.id.textView2);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_disclaimer, null);
// 2. 先找按钮（必须在 Builder 之前执行）


        // 绑定控件并动态赋值文字 防止逆向修改
        TextView tvFooter = findViewById(R.id.textView);
        TextView tvTitle = dialogView.findViewById(R.id.tv_title);
        TextView tvContent = dialogView.findViewById(R.id.tv_content);
        Button btnOk = dialogView.findViewById(R.id.btn_ok);
        tvTitle.setText("免责声明");
        tvContent.setText("本软件开源免费，禁止任何形式商用。仅供个人学习交流使用，请勿用于违规场景。使用本软件产生的一切后果，由使用者自行承担。");
        btnOk.setText("已知晓");
        tvFooter.setText("by:pingtang 联系邮箱:2471538565@qq.com");

        //Button btnOk = dialogView.findViewById(R.id.btn_ok);

// 3. 构建弹窗
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();

// 4. 设置点击事件
        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            context = this;
            if (!OpenCVLoader.initDebug()) {
                throw new RuntimeException("OpenCV 初始化失败！");
            }
            //无障碍
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "请先开启无障碍权限！", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                updatePermissionText();
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

            Button btnExpand = findViewById(R.id.areBig);
            btnExpand.setOnClickListener(v -> {
              if(!bigAre)
              {
                  btnExpand.setText("恢复搜索范围");
                  bigAre= true;
                  Toast.makeText(MainActivity.this, "已扩大搜索范围,并且自动重置坐标", Toast.LENGTH_SHORT).show();
                  resetAllPointData();
                  updatePermissionText();
              }else {
                  btnExpand.setText("扩大搜索范围(长按设置扩大倍率)");
                  bigAre = false;
                  Toast.makeText(MainActivity.this, "已恢复搜索范围，并且自动重置坐标", Toast.LENGTH_SHORT).show();
                  resetAllPointData();
                  updatePermissionText();

              }

                     });
            // ==========新增长按：弹出倍率修改弹窗==========
            btnExpand.setOnLongClickListener(v->{
                showExpandSettingDialog();
                return true;
            });

            Button btnResetPos = findViewById(R.id.ResetPos);
            // 设置点击事件
            btnResetPos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetAllPointData();
                }
            });

            Button mJoinGroupBtn = findViewById(R.id.JoinGroup);

            // 设置点击事件
            mJoinGroupBtn.setOnClickListener(v -> joinQQGroup());

        });
// 5. 显示弹窗
        dialog.show();

    }

    // 检测无障碍权限

    //长按弹窗：修改expendTheSechArea放大倍率
    //长按弹窗：修改expendTheSechArea放大倍率
    private void showExpandSettingDialog() {
        Context app = getApplicationContext();
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(app);
        builder.setTitle("图像扩大搜索倍率设置");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(app);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40,20,40,20);

        android.widget.TextView tvTip = new android.widget.TextView(app);
        tvTip.setText("扩大搜索系数(整数)平板用户适当增大");
        tvTip.setTextSize(15);
        layout.addView(tvTip);

        final android.widget.EditText etExpand = new android.widget.EditText(app);
        etExpand.setText(String.valueOf(SetingTheParmer.expendTheSechArea));
        layout.addView(etExpand);

        builder.setView(layout);
        android.app.AlertDialog dialog = builder.create();

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.type = type;
        dialog.getWindow().setAttributes(lp);

        dialog.setButton(android.app.Dialog.BUTTON_POSITIVE, "保存", (d, which) -> {
            try{
                int val = Integer.parseInt(etExpand.getText().toString().trim());
                SetingTheParmer.expendTheSechArea = val;
                SetingTheParmer.saveFile(app);
                Toast.makeText(this,"倍率已保存",Toast.LENGTH_SHORT).show();
            }catch (NumberFormatException e){
                Toast.makeText(this,"输入非法数字",Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setButton(android.app.Dialog.BUTTON_NEGATIVE,"取消",(d,w)->{});
        dialog.show();
    }

    // 检测悬浮窗权限
    private boolean isFloatWindowEnabled() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    // 检测录屏权限（根据你项目逻辑：授权后标记为true）
    // 这里复用你 ScreenRecordService 的授权状态，可根据自身逻辑修改


    private void updatePermissionText(){
        boolean floatPerm = isFloatWindowEnabled();
        boolean accessPerm = isAccessibilityEnabled();
        boolean recordPerm = mScreenRecordAuth;

        String content = String.format("当前权限状态：悬浮窗%s 无障碍%s 录屏%s\n 当前搜索范围扩大状态%s",
                floatPerm, accessPerm, recordPerm,bigAre);
        tvPermissionStatus.setText(content);
    }
    private void joinQQGroup() {
        try {
            // 直接用群号打开QQ群资料页，点加入即可
            String url = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=206811417&card_type=group&source=qrcode";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 没装QQ就打开你的加群链接
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://qun.qq.com/universal-share/share?ac=1&authKey=lT2wzR3odFrOzeSOtd%2Bcz%2FBzbU6M8B0UVQErHi8lKMl%2FLfweqeqNUidBx3Oeu8Ak&busi_data=eyJncm91cENvZGUiOiIyMDY4MTE0MTciLCJ0b2tlbiI6IkU1U21oKzRlSlNxV2ovZ2hqbVhRckVGTSszYzlLMEFtWlkycC92M05LWk83NXlLSlh4cEZXVlZEY3c0dmxQTFIiLCJ1aW4iOiIyNDcxNTM4NTY1In0%3D&data=NasoBg7Se0LX8nCyCFJWlQ3imWo_3HA8fd5uCDBBPRDGtdFOuyvBCHNgr3oegsRUBcL5HoVkzR8CusQYjW6sHg&svctype=4&tempid=h5_group_info"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Toast.makeText(context, "请安装QQ并加入群", Toast.LENGTH_SHORT).show();
        }
    }


    private void resetAllPointData() {
        // 1. 清空内存中的坐标变量
        MainFunction.hookPoint = null;
        MainFunction.fishStaPoint = null;
        MainFunction.cGLinepoint = null;

        // 2. 恢复标记位，让悬浮窗下次重新进入扫描模式
        FloatWindow.xmlState = false;

        // 3. 清空本地XML存储的坐标（覆盖写入0值）
        StrogeXml.writePoint(this, 0, 0, 0, 0,0,0);

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
                mScreenRecordAuth = true;
                Toast.makeText(this, "录屏权限获取成功！", Toast.LENGTH_SHORT).show();
                Log.d("ScreenCapture", "✅ 录屏权限授权成功，通道已初始化");
                ScreenRecordService.setAuthData(resultCode, data);
                updatePermissionText();
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