package com.example.fish;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import org.opencv.core.Scalar;


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

    public static int fishNum = 0;

    public FloatWindow(Context context) {//构造函数
        appContext = context.getApplicationContext();
        SetingTheParmer.init(appContext);
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


    // 三击弹窗相关
    private int tipsClickCount = 0;
    private long lastTipsClickTime = 0;
    // 三击判定间隔(ms)：1000ms内连续3次点击触发弹窗
    private final long CLICK_INTERVAL = 300;


    private void createFloatTips() {
        mTipsText = new android.widget.TextView(appContext);
        mTipsText.setText("未开始（连续点击三下进入参数设置）");
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
       // mTipsParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;

        mTipsParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE ;
        mTipsParams.format = PixelFormat.TRANSLUCENT;
        mTipsParams.gravity = Gravity.TOP | Gravity.LEFT;
        mTipsParams.x = 0;
        mTipsParams.y = ImageHadle.height/2;
// 绑定点击监听，实现三击弹窗
        mTipsText.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            // 超过间隔清空计数
            if (now - lastTipsClickTime > CLICK_INTERVAL) {
                tipsClickCount = 0;
            }
            lastTipsClickTime = now;
            tipsClickCount++;

            if (tipsClickCount >= 3) {
                tipsClickCount = 0; // 重置计数
                showSettingDialog(); // 弹出配置弹窗
            }
        });

        mWindowManager.addView(mTipsText, mTipsParams);
    }

    private void showSettingDialog() {

        // 改用系统悬浮窗弹窗，避开Activity Token报错
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(appContext);
        builder.setTitle("钓鱼参数配置");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(appContext);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40,20,40,20);

        // 单独标题文本：全局循环帧数(hz)
        android.widget.TextView tvAllTip = new android.widget.TextView(appContext);
        tvAllTip.setText("全局循环帧数(hz)越大光标越灵敏，但发热也越严重");
        tvAllTip.setTextSize(15);
        layout.addView(tvAllTip);

        final android.widget.EditText etAllTime = new android.widget.EditText(appContext);
        etAllTime.setText(String.valueOf((int)(1000.0 / SetingTheParmer.allTheardTime)));
        layout.addView(etAllTime);

        // 单独标题文本：点击后等待延时(ms)
        android.widget.TextView tvClickTip = new android.widget.TextView(appContext);
        tvClickTip.setText("点击后等待延时(ms)");
        tvClickTip.setTextSize(15);
        layout.addView(tvClickTip);

        final android.widget.EditText etClickTime = new android.widget.EditText(appContext);
        etClickTime.setText(String.valueOf(SetingTheParmer.clickTime));
        layout.addView(etClickTime);

        builder.setView(layout);

        android.app.AlertDialog dialog = builder.create();
        // 关键：弹窗设置悬浮窗类型，脱离Activity依赖
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.type = type;
        dialog.getWindow().setAttributes(lp);

        dialog.setButton(android.app.Dialog.BUTTON_POSITIVE, "保存并修改", (d, which) -> {
            try{
                int allT = Integer.parseInt(etAllTime.getText().toString().trim());
                int clickT = Integer.parseInt(etClickTime.getText().toString().trim());
                SetingTheParmer.clickTime = clickT;
                SetingTheParmer.allTheardTime = 1000/allT;
                SetingTheParmer.saveFile(appContext);
                setTipsText("参数已保存");
            }catch (NumberFormatException e){
                setTipsText("参数格式错误！");
            }
        });
        dialog.setButton(android.app.Dialog.BUTTON_NEGATIVE, "取消", (d, which)->{});
        dialog.show();
    }

    private View selectOverlay;

    private WindowManager.LayoutParams selectLp;
    private float selStartX,selStartY,selEndX,selEndY;
    //回调接口
    public interface SelectCallback{
        void onSelect(int left,int top,int right,int bottom);
    }
    //临时保存当前等待回调
    private SelectCallback tempCb;

    /**
     * 外部/本类任意位置调用：唤起框选
     * @param callback 框选确定后返回坐标
     */
    public void showSelectOverlay(SelectCallback callback){
        if(selectOverlay!=null && selectOverlay.getParent()!=null) return;
        //接收本次回调
        this.tempCb = callback;

        //初始化坐标=0，默认全屏灰色
        selStartX = 0;
        selStartY = 0;
        selEndX = 0;
        selEndY = 0;

        // 1、拿到设备物理全屏尺寸，彻底避开系统裁切
        Point realScreen = new Point();
        mWindowManager.getDefaultDisplay().getRealSize(realScreen);

//final int SCREEN_W = realScreen.y;
//final int SCREEN_H = realScreen.x;
        // 平板适配：全局对调宽高，统一坐标系
        int[] screenSize = new int[]{realScreen.x, realScreen.y};

        final int[] WH = screenSize;

        selectOverlay = new View(appContext){
            final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                maskPaint.setColor(0x99000000);
                linePaint.setColor(Color.WHITE);
                linePaint.setStrokeWidth(3f);
                linePaint.setStyle(Paint.Style.STROKE);
            }

            // 强制View测量尺寸 = 真实物理屏幕
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(WH[0], WH[1]);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int l = (int)Math.min(selStartX,selEndX);
                int t = (int)Math.min(selStartY,selEndY);
                int r = (int)Math.max(selStartX,selEndX);
                int b = (int)Math.max(selStartY,selEndY);

                Path path = new Path();
                // 固定真实屏幕宽高绘制，不再依赖getWidth()
                path.addRect(0,0,WH[0],WH[1],Path.Direction.CW);
                Path rectPath = new Path();
                rectPath.addRect(l,t,r,b,Path.Direction.CW);
                path.op(rectPath, Path.Op.DIFFERENCE);
                canvas.drawPath(path,maskPaint);
                canvas.drawRect(l,t,r,b,linePaint);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                // 使用屏幕绝对坐标，框选坐标和画面坐标完全一致无偏移
                float x = ev.getRawX();
                float y = ev.getRawY();
                switch (ev.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        selStartX=x;selStartY=y;selEndX=x;selEndY=y;
                        invalidate();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        selEndX=x;selEndY=y;
                        invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        int l = (int)Math.min(selStartX,selEndX);
                        int t = (int)Math.min(selStartY,selEndY);
                        int r = (int)Math.max(selStartX,selEndX);
                        int b = (int)Math.max(selStartY,selEndY);

                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(appContext);
                        builder.setTitle("确认选区？");
                        //确定：先关弹窗、移除遮罩，延时再回调，解决截图遮挡
                        builder.setPositiveButton("确定",(di,w)->{
                            ((android.app.AlertDialog)di).dismiss();
                            if(selectOverlay != null && selectOverlay.getParent() != null){
                                mWindowManager.removeView(selectOverlay);
                            }
                            selectOverlay = null;
                            new android.os.Handler().postDelayed(()->{
                                if(tempCb != null){
                                    tempCb.onSelect(l,t,r,b);
                                }
                                tempCb = null;
                            },80);
                        });
                        //重选：坐标归零+刷新=变回全屏灰色
                        builder.setNegativeButton("重选",(di,w)->{
                            selStartX = 0;
                            selStartY = 0;
                            selEndX = 0;
                            selEndY = 0;
                            invalidate();
                        });
                        builder.setNeutralButton("翻转画布", (di,w)->{
                            int tmp = WH[0];
                            WH[0] = WH[1];
                            WH[1] = tmp;
                            //同步修改悬浮窗实际窗口尺寸，关键修复
                            selectLp.width = WH[0];
                            selectLp.height = WH[1];
                            mWindowManager.updateViewLayout(selectOverlay, selectLp);

                            selStartX=selStartY=selEndX=selEndY=0;
                            invalidate();
                        });




                        android.app.AlertDialog dialog = builder.create();

                        int dialogType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                : WindowManager.LayoutParams.TYPE_PHONE;
                        WindowManager.LayoutParams dLp = dialog.getWindow().getAttributes();
                        dLp.type = dialogType;
                        dialog.getWindow().setAttributes(dLp);
                        dialog.show();
                        break;
                }
                return true;
            }
        };

        selectLp = new WindowManager.LayoutParams();
        int type = Build.VERSION.SDK_INT>=Build.VERSION_CODES.O
                ?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                :WindowManager.LayoutParams.TYPE_PHONE;
        selectLp.type=type;
        // 不再MATCH_PARENT，直接硬编码真实屏幕尺寸
        selectLp.width = WH[0];
        selectLp.height = WH[1];

        selectLp.x = 0;
        selectLp.y = 0;
        // 全量全屏flag突破系统裁切
        selectLp.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

        selectLp.format=PixelFormat.TRANSLUCENT;
        selectLp.gravity=Gravity.TOP|Gravity.LEFT;

        mWindowManager.addView(selectOverlay,selectLp);
    }

    //废弃旧的无参方法，删除原来 public void showSelectOverlay(){}
