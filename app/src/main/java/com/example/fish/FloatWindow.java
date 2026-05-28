package com.example.fish;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
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
    private boolean threadIsRunning = true,  threadIsRunning1 = true;;
    private static final String TAG = "FishDebug";


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
        int textSize = (int)(ImageHadle.height*0.013f);
        mTipsText.setTextSize(textSize);
       // mTipsText.setTextSize(14);
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
        mTipsParams.x = 0;
        mTipsParams.y = ImageHadle.height/2;

        mWindowManager.addView(mTipsText, mTipsParams);
    }


    public int getGreenSizPo()
    {
        if(ImageHadle.width>1920)
        {
            return MainFunction.sizdToTrsf(21);//2340 2400 2408
        } else if (ImageHadle.width>720) {
            return MainFunction.sizdToTrsf(23);//1920
        }
        else {
            return MainFunction.sizdToTrsf(20)+2;//1280
        }
    }

    public void setTipsText(String text) {
        if (mTipsText == null || text == null) {
            return;
        }
        // 抛到主线程更新UI
        mTipsText.post(() -> mTipsText.setText(text));
    }
    public static boolean xmlState = false;
    private void initFloatWindow(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mFloatView = LayoutInflater.from(context).inflate(R.layout.float_window, null);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O//安卓版本不同设置不同类型
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        Point screenSize = new Point();
        mWindowManager.getDefaultDisplay().getSize(screenSize);
        int screenWidth = screenSize.x;
        float density = context.getResources().getDisplayMetrics().density;

        // 2. 找到按钮控件
        ImageButton btn1 = mFloatView.findViewById(R.id.float_btn);

        // 3. if 判断，动态修改按钮宽高（等价改 xml 的 dp）
        int targetDp;
        if (screenWidth <= 540) {
            targetDp = 40;   // 极小屏
        } else if (screenWidth <= 720) {
            targetDp = 40;   // 小屏
        } else {
            targetDp = 55;   // 大屏，和xml默认一致
        }

        // dp 转像素，赋值给按钮布局参数
        int px = (int) (targetDp * density + 0.5f);
        ViewGroup.LayoutParams btnParams = btn1.getLayoutParams();
        btnParams.width = px;
        btnParams.height = px;
        btn1.setLayoutParams(btnParams);
        // 5. 初始化悬浮窗参数
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        mParams.x = 100;
        mParams.y = 300;


        // 按钮点击
        ImageButton btn = mFloatView.findViewById(R.id.float_btn);
        MainFunction.init();

        //检测文件内容不空读取数据

        btn.setOnClickListener(v -> {
            Log.d(TAG, "按钮被点击，当前按钮状态：" + buttonState);
            if(buttonState) {
                buttonState = false;
                threadIsRunning = true;
               threadIsRunning1 = true;
                btn.setImageResource(R.drawable.zaowu_glasses);
                setTipsText("运行中");
                Log.i(TAG, "启动钓鱼运行线程");


                if(StrogeXml.readTwoPoint(context)[0][0] == 0) {
                    xmlState = true;
                }
                else {
                    int point[][] = StrogeXml.readTwoPoint(context);
                    MainFunction.hookPoint = new Point(point[0][0],point[0][1]);
                    MainFunction.fishStaPoint = new Point(point[1][0],point[1][1]);
                    MainFunction.cGLinepoint = new Point( MainFunction.fishStaPoint.x +MainFunction.sWToTrsf(119),
                            MainFunction.fishStaPoint.y + getGreenSizPo());
                    xmlState =false;

                }
              /*  if(MainFunction.hookPoint == null)
                {
                    MainFunction.hookPoint = ImageHadle.uiLineSearch(MainFunction.hook,new Point((int)((0.88125*ImageHadle.width) +10.5),(int)(0.885*ImageHadle.height)),
                            57*ImageHadle.height/1080*2,200);
                    MainFunction.fishStaPoint = ImageHadle.uiLineSearch(MainFunction.fishsate,
                            new Point((int)((0.3324*ImageHadle.width)-81.43),(int)(0.085*ImageHadle.height)),
                            60*ImageHadle.height/1080*2,140);
                    MainFunction.cGLinepoint = new Point( MainFunction.fishStaPoint.x +MainFunction.sWToTrsf(119),
                            MainFunction.fishStaPoint.y + getGreenSizPo());
                    Log.v(TAG, "fishstaok");
                }
                if (MainFunction.isFishStae()) {
                    Log.d(TAG, String.valueOf(MainFunction.greenPostison()));

                    //clickStablePostion();
                }
                else{
                    Log.v(TAG, "污垢子");
                }*/
              //  Log.i(TAG, "X坐标"+ (int)((0.88125*ImageHadle.width) +10.5));

             //   Point point = ImageHadle.uiLineSearch(MainFunction.hook,new Point((int)((0.88125*ImageHadle.width) +10.5),(int)(0.885*ImageHadle.height)),
              //          57*ImageHadle.height/1080*2,200);

           /*     Point pointFish = ImageHadle.uiLineSearch(MainFunction.fishsate,
                        new Point((int)((0.3324*ImageHadle.width)-81.43),(int)(0.085*ImageHadle.height)),
                        60*ImageHadle.height/1080*2,140);

                if(  pointFish!=null ){
                 //  Log.i(TAG, "X坐标"+ point.x +"Y坐标"+ point.y);
                    Log.i(TAG, "fishX坐标"+ pointFish.x +"Y坐标"+ pointFish.y);

               }
               else {
                    Log.i(TAG, "搜索失败");

                }*/


                    new Thread(() -> {
                        try {
                            if(xmlState) {
                                setTipsText("首次图标坐标扫描(注意不要乱动屏幕)");
                                sleep(2000);
                                setTipsText("扫描钩子(耐心等待)");
                                while (MainFunction.hookPoint == null)
                                {
                                    MainFunction.hookPoint = ImageHadle.uiLineSearch(MainFunction.hook,new Point((int)((0.88125*ImageHadle.width) +10.5),(int)(0.885*ImageHadle.height)),
                                            57*ImageHadle.height/1080*2,200);
                                }

                                    boolean stateWithserch = true;
                                    setTipsText("扫到钩子，开始点击");
                                sleep(500);
                                    while(stateWithserch)
                                    {
                                        clickStablePostion();
                                        if(MainFunction.isHook() == false){
                                            setTipsText("扫描鱼标");
                                            MainFunction.fishStaPoint = ImageHadle.uiLineSearch(MainFunction.fishsate,
                                                    new Point((int)((0.3324*ImageHadle.width)-81.43),(int)(0.085*ImageHadle.height)),
                                                    60*ImageHadle.height/1080*2,140);
                                            if(MainFunction.fishStaPoint != null)
                                            {
                                                MainFunction.cGLinepoint = new Point( MainFunction.fishStaPoint.x +MainFunction.sWToTrsf(119),
                                                        MainFunction.fishStaPoint.y + getGreenSizPo());
                                                StrogeXml.writeTwoPoint(context,MainFunction.hookPoint.x,MainFunction.hookPoint.y,
                                                                        MainFunction.fishStaPoint.x,MainFunction.fishStaPoint.y);
                                                setTipsText("ok所有图标均已扫完");
                                                sleep(500);
                                                xmlState = false;
                                                stateWithserch = false;
                                                break;
                                            }
                                        }
                                    }

                            }
                            setTipsText("1s后运行（如果不动了请清掉该程序后台并重启）");
                            sleep(1000);
                            setTipsText("运行中");
                            int mainState = 0;
                            while (threadIsRunning) {
                                switch(mainState)
                                {
                                    case 0:

                                        if(MainFunction.isHook()) {
                                            //Log.d(TAG, "检测到钩子，执行点击");
                                            clickStablePostion();
                                        }else if(MainFunction.isFishStae()){
                                          /*  MainFunction.addERROR = 0;
                                            MainFunction.lastError = 0;
                                      */      mainState = 1;

                                        }else {

                                            clickStablePostion();
                                        }
                                        break;
                                    case 1:

                                        if(MainFunction.isFishStae()){

                                            MainFunction.contralCurr();
                                        }
                                        else {

                                            clickStablePostion();
                                            if (MainFunction.isHook()) {
                                                MainFunction.weithState = true;
                                                mainState = 0;

                                            }
                                        }
                                        break;
                                }
                            }
                            Log.i(TAG, "钓鱼线程正常退出循环");
                        } catch (Exception e) {
                            Log.e(TAG, "钓鱼线程异常崩溃", e);
                        }
                    }).start();


            }
            else {
                threadIsRunning1  = false;
                buttonState = true;
                threadIsRunning = false;
                btn.setImageResource(R.drawable.zaowu_icon);
                setTipsText("已暂停");
                Log.i(TAG, "停止钓鱼运行，线程标记关闭");
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
        AutoClick.service.click(MainFunction.hookPoint.x,MainFunction.hookPoint.y,0,50);//z这样都没有也能防卡住

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




