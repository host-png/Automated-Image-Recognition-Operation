package com.example.fish;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.provider.ContactsContract;
import android.util.Log;

import org.opencv.core.Mat;

public class  MainFunction {
    public static Mat hook, fishsate;

    public static Point hookPoint = null,fishStaPoint = null,cGLinepoint = null;
    //钩子识别
    // Mat hookTemplate = ImageHadle.loadRawTemplate(context, R.raw.hook);

    public static int sizdToTrsf(int x)//输入旧尺寸输出当前分辨率下的新尺寸
    {
        return x*MainActivity.width/1080;
    }
    public static int sWToTrsf(int x) {
    return  x*MainActivity.height/2408;
    }


    public static void init(){//初始化
        hook = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.hook);
        fishsate =  ImageHadle.loadRawTemplate(MainActivity.context,R.raw.fishstate);
        hook = ImageHadle.scaleMat(hook,sizdToTrsf(48),sizdToTrsf(57));//尺寸之更宽度有关
        fishsate = ImageHadle.scaleMat(fishsate,sizdToTrsf(60),sizdToTrsf(55));
      //  fishsate=ImageHadle.scaleMat(fishsate,xToTrsf(60),yToTrsf(55));
    }

    public static void getAllUiPointa() {//改方法只能够在已经获取到Ui的相对坐标下才能够使用
        //读取文件赋值

    }


    public static boolean isHook()//钩子判断 必须得有point对象
    {
        if(hookPoint !=null){
            Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(hookPoint.x ,hookPoint.y,sizdToTrsf(48),sizdToTrsf(57));
            Mat mat = ImageHadle.binarizeToMat(bitmap,200);
            if (bitmap == null) {
                return false;
            }
            if( ImageHadle.matchSimilarity(mat,hook) > 0.5) {
               // Log.d("hokokxia", String.valueOf(ImageHadle.matchSimilarity(hook,mat)));
                bitmap.recycle();
                mat.release();
                return true;
            }else{
                bitmap.recycle();
                mat.release();
                return false;
            }
        }
     return  false;
    }


    public static boolean isFishStae(){//鱼判断
     if(fishStaPoint != null)
     {
         Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(fishStaPoint.x,fishStaPoint.y,sizdToTrsf(60),sizdToTrsf(55));
         Mat mat = ImageHadle.binarizeToMat(bitmap,140);
         if (bitmap == null) {
             return false;
         }
         if(ImageHadle.matchSimilarity(mat,fishsate) > 0.5){
             //og.d("hokokxia", String.valueOf(ImageHadle.matchSimilarity(mat,fishsate)));
             mat.release();
             bitmap.recycle();
             return true;
         }else{
             bitmap.recycle();
             mat.release();
             return false;
         }
     }
     return false;
    }

    //获取黄条的坐标只返回x y是已知的
    // x是相对坐标还得加上815

    //cGLinepoint.x = fishSta.x+sWToTrsf(119)
    public static int cursorPoint() {
        int x = 0;
        Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y, sWToTrsf(785), sizdToTrsf(5));
        if (areaBitmap == null) return -1;

        Mat mat = ImageHadle.binarizeToMat(areaBitmap, 190);
        if (mat == null || mat.empty()) {
            areaBitmap.recycle();
            return -1;
        }

        while (x <= sWToTrsf(785)) {
            if (ImageHadle.getPointColorFromMat(mat, x, (int)((sizdToTrsf(5)+1)/2)) == 0xFFFFFFFF) break;
            x++;
        }

        mat.release();
        areaBitmap.recycle();
        return x + cGLinepoint.x;//返回光标x
    }

    public static boolean weithState = true;
    public static int weightOfGreen;

    public static int isGreen(Mat mat,int x,int y,int direction) {//这里的mat不用释放
        int i = 1;//在王后面便利六格防止光标误判
        for (;i<6;i++) {
            if (ImageHadle.getPointColorFromMat(mat, x+(i*direction), y) == 0xFFFFFFFF) {
                continue;
            }
            else {
                break;
            }
        }
       return i;
    }

    public static int greenPostison() {
        int x = 0;
        //xToTrsf(815), yToTrsf(86), xToTrsf(785), yToTrsf(5)
      Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y, sWToTrsf(785), sizdToTrsf(5));
        if (areaBitmap == null) return -1;

        Mat mat = ImageHadle.twoBinarizeToMat(areaBitmap, 160,180);
     /*   MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(mat));
        Bitmap areaBitmap1 = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y, sWToTrsf(785), (sizdToTrsf(5)+1)/2);
        MainActivity.imageHadle.saveBitmap(areaBitmap1);
       */ if (mat == null || mat.empty()) {
            areaBitmap.recycle();
            return -1;
        }
        if(weithState)
        {

            while (x <= sWToTrsf(785)) {//左边网友变
                if (ImageHadle.getPointColorFromMat(mat, x, (int)((sizdToTrsf(5)+1)/2)) == 0xFFFFFFFF)
                {
                    int i =isGreen(mat,x,  (int)((sizdToTrsf(5)+1)/2),1);
                  if(i == 6)
                  {
                      break;
                  }
                  else {
                      x=x+i-1;
                  }
                }
                x++;
            }
            int m = x;
            x = sWToTrsf(785);
            while (x > 0) {//右边王左边
                if (ImageHadle.getPointColorFromMat(mat, x, (int)((sizdToTrsf(5)+1)/2)) == 0xFFFFFFFF){
                    int i =isGreen(mat,x, (int)((sizdToTrsf(5)+1)/2),-1);
                    if(i == 6)
                    {
                        break;
                    }
                    else {
                        x=x-i+1;
                    }
                }
                x--;
            }
            weightOfGreen = x-m;
            weithState = false;

            mat.release();
            areaBitmap.recycle();
            return (weightOfGreen/2) + m + cGLinepoint.x;
        }

        while (x <= sWToTrsf(785)) {
            if (ImageHadle.getPointColorFromMat(mat, x, (int)((sizdToTrsf(5)+1)/2)) == 0xFFFFFFFF){
                int i =isGreen(mat, x, (int)((sizdToTrsf(5)+1)/2),1);
                if(i == 6)
                {
                    break;
                }
                else {
                    x=x+i-1;
                }
            }
            x++;
        }

        mat.release();
        areaBitmap.recycle();
        return (weightOfGreen/2) + x + cGLinepoint.x;//返回率条中心坐标
    }

    public static int addERROR = 0;
    public static int lastError = 0;
    // 修正：改成静态代码块初始化
    public static float speep;
    static {
        speep =  (float)sWToTrsf(771)/2230;//xToTrsf像素/时间   单位像素/ms
    }

    public static void currMove(int move)//输入移动移动方向进行移动s
    {
        if(move>0)//光标右移
        {

            AutoClick.service.click(sWToTrsf(2060), sizdToTrsf(830), 0, (int)((float)move/speep));
           /* addERROR += move;
            int AT = (int)((move*1.8) + addERROR * 0.01 + (move-lastError) * 0.2);
            AutoClick.service.click(2060, 830, 0, AT);
            lastError = move;*/
        }
        else {//光标左移
            move = -move;
            AutoClick.service.click(sWToTrsf(380),sizdToTrsf(830), 0, (int) ((float) move / speep));

          /*  move = -move;
            addERROR += move;
            int AT = (int)((move*1.8) + addERROR * 0.01 + (move-lastError) * 0.2);
            AutoClick.service.click(380,830,0,AT);
            lastError = move;     }
*/
        }
    }

    //高灵敏控制
    public static void contralCurr() {
        int currX = cursorPoint();
        int greenX = greenPostison();
        if(-5>(currX-greenX)||(currX-greenX)>5){//控制在10个像素以内
            currMove(greenX-currX);
        }

    }


}