public Point[] selectArea() throws InterruptedException {
    Object lock = new Object();
    Point lt = new Point();
    Point rb = new Point();
    //主线程拉起框选UI
    appContext.getMainExecutor().execute(()-> showSelectOverlay((l,t,r,b)->{
        lt.set(l,t);
        rb.set(r,b);
        synchronized (lock){
            lock.notifyAll();
        }
    }));
    //当前调用线程阻塞等待手动确定
    synchronized (lock){
        lock.wait();
    }
    return new Point[]{lt,rb};
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

    public void  setTipsText(String text) {
        if (mTipsText == null || text == null) {
            return;
        }
        // 抛到主线程更新UI
        mTipsText.post(() -> mTipsText.setText(text));
    }
    public static boolean xmlState = false;
    public static Point[] pointsLTRD= new  Point[2];
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


                if(StrogeXml.readPoint(context)[0][0] == 0) {
                    xmlState = true;
                }
                else {
                    int point[][] = StrogeXml.readPoint(context);
                    MainFunction.hookPoint = new Point(point[0][0],point[0][1]);
                    MainFunction.fishStaPoint = new Point(point[1][0],point[1][1]);
                    MainFunction.cGLinepoint = new Point( point[2][0],point[2][1]);
                    xmlState =false;

                }

                if(SetingTheParmer.useLeftRightControl == 1)
                {
                    if(StrogeXml.readLeftRightPos(context)[0][0]!= 0) {
                        MainFunction.controlPos = StrogeXml.readLeftRightPos(context);
                    }
                }
            //    showSelectOverlay();
              /*  showSelectOverlay((left,top,right,bottom)->{
                    int w = right-left;
                    int h = bottom-top;
                    if(w>0&&h>0){
                        setTipsText("框选："+left+","+top+","+right+","+bottom);
                        // 截图、保存xml代码放这里
                    }
                });*/


               // Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(2827,1845,2899-2827,1932-1845);
               /* Bitmap fisj = MainActivity.imageHadle.getAreaBitmap(815,89,904-815,161-89);
                Mat mat =  ImageHadle.binarizeToMat(fisj,140);
                MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(mat));
             */ /* showSelectOverlay((left,top,right,bottom)->{
                    int w = right-left;
                    int h = bottom-top;
                    if(w>0&&h>0){
                        setTipsText("框选："+left+","+top+","+right+","+bottom +"xxx"+MainFunction.bigHook.width());
                        // 截图、保存xml代码放这里
                        pointsLTRD[0].x = left;
                        pointsLTRD[0].y = top;
                        pointsLTRD[1].x = right;
                        pointsLTRD[1].y = bottom;

                        *//*    Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(target.x,target.y,MainFunction.bigHook.width(),MainFunction.bigHook.height());
                        MainActivity.imageHadle.saveBitmap(bitmap);
*//*
                    }
                });*/

                //平板调试
           /*     new Thread(()->{
                    try {
                        if(SetingTheParmer.stateDeviceModel  ==3){//框选
                            setTipsText("请框选钩子");
                            LogSaveUtil.saveLog("【步骤1】开始执行框选钩子流程");

                            Point[] area = selectArea();
                            String areaLog = "【步骤3】框选完成，赋值坐标：l="+area[0].x+","+area[0].y+" r="+area[1].x+","+area[1].y;
                            setTipsText(areaLog);
                            LogSaveUtil.saveLogWithLine(areaLog);
                           // sleep(3000);

                            pointsLTRD[0] = area[0];
                            pointsLTRD[1] = area[1];
                            MainActivity.imageHadle.saveBitmap(MainActivity.imageHadle.getAreaBitmap(pointsLTRD[0].x,pointsLTRD[0].y,
                                                                                    pointsLTRD[1].x-pointsLTRD[0].x,pointsLTRD[1].y-pointsLTRD[0].y));
                           // sleep(1000);
                            setTipsText("搜索中()");
                            LogSaveUtil.saveLog("【步骤4】进入图像匹配逻辑");

                            Point leftTop = pointsLTRD[0];
                            Point rightDown = pointsLTRD[1];
                            Mat mat = MainFunction.bigHook;

                            // 修复：矫正反向框选
                            int realL = Math.min(leftTop.x, rightDown.x);
                            int realT = Math.min(leftTop.y, rightDown.y);
                            int realR = Math.max(leftTop.x, rightDown.x);
                            int realB = Math.max(leftTop.y, rightDown.y);
                            leftTop.set(realL, realT);
                            rightDown.set(realR, realB);
                            LogSaveUtil.saveLog("矫正后选区：L="+realL+" T="+realT+" R="+realR+" B="+realB);

                            setTipsText("【步骤6】开始获取全屏截图");
                            Bitmap allScreen = MainActivity.imageHadle.getScreenBitmap();
                            if (allScreen == null) {
                                String nullLog = "【错误】获取全屏截图allScreen为空，直接终止匹配";
                                setTipsText(nullLog);
                                LogSaveUtil.saveLogWithLine(nullLog);
                               // sleep(5000);
                                return;
                            }
                            String screenInfo = "【步骤7】截图获取成功，屏幕宽="+allScreen.getWidth()+" 高="+allScreen.getHeight();
                            setTipsText(screenInfo);
                            LogSaveUtil.saveLogWithLine(screenInfo);
                            //sleep(5000);

                            Mat movemat;
                            Bitmap areaBmp;
                            float k = 0,b =0;
                            int totalCol = rightDown.x-leftTop.x - mat.cols();
                            int totalRow = rightDown.y-leftTop.y - mat.rows();

                            // 框选区域小于模板，直接退出
                            if(totalCol <= 0 || totalRow <= 0){
                                String smallLog = "【参数】框选区域小于模板尺寸，无法遍历匹配";
                                setTipsText(smallLog);
                                LogSaveUtil.saveLogWithLine(smallLog);
                               // sleep(3000);
                                if (allScreen != null) allScreen.recycle();
                                return;
                            }

                            String paramLog = "【参数】总遍历列:"+totalCol+" 总遍历行:"+totalRow+" 模板宽:"+mat.cols()+" 模板高:"+mat.rows();
                            setTipsText(paramLog);
                            LogSaveUtil.saveLogWithLine(paramLog);
                            sleep(2000);

                            try{
                                for (int co = 0;co < totalCol;co++){
                                    for (int ro = 0;ro < totalRow;ro++){
                                        int currX = leftTop.x + co;
                                        int currY = leftTop.y + ro;
                                        int cutW = mat.cols();
                                        int cutH = mat.rows();
                                        int screenW = allScreen.getWidth();
                                        int screenH = allScreen.getHeight();

                                 *//*       String posLog = "遍历位置：co="+co+" ro="+ro+" X="+currX+" Y="+currY;
                                        setTipsText(posLog);
                                        LogSaveUtil.saveLogWithLine(posLog);
                                   *//*   //  sleep(8000);

                                        // 边界校验：防止Bitmap.createBitmap越界崩溃
                                        if(currX < 0 || currY < 0 || (currX + cutW) > screenW || (currY + cutH) > screenH){
                                            String skipLog = "坐标越界跳过 X="+currX+" Y="+currY +"ScreenW = "+screenW+"ScreenY"+ screenH;
                                            setTipsText(skipLog);
                                            LogSaveUtil.saveLogWithLine(skipLog);
                                            sleep(1000);
                                            continue;
                                        }

                                        // 单独捕获裁切异常
                                        try {
                                            areaBmp = Bitmap.createBitmap(allScreen, currX,currY, cutW,cutH);
                                        } catch (IllegalArgumentException ex) {
                                            String errLog = "Bitmap裁切参数异常 X="+currX+" Y="+currY;
                                            setTipsText(errLog);
                                            LogSaveUtil.saveLogWithLine(errLog);
                                            LogSaveUtil.saveException(ex);
                                           // sleep(3000);
                                            continue;
                                        }

                                        if(areaBmp == null)
                                        {
                                            setTipsText("area为空");
                                            LogSaveUtil.saveLogWithLine("areaBmp 创建返回null");
                                           // sleep(3000);
                                        }else {
                                        *//*    setTipsText("area不为空");
                                            MainActivity.imageHadle.saveBitmap(areaBmp);
                                            LogSaveUtil.saveLogWithLine("areaBmp 创建成功");
                                       *//*    // sleep(3000);
                                        }

                                        movemat =  ImageHadle.binarizeToMat(areaBmp,140);
                                        b = ImageHadle.matchSimilarity(movemat,mat);
                                        LogSaveUtil.saveLog("当前相似度 = " + String.format("%.2f",k));
                                      //  sleep();

                                        if(k < b){
                                            k= b;
                                            setTipsText("刷新最大相似度："+String.format("%.2f",k));
                                            LogSaveUtil.saveLog("当前最大相似度 = " + String.format("%.2f",k));
                                     //  sleep(1000);
                                        }

                                        if( b>0.6){
                                            String okLog = "✅匹配成功！相似度"+String.format("%.2f",b)+" 坐标"+currX+","+currY;
                                            sleep(5000);
                                            setTipsText(okLog);
                                            LogSaveUtil.saveLogWithLine(okLog);

                                            areaBmp = Bitmap.createBitmap(allScreen, currX,currY, mat.cols(),mat.rows());
                                            MainActivity.imageHadle.saveBitmap(areaBmp);
                                            MainFunction.hookPoint = new Point(currX,currY);
                                           // sleep(2000);
                                        }

                                        // 资源释放
                                        if (areaBmp != null) {
                                            areaBmp.recycle();
                                            areaBmp = null;
                                        }
                                        if (movemat != null) {
                                            movemat.release();
                                            movemat = null;
                                        }
                                    }
                                }
                            } finally {
                                if (allScreen != null) {
                                    allScreen.recycle();
                                    String endLog = "【收尾】全屏Bitmap已回收，本次最大相似度："+String.format("%.2f",k);
                                    sleep(10000);
                                    setTipsText(endLog);
                                    LogSaveUtil.saveLogWithLine(endLog);
                                }
                                if(MainFunction.hookPoint==null){
                                    setTipsText("【结束】全区域遍历完毕，无匹配项");
                                    LogSaveUtil.saveLog("遍历结束：未找到匹配目标");
                                }else{
                                    setTipsText("【结束】已成功保存钩子坐标");
                                    LogSaveUtil.saveLog("遍历结束：成功匹配到目标");
                                }
                            }
                        }
                    }catch (Exception e){
                        String crashLog = "线程全局异常崩溃";
                        setTipsText(crashLog);
                        LogSaveUtil.saveLogWithLine(crashLog);
                        LogSaveUtil.saveException(e);
                    }
                }).start();

             */  /* pointsLTRD = selectArea();

                MainActivity.imageHadle.saveBitmap(MainActivity.imageHadle.getScreenBitmap());
                Point target = ImageHadle.cutSelf(MainFunction.bigHook,pointsLTRD[0],pointsLTRD[1],140);
                Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(target.x,target.y,MainFunction.bigHook.width(),MainFunction.bigHook.height());
                MainActivity.imageHadle.saveBitmap(bitmap);*/
              /*  Bitmap bitmap1 = ImageHadle.binaryzationToBit(bitmap,140);
                MainActivity.imageHadle.saveBitmap(bitmap1);
                */
    /*            Bitmap all = MainActivity.imageHadle.getScreenBitmap();
                MainFunction.initSearchTheGreenPosHsv(all);
                int wid = ImageHadle.width - (2*MainFunction.cGLinepoint.x);
                //xToTrsf(815), yToTrsf(86), xToTrsf(785), yToTrsf(5)
                Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(MainFunction.fishStaPoint.x, MainFunction.fishStaPoint.y, wid ,40);
                MainActivity.imageHadle.saveBitmap(areaBitmap);

                if (areaBitmap == null)
                {

                }else {
                    Mat mat = ImageHadle.bitmapToBGRMat(areaBitmap);
                    //定义绿色范围
                    Scalar lowerYellow = new Scalar(80,30,240);
                    Scalar upperYellow = new Scalar(100,255,255);
                    Mat yellowMask = ImageHadle.filterByHSV(mat, lowerYellow, upperYellow);
                    MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(yellowMask));
                    Scalar lowerGreen = new Scalar(35,70,220);
                    Scalar upperGreen = new Scalar(75,255,255);
                    Mat greenMask = ImageHadle.filterByHSV(mat, lowerGreen, upperGreen);
                    MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(greenMask));

                }
*/





           /*     Bitmap all = MainActivity.imageHadle.getScreenBitmap();
                MainFunction.initSearchTheGreenPosHsv(all);
                Log.i("test", MainFunction.cGLinepoint.y +" "+ String.valueOf(MainFunction.cGLinepoint.x));
      */     /*     Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(MainFunction.cGLinepoint.x, MainFunction.cGLinepoint.y, MainFunction.sWToTrsf(785), MainFunction.sizdToTrsf(5));
                if (areaBitmap == null){

                }else {
                    MainActivity.imageHadle.saveBitmap(areaBitmap);
                    Mat mat = ImageHadle.twoBinarizeToMat(areaBitmap, 160,177);
                    MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(mat));
                }

*/
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

                                    if(SetingTheParmer.stateDeviceModel == 1) {//手机
                                        MainFunction.hookPoint = ImageHadle.uiLineSearch(MainFunction.hook,new Point((int)((0.88125*ImageHadle.width) +10.5),(int)(0.885*ImageHadle.height)),
                                                57*ImageHadle.height/1080*2,200);
                                            if(MainFunction.hookPoint == null)
                                            {
                                                setTipsText("这一轮失败2s后进入下一轮");
                                                sleep(1000);
                                                setTipsText("..扫描钩子(耐心等待)");
                                            }
                                    }else if(SetingTheParmer.stateDeviceModel  == 2){//云异环

                                            MainFunction.hookPoint= ImageHadle.yunuiLineSearch(MainFunction.hook,new Point((int)((0.88125*ImageHadle.width) +10.5),(int)(0.885*ImageHadle.height)),
                                                    57*ImageHadle.height/1080*2,200);//云扩大范围
                                            if(MainFunction.hookPoint == null)
                                            {
                                                setTipsText("这一轮失败2s后进入下一轮");
                                                sleep(2000);
                                                setTipsText("..扫描钩子(耐心等待)");
                                            }
                                    }
                                    else if(SetingTheParmer.stateDeviceModel  ==3){//框选
                                            setTipsText("请框选钩子");
                                            Point[] area = selectArea();
                                            pointsLTRD[0] = area[0];
                                            pointsLTRD[1] = area[1];
                                            sleep(1000);
                                            setTipsText("搜索中()");
                                            MainFunction.hookPoint = ImageHadle.cutSelf(MainFunction.bigHook,pointsLTRD[0],pointsLTRD[1],200);

                                            if (MainFunction.hookPoint == null)//小钩子
                                            {
                                                MainFunction.hookPoint = ImageHadle.cutSelf(MainFunction.hook,pointsLTRD[0],pointsLTRD[1],200);
                                            }
                                        if (MainFunction.hookPoint == null)
                                        {
                                            setTipsText("未找到请重新框选");
                                            sleep(1000);

                                            }


                                    }
                                       }

                                    boolean stateWithserch = true;
                                    setTipsText("扫到钩子，开始点击");

                                sleep(500);
                                    while(stateWithserch)
                                    {
                                        clickStablePostion();
                                        sleep(SetingTheParmer.clickTime);
                                        if(MainFunction.isHook() == false){
                                            setTipsText("扫描鱼标");
                                            if(SetingTheParmer.stateDeviceModel ==1) {//手机

                                                MainFunction.fishStaPoint = ImageHadle.uiLineSearch(MainFunction.fishsate,
                                                        new Point((int)((0.3324*ImageHadle.width)-81.43),(int)(0.085*ImageHadle.height)),
                                                        60*ImageHadle.height/1080*2,140);

                                            }else if(SetingTheParmer.stateDeviceModel  == 2){//云

                                                MainFunction.fishStaPoint = ImageHadle.yunuiLineSearch(MainFunction.fishsate,
                                                        new Point((int)((0.3324*ImageHadle.width)-81.43),(int)(0.085*ImageHadle.height)),
                                                        60*ImageHadle.height/1080*2,140);

                                             //  MainFunction.initSearchTheGreenPosHsv(all);
                                            }
                                            else if(SetingTheParmer.stateDeviceModel  ==3){//框选
                                                sleep(1000);
                                                if(MainFunction.isHook() == false)
                                                {
                                                    setTipsText("请框选鱼标");
                                                    Point[] area = selectArea();
                                                    pointsLTRD[0] = area[0];
                                                    pointsLTRD[1] = area[1];
                                                    sleep(1000);
                                                    setTipsText("搜索中()");
                                                    MainFunction.fishStaPoint = ImageHadle.cutSelf(MainFunction.bigFish,pointsLTRD[0],pointsLTRD[1],140);
                                                    if (MainFunction.fishStaPoint == null)
                                                    {
                                                        MainFunction.fishStaPoint = ImageHadle.cutSelf(MainFunction.fishsate,pointsLTRD[0],pointsLTRD[1],140);

                                                    }
                                                    if (MainFunction.fishStaPoint == null)
                                                    {
                                                        setTipsText("未找到请重新框选");
                                                        sleep(1000);
                                                    }
                                                }



                                            }
                                           if(MainFunction.fishStaPoint != null)
                                            {
                                           //     MainFunction.cGLinepoint = new Point( MainFunction.fishStaPoint.x +MainFunction.sWToTrsf(119),
                                                   //     MainFunction.fishStaPoint.y + getGreenSizPo());
                                                if(MainFunction.isFishStae()){
                                                    Bitmap all = MainActivity.imageHadle.getScreenBitmap();
                                                    MainFunction.initSearchTheGreenPosHsv(all);
                                                    all.recycle();
                                                }


                                                StrogeXml.writePoint(context,MainFunction.hookPoint.x,MainFunction.hookPoint.y,
                                                                        MainFunction.fishStaPoint.x,MainFunction.fishStaPoint.y,
                                                                        MainFunction.cGLinepoint.x,MainFunction.cGLinepoint.y);
                                                setTipsText("ok所有图标均已扫完");
                                                sleep(500);

                                                if(SetingTheParmer.useLeftRightControl == 1)
                                                {
                                                    int[][] controlPos = new int[2][2];

                                                    setTipsText("框选俩控制，注意只是获取其坐标，不比对");
                                                    sleep(2000);
                                                    setTipsText("获取第一个");
                                                    Point[] area = selectArea();
                                                    Point point1 = new Point( (area[1].x + area[0].x)/2,(area[1].y+area[0].y)/2);
                                                    setTipsText("获取第二个");
                                                    Point[] area2 = selectArea();
                                                    Point point2 = new Point( (area2[1].x + area2[0].x)/2,(area2[1].y+area2[0].y)/2);
                                                    //判断左右
                                                    if(point1.x < point2.x)//1在左
                                                    {
                                                        controlPos[0][0] = point1.x;
                                                        controlPos[0][1] = point1.y;
                                                        controlPos[1][0] = point2.x;
                                                        controlPos[1][1] = point2.y;

                                                      }else{//1在右边
                                                        controlPos[0][0] = point2.x;
                                                        controlPos[0][1] = point2.y;
                                                        controlPos[1][0] = point1.x;
                                                        controlPos[1][1] = point1.y;
                                                    }
                                                    MainFunction.controlPos = controlPos;
                                                    StrogeXml.writeLeftRightPos(context,controlPos[0][0],controlPos[0][1],  controlPos[1][0], controlPos[1][1]);

                                                }

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
                                sleep(SetingTheParmer.allTheardTime);//防止太快出bug
                                switch(mainState)
                                {

                                    case 0:

                                        if(MainFunction.isHook()) {
                                            //Log.d(TAG, "检测到钩子，执行点击");
                                            clickStablePostion();
                                            sleep(SetingTheParmer.clickTime);
                                        }else if(MainFunction.isFishStae()){

                                            mainState = 1;

                                        }else {

                                            clickStablePostion();
                                            sleep(SetingTheParmer.clickTime);
                                        }
                                        break;
                                    case 1:

                                        if(MainFunction.isFishStae()){

                                            MainFunction.contralCurr();
                                        }
                                        else {

                                            clickStablePostion();
                                            sleep(SetingTheParmer.clickTime);
                                            if (MainFunction.isHook()) {
                                                MainFunction.weithState = true;
                                                mainState = 0;
                                                fishNum++;
                                                setTipsText("已上鱼："+ fishNum);
                                            }
                                        }
                                        break;
                                }
                            }
                            Log.i(TAG, "钓鱼线程正常退出循环");
                        } catch (Exception e) {
                            setTipsText("钓鱼线程异常崩溃");
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
        //长按弹出框选

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




