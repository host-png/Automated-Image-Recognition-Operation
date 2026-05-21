package com.example.fish;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ImageHadle {
    private int width, height, dpi;//屏幕信息

    private ImageReader mImageReader;//图像接受对象
    private boolean isInitialized = false;
    public ImageHadle(int width, int height, int dpi) {//横屏交换 因为游戏是横平，后续不好改
        this.width = height;
        this.height = width;
        this.dpi = dpi;
    }

    // 授权后 初始化通道（只建通道，不读取）
    private MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            if(mImageReader != null){
                mImageReader.close();
                mImageReader = null;
            }
        }
    };

    // 初始化截图（直接用服务里的 MediaProjection）
    public void init() {
        if (isInitialized) {
            return; // 已经初始化过，直接返回，不重复创建！
        }
        // 强制注册回调，修复当前崩溃
        ScreenRecordService.mMediaProjection.registerCallback(mProjectionCallback, null);


        if (mImageReader == null) {// 创建图像接收器
            mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        }

        ScreenRecordService.mMediaProjection.createVirtualDisplay( // 创建虚拟屏幕（画面开始流入，但你不读就不消耗性能）
                "capture",
                width, height, dpi,
                0,
                mImageReader.getSurface(),
                null, null
        );
        isInitialized = true; // 标记已初始化
    }

    public Bitmap getScreenBitmap() { //获取一帧的图片信息
        if (mImageReader == null) return null;

        Image image = mImageReader.acquireLatestImage();
        if (image == null) return null;

        Bitmap bitmap = imageToBitmap(image);
        image.close();
        return bitmap;
    }
    // 图片转Bitmap（固定工具方法）
    private Bitmap imageToBitmap(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();

        // 算出内存真实宽度
        int realW = plane.getRowStride() / plane.getPixelStride();

        // 先读满内存数据
        Bitmap temp = Bitmap.createBitmap(realW, image.getHeight(), Bitmap.Config.ARGB_8888);
        temp.copyPixelsFromBuffer(buffer);

        // 裁掉多余空白，画面就正了
        Bitmap correct = Bitmap.createBitmap(temp, 0, 0, image.getWidth(), image.getHeight());
        temp.recycle();
        return correct;
    }




    /*
      截取指定区域画面
      x起始X坐标
      y 起始Y坐标
     width 截取宽度
     height 截取高度
     区域Bitmap
     */
    public Bitmap getAreaBitmap(int x, int y, int width, int height) {//XY左上角
        Bitmap fullBmp = getScreenBitmap();
        if (fullBmp == null) return null;
        // 边界防越界
        x = Math.max(0, x);//小于0变0
        y = Math.max(0, y);//小于0变0
        width = Math.min(width, fullBmp.getWidth() - x);//如果越界了就取没越的部分
        height = Math.min(height, fullBmp.getHeight() - y);
        // 裁剪
        Bitmap areaBmp = Bitmap.createBitmap(fullBmp, x, y, width, height);
        // 回收原图节省内存
        fullBmp.recycle();
        return areaBmp;
    }


    // 直接获取单个坐标颜色值
    public int getPointColor(int x, int y) {
        Bitmap bmp = getScreenBitmap();
        if(bmp == null) return -1;
        int color = bmp.getPixel(x,y);
        bmp.recycle();
        return color;
    }

    /*

    输入mat
    俩个坐标，
    输出颜色
     */
    public static int getPointColorFromMat(Mat mat, int x, int y) {
        // 越界判断
        if (mat == null || mat.empty() || x < 0 || y < 0 || x >= mat.cols() || y >= mat.rows()) {
            return -1;
        }

        // 读取像素（OpenCV 顺序：y, x）
        double[] pixels = mat.get(y, x);
        if (pixels == null) {
            return -1;
        }

        // 单通道灰度图 → 直接取第一个值
        int gray = (int) Math.max(0, Math.min(255, pixels[0]));

        // 灰度转颜色：黑/白
        if (gray == 255) {
            return 0xFFFFFFFF;  // 白色
        } else if (gray == 0) {
            return 0xFF000000;  // 黑色
        }

        // 其他灰度值
        return 0xFF000000 | (gray << 16) | (gray << 8) | gray;
    }


    // 保存截图到本地 格式PNG
    // 保存 Bitmap 到手机图片
    // 保存图片到相册（不刷新相册，绝对不崩）
    public String saveBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            java.io.File folder = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
            if (!folder.exists()) folder.mkdirs();

            String name = "screen_" + System.currentTimeMillis() + ".png";
            java.io.File file = new java.io.File(folder, name);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();


            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /*
     * 从文件读取png，直接返回 Mat（灰度图，方便匹配）
     * filePath 模板图片的路径
     * 灰度模板 Mat
     */
    public static Mat pngLoadTemplateMat(String filePath) {
        try {
            // ==============================================
            // 关键：用 FileInputStream 强行读取，绕过系统解码限制
            // ==============================================
            InputStream is = new FileInputStream(filePath);
            Bitmap templateBmp = BitmapFactory.decodeStream(is);
            is.close();

            if (templateBmp == null) {
                return null;
            }

            // 转 Mat
            Mat templateMat = bitmapToMat(templateBmp);
            Mat grayTemplate = new Mat();
            Imgproc.cvtColor(templateMat, grayTemplate, Imgproc.COLOR_RGBA2GRAY);

            templateMat.release();
            templateBmp.recycle();

            return grayTemplate;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 从raw资源读取模板灰度Mat
    public static Mat loadRawTemplate(Context context, int resId){
        try{
            InputStream is = context.getResources().openRawResource(resId);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if(bmp == null) return new Mat();

            Mat srcMat = bitmapToMat(bmp);
            Mat grayMat = new Mat();
            Imgproc.cvtColor(srcMat,grayMat,Imgproc.COLOR_RGBA2GRAY);

            srcMat.release();
            bmp.recycle();
            return grayMat;
        }catch (Exception e){
            e.printStackTrace();
            return new Mat();
        }
    }

    //opencv下面是
    // Bitmap转Mat
    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), org.opencv.core.CvType.CV_8UC4);
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        mat.put(0, 0, buffer.array());

        // 关键：转换通道顺序，解决花屏
        Mat matRGBA = new Mat();
        Imgproc.cvtColor(mat, matRGBA, Imgproc.COLOR_BGRA2RGBA);

        mat.release(); // 释放中间Mat
        return matRGBA;
    }


    // Mat转Bitmap
    public static Bitmap matToBitmap(Mat mat) {
        // 如果是RGBA格式，转回ARGB给Bitmap
        Mat matARGB = new Mat();
        Imgproc.cvtColor(mat, matARGB, Imgproc.COLOR_RGBA2BGRA);

        Bitmap bitmap = Bitmap.createBitmap(matARGB.cols(), matARGB.rows(), Bitmap.Config.ARGB_8888);
        byte[] bytes = new byte[matARGB.cols() * matARGB.rows() * 4];
        matARGB.get(0, 0, bytes);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes));

        matARGB.release();
        return bitmap;
    }


    /* 输入bit返回bit灰度图
     * 图像二值化
     * src 原图Bitmap
     * thresh 阈值 0~255 推荐90-130
     * 返回黑白二值图
     */
    public static Bitmap binaryzationToBit(Bitmap src, int thresh) {
        Mat srcMat = bitmapToMat(src);
        Mat grayMat = new Mat();
        Mat binMat = new Mat();

        // 1. 4通道RGBA转单通道灰度图
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
        // 2. 二值化：大于阈值变白，小于变黑
        Imgproc.threshold(grayMat, binMat, thresh, 255, Imgproc.THRESH_BINARY);

        Bitmap result = matToBitmap(binMat);

        // 释放内存
        srcMat.release();
        grayMat.release();
        binMat.release();
        return result;
    }

    //双阈值法
    public static Bitmap twoOinaryzationToBit(Bitmap src, int minThresh,int maxThresh) {//取中间为白色其他为黑色
        Mat srcMat = bitmapToMat(src);
        Mat grayMat = new Mat();
        Mat binMat = new Mat();

        // 1. 4通道RGBA转单通道灰度图
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

      //  Imgproc.threshold(grayMat, binMat, minThresh, 255, Imgproc.THRESH_TOZERO);
         Core.inRange(
                grayMat,
                new Scalar(minThresh),   // 最小值
                new Scalar(maxThresh),   // 最大值
                 binMat
        );
        Bitmap result = matToBitmap(binMat);


        // 释放内存
        srcMat.release();
        grayMat.release();
        binMat.release();
        return result;
    }


    //输入bit输出mat灰度图
    public static Mat binarizeToMat(Bitmap src, int thresh) {//外用完友爱释放
        Mat srcMat = bitmapToMat(src);
        Mat grayMat = new Mat();
        Mat binMat = new Mat();

        // 1. 4通道RGBA转单通道灰度图
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
        // 2. 二值化：大于阈值变白，小于变黑
        Imgproc.threshold(grayMat, binMat, thresh, 255, Imgproc.THRESH_BINARY);

        // 注意：
        // binMat 要返回出去给外面用，所以这里不能 release！
        srcMat.release();
        grayMat.release();

        return binMat;
    }

    //双阈值mat
    public static Mat twoBinarizeToMat(Bitmap src, int minThresh,int maxThresh) {//取中间为白色其他为黑色
        Mat srcMat = bitmapToMat(src);
        Mat grayMat = new Mat();
        Mat binMat = new Mat();

        // 1. 4通道RGBA转单通道灰度图
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        //  Imgproc.threshold(grayMat, binMat, minThresh, 255, Imgproc.THRESH_TOZERO);
        Core.inRange(
                grayMat,
                new Scalar(minThresh),   // 最小值
                new Scalar(maxThresh),   // 最大值
                binMat
        );


        // 释放内存
        srcMat.release();
        grayMat.release();

        return binMat;
    }
    /* 输入俩灰度图输出相似度
     * 直接比较两个 Mat 的相似度
     * @param source  原图 Mat（必须是灰度图）
     * @param template 模板 Mat（必须是灰度图，尺寸更小）
     * @return 相似度 0 ~ 1，越接近1越像
     */
    public static float matchSimilarity(Mat source, Mat template) {
        Mat result = new Mat();

        // 执行模板匹配（最准的算法）
        Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED);

        // 获取最大匹配值
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        // 释放内存
        result.release();
        if((float) mmr.maxVal<0||(float) mmr.maxVal>=1)//1全黑纯色
        {
            return 0.0f;
        }
        return (float) mmr.maxVal;
    }
}
