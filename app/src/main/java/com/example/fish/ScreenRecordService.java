package com.example.fish;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class ScreenRecordService extends Service {
    private static final String CHANNEL_ID = "screen_record_channel";
    public static MediaProjection mMediaProjection;
    private static int mResultCode;
    private static Intent mResultData;

    // 外部传入授权结果
    public static void setAuthData(int code, Intent data) {
        mResultCode = code;
        mResultData = data;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // 启动前台服务
        startForeground(1, buildNotification());
        // 在服务里初始化录屏（不会崩溃）
        initMediaProjection();
    }

    private void initMediaProjection() {
        if (mMediaProjection == null && mResultData != null) {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mMediaProjection = manager.getMediaProjection(mResultCode, mResultData);
        }
        MainActivity.imageHadle.init();
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("屏幕录制服务运行中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "录屏服务", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
