package com.example.fish;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.io.File;

public class FloatWindow {


    private WindowManager mWindowManager;//·窗口管理对象
    private View mFloatView;//悬浮创控件
    private WindowManager.LayoutParams mParams;

    private boolean buttonState = true;
    private float downX, downY;
    private int startX, startY;

    // 全局 Application Context（永远不死，悬浮窗专用）
    private Context appContext;


    public FloatWindow(Context context) {//构造函数
        appContext = context.getApplicationContext();
        initFloatWindow(context);
    }
    private View mTipsView;
    private WindowManager.LayoutParams mTipsParams;
    private android.widget.TextView mTipsText;
    private boolean threadIsRunning = true;



    // ------------------- 实时绘制两个标记点 -------------------
    private android.widget.TextView markGreen, markCursor;
    private WindowManager.LayoutParams paramsGreen, paramsCursor;

    private void createMarkPoints() {
        // 绿色条中心标记（红色）
        markGreen = new android.widget.TextView(appContext);
        markGreen.setText("✱");
        markGreen.setTextSize(20);
        markGreen.setTextColor(0xFFFF0000);

        // 光标标记（黄色）
        markCursor = new android.widget.TextView(appContext);
        markCursor.setText("✱");
        markCursor.setTextSize(20);
        markCursor.setTextColor(0xFFFFFF00);

        // 共用参数
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        // 绿色标记
        paramsGreen = new WindowManager.LayoutParams();
        paramsGreen.width = 50;
        paramsGreen.height = 50;
        paramsGreen.type = type;
        paramsGreen.flags = flags;
        paramsGreen.format = PixelFormat.TRANSLUCENT;
        paramsGreen.gravity = Gravity.TOP | Gravity.LEFT;

        // 光标标记
        paramsCursor = new WindowManager.LayoutParams();
        paramsCursor.width = 50;
        paramsCursor.height = 50;
        paramsCursor.type = type;
        paramsCursor.flags = flags;
        paramsCursor.format = PixelFormat.TRANSLUCENT;
        paramsCursor.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowManager.addView(markGreen, paramsGreen);
        mWindowManager.addView(markCursor, paramsCursor);
    }

    // 实时更新两个 ✱ 位置
    public void updateMarkPoints(int greenX, int cursorX) {
        if (markGreen == null || markCursor == null) return;

        // 绿色条中心 ✱ 位置
        paramsGreen.x = greenX;
        paramsGreen.y = 86 + 2; // 对应你截取区域的Y
        mWindowManager.updateViewLayout(markGreen, paramsGreen);

        // 光标 ✱ 位置
        paramsCursor.x = cursorX;
        paramsCursor.y = 86 + 2; // 对应光标截取区域Y
        mWindowManager.updateViewLayout(markCursor, paramsCursor);
    }

    private void createFloatTips() {
        mTipsText = new android.widget.TextView(appContext);
        mTipsText.setText("未开始");
        mTipsText.setTextSize(14);
        mTipsText.setTextColor(0xFFFF0000);
        mTipsText.setBackgroundColor(0x88000000);
        mTipsText.setPadding(10, 5, 10, 5);

        mTipsParams = new WindowManager.LayoutParams();
        mTipsParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mTipsParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mTipsParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        mTipsParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        mTipsParams.format = PixelFormat.TRANSLUCENT;
        mTipsParams.gravity = Gravity.TOP | Gravity.LEFT;
        mTipsParams.x = 100;
        mTipsParams.y = 100;

        mWindowManager.addView(mTipsText, mTipsParams);
    }
    public void setTipsText(String text) {
        if (mTipsText != null) {
            mTipsText.setText(text);
        }
    }
    private void initFloatWindow(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mFloatView = LayoutInflater.from(context).inflate(R.layout.float_window, null);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O//安卓版本不同设置不同类型
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // 悬浮参数
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,  // 1. 宽度自适应按钮大小
                WindowManager.LayoutParams.WRAP_CONTENT,  // 2. 高度自适应按钮大小
                type,                                     // 3. 悬浮窗类型（全局顶层）
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // 4. 不抢焦点（不影响你点屏幕）
                PixelFormat.TRANSLUCENT                   // 5. 透明格式
        );
        //初始位置
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        mParams.x = 100;
        mParams.y = 300;


        // 按钮点击
        Button btn = mFloatView.findViewById(R.id.float_btn);
        MainFunction.init();
        btn.setOnClickListener(v -> {
            if(buttonState) {
                buttonState = false;
                threadIsRunning = true;
                btn.setText("关闭钓鱼");
                new Thread(() -> {
                    try {
                        int mainState = 0;//状态重置
                        while (threadIsRunning) {
                            switch(mainState)
                            {
                                case 0://状态一
                                       if(MainFunction.isHook()) //识别有无钩子然后点击
                                       {
                                           clickStablePostion();
                                       }else if(MainFunction.isFishStae()){//没有识别有无鱼标
                                           mainState = 1;//有进入状态二

                                           MainFunction.addERROR = 0;
                                           MainFunction.lastError = 0;
                                       }
                                       else {//俩种状态都没有  点击会点击的地方可以有更大的空间挂其他应用
                                           clickStablePostion();
                                       }
                                       break;
                                case 1://状态二
                                    if(MainFunction.isFishStae()){
                                        MainFunction.contralCurr();
                                    }
                                    else
                                    {//识别不到就点击屏幕
                                        clickStablePostion();
                                        if (MainFunction.isHook()) {
                                            MainFunction.weithState = true;
                                            mainState = 0;
                                        }
                                    }
                                    break;
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start(); // 启动线程
            }
            else {
                buttonState = true;
                threadIsRunning = false;
                btn.setText("开始钓鱼");
            }


        });
        //拖动
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startX = mParams.x;
                    startY = mParams.y;
                    break;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - downX);
                    int dy = (int) (event.getRawY() - downY);
                    mParams.x = startX + dx;
                    mParams.y = startY + dy;
                    mWindowManager.updateViewLayout(mFloatView, mParams);
                    break;
            }
            return false;
        });
    }
    public void clickStablePostion() {//2151   952
        AutoClick.service.click((2151*MainActivity.height/2408),(952*MainActivity.width/1080),0,50);//z这样都没有也能防卡住

    }

    public void show() {//显示悬浮创
        if (mFloatView.getParent() == null) {
            mWindowManager.addView(mFloatView, mParams);
        }
        if (mTipsText == null) {
            createFloatTips();
        }
     /*   if (markGreen == null) {
            createMarkPoints();
        }*/
    }
    // ✅ 专门提供一个方法，授权完再初始化
//    public void initCapture() {
//        if (MainActivity.imageHadle != null) {
//            MainActivity.imageHadle.init();
//        }
//    }


}




