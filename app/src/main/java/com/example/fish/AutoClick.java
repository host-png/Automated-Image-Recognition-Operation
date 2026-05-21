package com.example.fish;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class AutoClick extends AccessibilityService {
    public static AutoClick service;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }
    @Override
    public void onInterrupt() {

    }
    @Override
    public void onCreate() {//为了外部能调用,无障碍开启后自动执行
        super.onCreate();
        service = this;
    }
    // 坐标点击
    public  void click(int x, int y ,int satrTime,int delayTime) {//xy位置 开始按下时间 按下停留时间
        if (Build.VERSION.SDK_INT >= 24) {
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();//手指构造
            builder.addStroke(new GestureDescription.StrokeDescription(path, satrTime, delayTime));//1点击位置2开始时间3持续时间
            dispatchGesture(builder.build(), null, null);//发送执行手势
        }
    }


}
