package com.example.fish;

import static android.os.SystemClock.sleep;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.provider.ContactsContract;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class  MainFunction {
    public static Mat hook, fishsate;
    public  static Mat bigHook,bigFish;

    public static int[] GCpos= new int[2];
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
        bigHook = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.bighook);
        bigFish = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.bigfish);
        //hook = ImageHadle.scaleMat(hook,sizdToTrsf(48),sizdToTrsf(57));
        hook = ImageHadle.vectorScaleMat(hook,sizdToTrsf(48),sizdToTrsf(57));

        //fishsate = ImageHadle.scaleMat(fishsate,sizdToTrsf(60),sizdToTrsf(55));
        fishsate = ImageHadle.vectorScaleMat(fishsate,sizdToTrsf(60),sizdToTrsf(55));
      //  fishsate=ImageHadle.scaleMat(fishsate,xToTrsf(60),yToTrsf(55));
          bigHook = ImageHadle.vectorScaleMat(bigHook,72* ImageHadle.height/2064
                        ,87* ImageHadle.height/2064);
          bigFish = ImageHadle.vectorScaleMat(bigFish,89* ImageHadle.height/2064
                  ,72* ImageHadle.height/2064);
    }

    public static void getAllUiPointa() {//改方法只能够在已经获取到Ui的相对坐标下才能够使用
        //读取文件赋值

    }


    public static boolean isHook()//钩子判断 必须得有point对象
    {
        if(SetingTheParmer.stateDeviceModel == 3)
        {
            if (isBigHook())
            {
                return true;
            }

        }
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

    public static boolean isBigHook()//钩子判断 必须得有point对象
    {
        if(hookPoint !=null){
            Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(hookPoint.x ,hookPoint.y,72* ImageHadle.height/2064
                    ,87* ImageHadle.height/2064);
            Mat mat = ImageHadle.binarizeToMat(bitmap,140);
            if (bitmap == null) {
                return false;
            }
            if( ImageHadle.matchSimilarity(mat,bigHook) > 0.5) {
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
        if(SetingTheParmer.stateDeviceModel == 3)
        {
            if( isBigFish())
            {
                return true;
            }
        }
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
    public static boolean isBigFish(){//鱼判断
        if(fishStaPoint != null)
        {
            Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(fishStaPoint.x,fishStaPoint.y,89* ImageHadle.height/2064
                    ,72* ImageHadle.height/2064);
            Mat mat = ImageHadle.binarizeToMat(bitmap,140);
            if (bitmap == null) {
                return false;
            }
            if(ImageHadle.matchSimilarity(mat,bigFish) > 0.5){
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
    public static boolean twoVerifyHook() {//双重验证法

        boolean sta1 = isBigHook();
        sleep(500);
        boolean sta2 = isBigHook();

        return sta1||sta2;

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



    public static int[][] controlPos = new int[2][2];
    // 修正：改成静态代码块初始化
    public static float speep;
    static {
       // speep =  (float)sWToTrsf(771)/2230;//xToTrsf像素/时间   单位像素/ms771/2230
        speep =  (float)771/2230;//xToTrsf像素/时间   单位像素/ms771/2230
    }

    public static void currMove(int move)//输入移动移动方向进行移动s
    {
        if(move>0)//光标右移
        {
            if(SetingTheParmer.useLeftRightControl == 1)
            {
                AutoClick.service.click(controlPos[1][0], controlPos[1][1], 0, (int)((float)move/speep));

            }else {
                AutoClick.service.click(sWToTrsf(2060), sizdToTrsf(830), 0, (int)((float)move/speep));

            }
               /* addERROR += move;
            int AT = (int)((move*1.8) + addERROR * 0.01 + (move-lastError) * 0.2);
            AutoClick.service.click(2060, 830, 0, AT);
            lastError = move;*/
        }
        else {//光标左移
            move = -move;
            if(SetingTheParmer.useLeftRightControl == 1)
            {
                AutoClick.service.click(controlPos[0][0], controlPos[0][1], 0, (int)((float)move/speep));

            }else {
                AutoClick.service.click(sWToTrsf(380), sizdToTrsf(830), 0, (int) ((float) move / speep));
            }
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
       /* int greenX = newGreenPostison();
        int currX = newCursorPoint();
*/
        GCpos = getGCPos();//0是绿标1是光标

        if (GCpos == null || GCpos.length < 2) {
            return; // 识别不到目标，不执行移动逻辑
        }
        else {
            if(-5>(GCpos[1]-GCpos[0])||(GCpos[1]-GCpos[0])>5){//控制在10个像素以内
                currMove(GCpos[0]-GCpos[1]);
            }
        }


    }




    //hsv图像色阈法设置第三组坐标
    public static void initSearchTheGreenPosHsv(Bitmap allscren) {//前提要有fishstatePoint//外面传入screemall

        int x = MainFunction.fishStaPoint.x+MainFunction.fishsate.width();
        int y =   MainFunction.fishStaPoint.y;
        //截取
     Bitmap bitmap  = MainActivity.imageHadle.getAreaBitmap(allscren,x,y,
            ImageHadle.width - 2*(x),MainFunction.fishsate.height());

    Mat mat = ImageHadle.bitmapToBGRMat(bitmap);
    //定义绿色范围
        Scalar lowerGreen = new Scalar(35,70,220);
        Scalar upperGreen = new Scalar(75,255,255);
    Mat greenMask = ImageHadle.filterByHSV(mat, lowerGreen, upperGreen);
      /*  MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(greenMask));
*/
        mat.release();
    int addPos = 0;
    int num = 0;
    boolean firstWrite = false,exit1 = false;
    //从下网上扫
    for (int i = MainFunction.fishsate.height()-1;i>=0;i--){//行
        if(num == 10 || exit1)
        {
            break;
        }
        for(int l = 0;l< ImageHadle.width - 2*(x);l++) {
          if(ImageHadle.getPointColorFromMat(greenMask,l,i) == 0xFFFFFFFF) {

             if( isGreen(greenMask,l,i,1) == 6) {


                 if(!firstWrite){
                     firstWrite =true;
                 }
                 addPos += i;
                 num++;
                 break;
             }
          }else {
              if(firstWrite){
                  exit1 = true;
                  break;
              }
          }
        }
    }
        MainFunction.cGLinepoint = new Point( x, y+(addPos/num));


        greenMask.release();
        bitmap.recycle();


    }

    public static int[] getGCPos(){//获取同一截图下的绿条与光标
        int greenX = 0;//绿条相对
        int currentX = 0;
        int wid = ImageHadle.width - (2*cGLinepoint.x);//获取整个条框的范围
        Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y-1, wid ,3);//限定截取范围，光标为主

        if (areaBitmap == null) return null;

        Mat mat = ImageHadle.bitmapToBGRMat(areaBitmap);
        //定义绿色范围
        Scalar lowerGreen = new Scalar(35,70,220);
        Scalar upperGreen = new Scalar(75,255,255);
        Mat greenMask = ImageHadle.cropMat(mat,0,1,mat.cols(),1);
        greenMask = ImageHadle.filterByHSV(greenMask, lowerGreen, upperGreen);//处理绿条截图

        //定义黄色范围
        Scalar lowerYellow = new Scalar(80,30,240);
        Scalar upperYellow = new Scalar(100,255,255);
        Mat yellowMask = ImageHadle.filterByHSV(mat, lowerYellow, upperYellow);
        //MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(greenMask));


        if (greenMask == null || greenMask.empty() || yellowMask == null || yellowMask.empty())  {
           // areaBitmap.recycle();
            greenMask.release();
            yellowMask.release();
            mat.release();
            areaBitmap.recycle();
            return null;
        }//空判断

        //绿条处理
        if(weithState) {

            while (greenX <= wid - 1) {//左边网友变
                if (ImageHadle.getPointColorFromMat(greenMask, greenX, 0) == 0xFFFFFFFF) {
                    int i = isGreen(greenMask, greenX, 0, 1);
                    if (i == 6) {
                        break;
                    } else {
                        greenX = greenX + i - 1;
                    }
                }
                greenX++;
            }
            int m = greenX;
            greenX = wid - 1;
            while (greenX > 0) {//右边王左边
                if (ImageHadle.getPointColorFromMat(greenMask, greenX, 0) == 0xFFFFFFFF) {
                    int i = isGreen(greenMask, greenX, 0, -1);
                    if (i == 6) {
                        break;
                    } else {
                        greenX = greenX - i + 1;
                    }
                }
                greenX--;
            }
            weightOfGreen = greenX - m;
            weithState = false;

           greenX =  (weightOfGreen / 2) + m + cGLinepoint.x;
        }
        else{
            while (greenX <= wid-1) {
                if (ImageHadle.getPointColorFromMat(greenMask, greenX,0) == 0xFFFFFFFF){
                    int i =isGreen(greenMask, greenX,0,1);
                    if(i == 6)
                    {
                        break;
                    }
                    else {
                        greenX=greenX+i-1;
                    }
                }
                greenX++;

            }
            greenX =  (weightOfGreen/2) + greenX + cGLinepoint.x;//返回率条中心坐标
        }

        //光标处理
        while (currentX <= wid-1) {
            if (ImageHadle.getPointColorFromMat(yellowMask, currentX, 1) == 0xFFFFFFFF) {
                if(yellowCrent(yellowMask,currentX)){
                    break;
                }
            }
            currentX++;
        }


        currentX = currentX + cGLinepoint.x;//返回光标x





        greenMask.release();
        yellowMask.release();
        mat.release();
        areaBitmap.recycle();
        int [] reslt= new int[2];
        reslt[0] = greenX;
        reslt[1] = currentX;
        return  reslt;
    }



    public static int newGreenPostison() {
        int x = 0;
        int wid = ImageHadle.width - (2*cGLinepoint.x);
        //xToTrsf(815), yToTrsf(86), xToTrsf(785), yToTrsf(5)
        Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y, wid ,1);
        if (areaBitmap == null) return -1;

        Mat mat = ImageHadle.bitmapToBGRMat(areaBitmap);
        //定义绿色范围
        Scalar lowerGreen = new Scalar(35,70,220);
        Scalar upperGreen = new Scalar(75,255,255);

        Mat greenMask = ImageHadle.filterByHSV(mat, lowerGreen, upperGreen);
    /*    MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(greenMask));

     */   mat.release(); /*   MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(mat));
        Bitmap areaBitmap1 = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y, sWToTrsf(785), (sizdToTrsf(5)+1)/2);
        MainActivity.imageHadle.saveBitmap(areaBitmap1);
       */
        if (greenMask == null || greenMask.empty()) {
            areaBitmap.recycle();
            greenMask.release();
            return -1;
        }
        if(weithState)
        {

            while (x <= wid-1) {//左边网友变
                if (ImageHadle.getPointColorFromMat(greenMask, x, 0) == 0xFFFFFFFF)
                {
                    int i =isGreen(greenMask,x, 0,1);
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
            x = wid -1;
            while (x > 0) {//右边王左边
                if (ImageHadle.getPointColorFromMat(greenMask, x,0) == 0xFFFFFFFF){
                    int i =isGreen(greenMask,x, 0,-1);
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

            greenMask.release();
            areaBitmap.recycle();
            return (weightOfGreen/2) + m + cGLinepoint.x;
        }

        while (x <= wid-1) {
            if (ImageHadle.getPointColorFromMat(greenMask, x,0) == 0xFFFFFFFF){
                int i =isGreen(greenMask, x,0,1);
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

        greenMask.release();
        areaBitmap.recycle();
        return (weightOfGreen/2) + x + cGLinepoint.x;//返回率条中心坐标
    }



    static boolean yellowCrent(Mat mat, int x){
        if (ImageHadle.getPointColorFromMat(mat, x+1, 1) == 0xFFFFFFFF){//前面
            return true;
        }else if(ImageHadle.getPointColorFromMat(mat, x, 0) == 0xFFFFFFFF){//上面
            return true;
        }
        else if(ImageHadle.getPointColorFromMat(mat, x, 2) == 0xFFFFFFFF){//下面
            return true;
        }
        return  false;
    }

    public static int newCursorPoint() {

        int wid = ImageHadle.width - (2*cGLinepoint.x);
        //xToTrsf(815), yToTrsf(86), xToTrsf(785), yToTrsf(5)
        Bitmap areaBitmap = MainActivity.imageHadle.getAreaBitmap(cGLinepoint.x, cGLinepoint.y-1, wid ,3);
        if (areaBitmap == null) return -1;

        Mat mat = ImageHadle.bitmapToBGRMat(areaBitmap);
        //定义黄色范围
        Scalar lowerYellow = new Scalar(80,30,240);
        Scalar upperYellow = new Scalar(100,255,255);
        Mat yellowMask = ImageHadle.filterByHSV(mat, lowerYellow, upperYellow);
        //MainActivity.imageHadle.saveBitmap(ImageHadle.matToBitmap(greenMask));


        int x = 0;

         if (yellowMask == null || yellowMask.empty()) {
             mat.release();
            areaBitmap.recycle();
            yellowMask.release();

            return -1;
        }

        while (x <= wid-1) {
            if (ImageHadle.getPointColorFromMat(yellowMask, x, 1) == 0xFFFFFFFF) {
                if(yellowCrent(yellowMask,x)){
                    break;
                }
            }
            x++;
        }

        mat.release();
        areaBitmap.recycle();
        yellowMask.release();
        return x + cGLinepoint.x;//返回光标x
    }
}
