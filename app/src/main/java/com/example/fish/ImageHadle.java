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
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.Mat;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import android.graphics.Point;
import java.util.ArrayList;
import java.util.List;

public class ImageHadle {
    public static int width, height, dpi;//屏幕信息

    private ImageReader mImageReader;//图像接受对象
    private boolean isInitialized = false;
    private final Object lock = new Object();
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

    public Bitmap getScreenBitmap() {
        synchronized (lock) {
            if (mImageReader == null) {
                return null;
            }

            Image image = null;
            Bitmap bitmap = null;
            try {
                // 串行取帧，不丢帧、不乱缓冲区状态
                image = mImageReader.acquireNextImage();
                if (image == null) {
                    return null;
                }
                bitmap = imageToBitmap(image);
            } catch (IllegalStateException e) {
                // 捕获缓冲区锁定/重复关闭异常
                Log.w("ImageHadle", "缓冲区状态异常，跳过当前帧");
                return null;
            } catch (Exception e) {
                Log.e("ImageHadle", "截图转换异常", e);
                return null;
            } finally {
                // 无论正常/异常，Image 必定关闭，杜绝泄漏
                if (image != null) {
                    image.close();
                }
            }
            return bitmap;
        }
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
    public Bitmap getAreaBitmap(Bitmap srcBitmap,int x, int y, int width, int height) {//XY左上角
        if (srcBitmap == null) return null;
        Bitmap fullBmp = srcBitmap;
        // 边界防越界
        x = Math.max(0, x);//小于0变0
        y = Math.max(0, y);//小于0变0
        width = Math.min(width, fullBmp.getWidth() - x);//如果越界了就取没越的部分
        height = Math.min(height, fullBmp.getHeight() - y);
        // 裁剪
        Bitmap areaBmp = Bitmap.createBitmap(fullBmp, x, y, width, height);
        // 外部传入的原图不在这里recycle，由调用方自己管理回收
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

    /**
     * 输入ARGB/Bitmap(RGBA内存排布) → 返回 BGR 3通道Mat
     */
    public static Mat bitmapToBGRMat(Bitmap srcBmp){
        // 先用你原有方法转 RGBA Mat
        Mat rgbaMat = bitmapToMat(srcBmp);
        Mat bgrMat = new Mat();
        // RGBA → BGR 标准转换
        Imgproc.cvtColor(rgbaMat, bgrMat, Imgproc.COLOR_RGBA2BGR);

        rgbaMat.release();//释放中间RGBA
        return bgrMat;
    }
    /**
     * BGR三通道Mat + HSV上下阈值 → 返回筛选掩码Mat(符合颜色=255白，其余=0黑)
     * @param bgrMat 输入：BGR格式3通道Mat
     * @param lower HSV下限Scalar
     * @param upper HSV上限Scalar
     * @return 单通道二值掩码Mat
     */
    public static Mat filterByHSV(Mat bgrMat, Scalar lower, Scalar upper){
        Mat hsv = new Mat();
        //BGR转HSV
        Imgproc.cvtColor(bgrMat, hsv, Imgproc.COLOR_BGR2HSV);

        Mat mask = new Mat();
        //区间筛选
        Core.inRange(hsv, lower, upper, mask);

        hsv.release();
        return mask;
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
        if (src == null) return new Mat();


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
    public static float matchSimilarity(Mat source, Mat template) {//截图  模板 别高反了
        Mat result = new Mat();
        if (source.rows() < template.rows() || source.cols() < template.cols()) {
            return 0.0f;
        }
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
    /*
     * OpenCV Mat 等比例缩放
     * @param src 原图Mat
     * @param targetW 目标宽度
     * @param targetH 目标高度
     * @return 缩放后新Mat
     */
    public static Mat scaleMat(Mat src, int targetW, int targetH) {
        Mat dst = new Mat();
        Imgproc.resize(src, dst, new Size(targetW, targetH));
        src.release(); // 释放原图内存
        return dst;
    }



    //传入比对图片(处理好的) 中心位置 扫描范围 灰度
    //输出比对图标的左上角坐标
    public static Point uiLineSearch(Mat mat,Point cenPos,int scope,int thresh){
        //Mat cutImage = bitmapToMat(MainActivity.imageHadle.getAreaBitmap(cenPos.x-(scope/2),cenPos.y-(scope/2),scope,scope));
        //县固定列扫描行数
        Log.d("cc", "mat宽度cols="+mat.cols()+" 高度rows="+mat.rows());
        Log.d("MatSizeCheck", "mat宽度cols=");

        Bitmap allScreen = MainActivity.imageHadle.getScreenBitmap();
        if (allScreen == null) {
            return null;
        }
        Mat movemat;
        Bitmap areaBmp;
        float a = 0;
        try{
        for (int co = 0;co<scope-mat.cols();co++)//列
        {
            for (int ro = 0;ro<scope-mat.rows();ro++)//行
            {

                areaBmp = Bitmap.createBitmap(allScreen, cenPos.x-(scope/2) + co,cenPos.y-(scope/2) + ro
                        , mat.cols(),mat.rows());
                movemat =  binarizeToMat(areaBmp,thresh);
                        //MainActivity.imageHadle.getAreaBitmap();

              //  Log.v("xianhgsoo", "fishstaok");

             /*   float b= matchSimilarity(movemat,mat);
                if(b>a){
                    Log.i("xd", String.valueOf(b));
                    a =b;
                }*/
                if( matchSimilarity(movemat,mat) >0.6)
               {
                   Point point = new Point(cenPos.x-(scope/2)+co,cenPos.y-(scope/2) +ro);
                   return point;
               }
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
        // 最终释放整屏图
        if (allScreen != null) {
            allScreen.recycle();
        }
    }
        return null;

    }

    public static Point uiLineSearch(Bitmap srcFull, Mat mat, Point cenPos, int scope, int thresh){
        if(srcFull == null) return null;
        Mat movemat;
        Bitmap areaBmp;
        try{
            int maxCo = scope - mat.cols();
            int maxRo = scope - mat.rows();
            for (int co = 0; co < maxCo; co++){
                for (int ro = 0; ro < maxRo; ro++){
                    int clipX = cenPos.x - (scope/2) + co;
                    int clipY = cenPos.y - (scope/2) + ro;
                    areaBmp = Bitmap.createBitmap(srcFull, clipX, clipY, mat.cols(), mat.rows());
                    movemat = binarizeToMat(areaBmp, thresh);

                    if( matchSimilarity(movemat,mat) > 0.6){
                        return new Point(clipX, clipY);
                    }
                    //释放
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
        }finally {
            //外部传入bitmap不由本方法回收，去掉allScreen.recycle()
        }
        return null;
    }
    public static Point yunuiLineSearch(Mat mat,Point cenPos,int scope,int thresh){

        scope*=SetingTheParmer.expendTheSechArea;
        // 1. 计算扫描区域左上角 & 做边界限制（核心修复：防止 x/y 负数）
        int scanLeft = cenPos.x - (scope / 2);
        int scanTop = cenPos.y - (scope / 2);

        int scanfRight = cenPos.x + (scope/2);
        int scanDown = cenPos.y + (scope/2);
        // 强制坐标 >= 0
        scanLeft = Math.max(0, scanLeft);
        scanTop = Math.max(0, scanTop);
        scanfRight = Math.min(width,scanfRight);
        scanDown=Math.min(height,scanDown);
    /*    Mat cutImage = bitmapToMat(MainActivity.imageHadle.getAreaBitmap(scanLeft,scanTop ,scanfRight-scanLeft,scanDown-scanTop));
        MainActivity.imageHadle.saveBitmap(matToBitmap(cutImage));
    */    //县固定列扫描行数
        Log.d("cc", "mat宽度cols="+mat.cols()+" 高度rows="+mat.rows());
        Log.d("MatSizeCheck", "mat宽度cols=");

        Bitmap allScreen = MainActivity.imageHadle.getScreenBitmap();
        if (allScreen == null) {
            return null;
        }

        Mat movemat;
        Bitmap areaBmp;
        float k = 0,b =0;
        try{
            for (int co = 0;co<scanfRight-scanLeft - mat.width();co++)//列
            {
                for (int ro = 0;ro<scanDown-scanTop - mat.height();ro++)//行
                {

                    areaBmp = Bitmap.createBitmap(allScreen, scanLeft + co,scanTop + ro
                            , mat.cols(),mat.rows());
                    movemat =  binarizeToMat(areaBmp,thresh);
                  /*  MainActivity.imageHadle.getAreaBitmap();

                     Log.v("xiangsi", String.valueOf(matchSimilarity(movemat,mat)));
*/
                /*    b =matchSimilarity(movemat,mat);
                 if(k < b)
                 {
                     k= b;
                     Log.v("xiangsi", String.valueOf(k));

                 }*/

                    if( matchSimilarity(movemat,mat)>0.6)
                    {
                        areaBmp = Bitmap.createBitmap(allScreen, scanLeft + co,scanTop + ro
                                , mat.cols(),mat.rows());
                     //   MainActivity.imageHadle.saveBitmap(areaBmp);

                        Point point = new Point(scanLeft+co,scanTop +ro);
                        return point;
                    }
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
            // 最终释放整屏图
            if (allScreen != null) {
                allScreen.recycle();
            }
        }
        return null;

    }

    public static Point cutSelf(Mat mat,Point leftTop,Point rightDown,int thresh){//

       // scope*=SetingTheParmer.expendTheSechArea;
        // 1. 计算扫描区域左上角 & 做边界限制（核心修复：防止 x/y 负数）

        // 强制坐标 >= 0

    /*    Mat cutImage = bitmapToMat(MainActivity.imageHadle.getAreaBitmap(scanLeft,scanTop ,scanfRight-scanLeft,scanDown-scanTop));
        MainActivity.imageHadle.saveBitmap(matToBitmap(cutImage));
    */    //县固定列扫描行数
        Log.d("cc", "mat宽度cols="+mat.cols()+" 高度rows="+mat.rows());
        Log.d("MatSizeCheck", "mat宽度cols=");

        Bitmap allScreen = MainActivity.imageHadle.getScreenBitmap();
        if (allScreen == null) {
            return null;
        }

        Mat movemat;
        Bitmap areaBmp;
        float k = 0,b =0;
        try{
            for (int co = 0;co<rightDown.x-leftTop.x - mat.width();co++)//列
            {
                for (int ro = 0;ro<rightDown.y-leftTop.y - mat.height();ro++)//行
                {

                    areaBmp = Bitmap.createBitmap(allScreen, leftTop.x + co,leftTop.y + ro
                            , mat.cols(),mat.rows());
                    movemat =  binarizeToMat(areaBmp,thresh);
                  /*  MainActivity.imageHadle.getAreaBitmap();

                     Log.v("xiangsi", String.valueOf(matchSimilarity(movemat,mat)));
*/
                    b =matchSimilarity(movemat,mat);
                    if(k < b)
                    {
                        k= b;
                        Log.v("xiangsi", String.valueOf(k));

                    }

                    if( matchSimilarity(movemat,mat)>0.55)
                    {
                        areaBmp = Bitmap.createBitmap(allScreen, leftTop.x + co,leftTop.y + ro
                                , mat.cols(),mat.rows());
                      MainActivity.imageHadle.saveBitmap(areaBmp);

                        Point point = new Point(leftTop.x + co,leftTop.y + ro);
                        return point;
                    }
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
            // 最终释放整屏图
            if (allScreen != null) {
                allScreen.recycle();
            }
        }
        return null;

    }
    public static Mat vectorScaleMat(Mat srcMat, int targetW, int targetH) {
        Mat gray = new Mat();
        if(srcMat.channels() > 1){
            Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY);
        }else{
            gray = srcMat.clone();
        }

        List<MatOfPoint> contours = new ArrayList<>();
        org.opencv.core.Mat hierarchy = new org.opencv.core.Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int srcW = srcMat.cols();
        int srcH = srcMat.rows();
        double scaleX = 1.0 * targetW / srcW;
        double scaleY = 1.0 * targetH / srcH;

        Mat dst = Mat.zeros(new Size(targetW, targetH), CvType.CV_8UC1);

        for (MatOfPoint contour : contours) {
            // OpenCV原生点位
            org.opencv.core.Point[] opPts = contour.toArray();
            // 你项目原有 android.graphics.Point 数组
            Point[] newPts = new Point[opPts.length];

            for (int i = 0; i < opPts.length; i++) {
                double nx = opPts[i].x * scaleX;
                double ny = opPts[i].y * scaleY;
                int px = (int) Math.round(nx);
                int py = (int) Math.round(ny);
                // 继续使用安卓Point，不改动全局Point类
                newPts[i] = new Point(px, py);
            }

            // android.Point → OpenCV.MatOfPoint 转换
            org.opencv.core.Point[] cvPoints = new org.opencv.core.Point[newPts.length];
            for(int i=0;i<newPts.length;i++){
                cvPoints[i] = new org.opencv.core.Point(newPts[i].x, newPts[i].y);
            }
            MatOfPoint newContour = new MatOfPoint(cvPoints);

            List<MatOfPoint> tempList = new ArrayList<>();
            tempList.add(newContour);
            Imgproc.drawContours(dst, tempList, 0, new Scalar(255), -1);
            newContour.release();
        }

        gray.release();
        hierarchy.release();
        for(MatOfPoint mp : contours){
            mp.release();
        }
        return dst;
    }



}
