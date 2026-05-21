package com.example.fish;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class  MainFunction {
    public static Mat hook, fishsate;

    //钩子识别
    // Mat hookTemplate = ImageHadle.loadRawTemplate(context, R.raw.hook);
    public static void init(){//初始化
        hook = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.hook);
        fishsate =  ImageHadle.loadRawTemplate(MainActivity.context,R.raw.fishstate);
    }

    public static boolean isHook()//钩子判断
    {
        Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(2127,924,48,57);
        Mat mat = ImageHadle.binarizeToMat(bitmap,200);
      if( ImageHadle.matchSimilarity(hook,mat) > 0.65) {
          bitmap.recycle();
          mat.release();
          return true;
      }else{
          bitmap.recycle();
          mat.release();
          return false;
      }
    }

    public static boolean isFishStae(){//鱼判断
        Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(700,65,60,55);
        Mat mat = ImageHadle.binarizeToMat(bitmap,140);
        if(ImageHadle.matchSimilarity(fishsate,mat) > 0.65){
            mat.release();
            bitmap.recycle();
            return true;
        }else{
            bitmap.recycle();
            mat.release();
            return false;
        }
    }

    //获取黄条的坐标只返回x y是已知的
    // x是相对坐标还得加上815
    public static int cursorPoint() {
        int x = 0;
        Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(815, 86, 785, 5);
        if (areaBitmap == null) return -1;

        Mat mat = ImageHadle.binarizeToMat(areaBitmap, 190);
        if (mat == null || mat.empty()) {
            areaBitmap.recycle();
            return -1;
        }

        while (x <= 785) {
            if (ImageHadle.getPointColorFromMat(mat, x, 3) == 0xFFFFFFFF) break;
            x++;
        }

        mat.release();
        areaBitmap.recycle();
        return x + 815;//返回光标x
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
      Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(815, 86, 785, 5);
        if (areaBitmap == null) return -1;

        Mat mat = ImageHadle.twoBinarizeToMat(areaBitmap, 160,180);
        if (mat == null || mat.empty()) {
            areaBitmap.recycle();
            return -1;
        }
        if(weithState)
        {

            while (x <= 785) {//左边网友变
                if (ImageHadle.getPointColorFromMat(mat, x, 3) == 0xFFFFFFFF)
                {
                    int i =isGreen(mat,x,3,1);
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
            x = 785;
            while (x > 0) {//右边王左边
                if (ImageHadle.getPointColorFromMat(mat, x, 3) == 0xFFFFFFFF){
                    int i =isGreen(mat,x,3,-1);
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
            return (weightOfGreen/2) + m + 815;
        }

        while (x <= 785) {
            if (ImageHadle.getPointColorFromMat(mat, x, 3) == 0xFFFFFFFF){
                int i =isGreen(mat,x,3,1);
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
        return (weightOfGreen/2) + x + 815;//返回率条中心坐标
    }

    public static int addERROR = 0;
    public static int lastError = 0;
    public static float speep = 0.3457f;
    public static void currMove(int move)//输入移动移动方向进行移动s
    {
        if(move>0)//光标右移
        {

            AutoClick.service.click(2060, 830, 0, (int)((float)move/speep));
           /* addERROR += move;
            int AT = (int)((move*1.8) + addERROR * 0.01 + (move-lastError) * 0.2);
            AutoClick.service.click(2060, 830, 0, AT);
            lastError = move;*/
        }
        else {//光标左移
            move = -move;
            AutoClick.service.click(380,830, 0, (int) ((float) move / speep));

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
