package com.example.fish;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;

import org.opencv.core.Mat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class DiscreteRandomWorld {

    private static final String BUY_XML = "buy.xml";
    public static int changeBaitCount = 0;  // 钓多少次自动换饵料
    public static int sellFishCount = 0;     // 钓多少次自动卖鱼

    // 改为 5组坐标：x1y1 x2y2 x3y3 x4y4 x5y5  → 一共 10 个元素
    public static int[] buyPos = new int[10];


    public static Mat shop220,fishfd140,nummax120,buy100,x100;
    /**
     * 写入 五组坐标 (x1,y1) (x2,y2) (x3,y3) (x4,y4) (x5,y5) 到 buy.xml
     */

    public static void init(){
        shop220 = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.shop220);
        shop220 = ImageHadle.vectorScaleMat(shop220,MainFunction.sizdToTrsf(44),MainFunction.sizdToTrsf(50));
        fishfd140 = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.fishfd140);
        fishfd140 = ImageHadle.vectorScaleMat(shop220,MainFunction.sizdToTrsf(49),MainFunction.sizdToTrsf(25));
        nummax120 = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.nummax120);
        nummax120 = ImageHadle.vectorScaleMat(shop220,MainFunction.sizdToTrsf(19),MainFunction.sizdToTrsf(24));
        buy100 = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.buy100);
        buy100 = ImageHadle.vectorScaleMat(shop220,MainFunction.sizdToTrsf(52),MainFunction.sizdToTrsf(27));
        x100 = ImageHadle.loadRawTemplate(MainActivity.context,R.raw.x100);
        x100 = ImageHadle.vectorScaleMat(shop220,MainFunction.sizdToTrsf(25),MainFunction.sizdToTrsf(22));

    }
    public static void writeBuyPos(Context context,
                                   int x1, int y1,
                                   int x2, int y2,
                                   int x3, int y3,
                                   int x4, int y4,
                                   int x5, int y5) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("root");
            doc.appendChild(root);

            // 第一组
            Element buy1 = doc.createElement("buy1");
            Element b1x = doc.createElement("x");
            b1x.setTextContent(String.valueOf(x1));
            Element b1y = doc.createElement("y");
            b1y.setTextContent(String.valueOf(y1));
            buy1.appendChild(b1x);
            buy1.appendChild(b1y);
            root.appendChild(buy1);

            // 第二组
            Element buy2 = doc.createElement("buy2");
            Element b2x = doc.createElement("x");
            b2x.setTextContent(String.valueOf(x2));
            Element b2y = doc.createElement("y");
            b2y.setTextContent(String.valueOf(y2));
            buy2.appendChild(b2x);
            buy2.appendChild(b2y);
            root.appendChild(buy2);

            // 第三组
            Element buy3 = doc.createElement("buy3");
            Element b3x = doc.createElement("x");
            b3x.setTextContent(String.valueOf(x3));
            Element b3y = doc.createElement("y");
            b3y.setTextContent(String.valueOf(y3));
            buy3.appendChild(b3x);
            buy3.appendChild(b3y);
            root.appendChild(buy3);

            // 第四组
            Element buy4 = doc.createElement("buy4");
            Element b4x = doc.createElement("x");
            b4x.setTextContent(String.valueOf(x4));
            Element b4y = doc.createElement("y");
            b4y.setTextContent(String.valueOf(y4));
            buy4.appendChild(b4x);
            buy4.appendChild(b4y);
            root.appendChild(buy4);

            // ========== 新增 第五组坐标 buy5 ==========
            Element buy5 = doc.createElement("buy5");
            Element b5x = doc.createElement("x");
            b5x.setTextContent(String.valueOf(x5));
            Element b5y = doc.createElement("y");
            b5y.setTextContent(String.valueOf(y5));
            buy5.appendChild(b5x);
            buy5.appendChild(b5y);
            root.appendChild(buy5);

            FileOutputStream fos = context.openFileOutput(BUY_XML, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            writer.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取全局 buyPos 数组(10位) 写入 XML
     */
    public static void WritePos(Context context) {
        writeBuyPos(
                context,
                buyPos[0], buyPos[1],
                buyPos[2], buyPos[3],
                buyPos[4], buyPos[5],
                buyPos[6], buyPos[7],
                buyPos[8], buyPos[9]  // 第五组
        );
    }

    /**
     * 读取 buy.xml 五组坐标
     * @return 二维数组 {{x1,y1},{x2,y2},{x3,y3},{x4,y4},{x5,y5}} 默认全0
     */
    public static int[][] readBuyPos(Context context) {
        // 改为 5 组
        int[][] buyPos = {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}};
        try {
            FileInputStream fis = context.openFileInput(BUY_XML);
            InputSource source = new InputSource(new InputStreamReader(fis));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
            doc.getDocumentElement().normalize();

            Element b1 = (Element) doc.getElementsByTagName("buy1").item(0);
            buyPos[0][0] = Integer.parseInt(b1.getElementsByTagName("x").item(0).getTextContent());
            buyPos[0][1] = Integer.parseInt(b1.getElementsByTagName("y").item(0).getTextContent());

            Element b2 = (Element) doc.getElementsByTagName("buy2").item(0);
            buyPos[1][0] = Integer.parseInt(b2.getElementsByTagName("x").item(0).getTextContent());
            buyPos[1][1] = Integer.parseInt(b2.getElementsByTagName("y").item(0).getTextContent());

            Element b3 = (Element) doc.getElementsByTagName("buy3").item(0);
            buyPos[2][0] = Integer.parseInt(b3.getElementsByTagName("x").item(0).getTextContent());
            buyPos[2][1] = Integer.parseInt(b3.getElementsByTagName("y").item(0).getTextContent());

            Element b4 = (Element) doc.getElementsByTagName("buy4").item(0);
            buyPos[3][0] = Integer.parseInt(b4.getElementsByTagName("x").item(0).getTextContent());
            buyPos[3][1] = Integer.parseInt(b4.getElementsByTagName("y").item(0).getTextContent());

            // ========== 读取第五组 ==========
            Element b5 = (Element) doc.getElementsByTagName("buy5").item(0);
            buyPos[4][0] = Integer.parseInt(b5.getElementsByTagName("x").item(0).getTextContent());
            buyPos[4][1] = Integer.parseInt(b5.getElementsByTagName("y").item(0).getTextContent());

            fis.close();
        } catch (Exception e) {
            // 异常/文件不存在 返回默认0
        }
        return buyPos;
    }

    public static  boolean isMatStae(Mat mat,int thresh, int x,int y ) {//传入一个比对图片比对相似度
        try {
            Point[] area = FloatWindow.selectArea();


            Point point = ImageHadle.cutSelf(mat, area[0], area[1], thresh);
            if(point!=null)
            {
                buyPos[x] = point.x;
                buyPos[y] = point.y;
                return true;
            }else {
                return false;
            }

            //FloatWindow.setTipsText("框选："+left+","+top+","+right+","+bottom);

        } catch (Exception e) {

        }
return true;
    }
    public static boolean isMatStateGet(Mat mat,int x,int y)//传入比对图片和坐标给出 比对结果
    {

        if(mat !=null){
            Bitmap bitmap = MainActivity.imageHadle.getAreaBitmap(x ,y,mat.width(),mat.height());
            Mat mat1 = ImageHadle.binarizeToMat(bitmap,200);
            if (bitmap == null) {
                return false;
            }
            if( ImageHadle.matchSimilarity(mat,mat1) > 0.5) {
                // Log.d("hokokxia", String.valueOf(ImageHadle.matchSimilarity(hook,mat)));
                bitmap.recycle();
                mat1.release();
                return true;
            }else{
                bitmap.recycle();
                mat1.release();
                return false;
            }
        }
        return  false;
    }


    //是否有坐标，没有就截图获取
    //有坐标就执行自动买鱼
    public static void autobuy() throws InterruptedException {
        if(buyPos[9] == 0){//没有坐标
            //获取商店坐标
            boolean shopFind = false;
            while (!shopFind) {
                shopFind = isMatStae(shop220, 220, 0, 1);
            }
            if(isMatStateGet(shop220,buyPos[0],buyPos[1]))
            {
                AutoClick.service.click(buyPos[0], buyPos[1], 0, 100);

            }
            // 2. 标定饵料分类坐标 (buyPos[2],buyPos[3])
            boolean fdFind = false;
            while (!fdFind) {
                fdFind = isMatStae(fishfd140, 140, 2, 3);
            }
            if (isMatStateGet(fishfd140, buyPos[2], buyPos[3])) {
                AutoClick.service.click(buyPos[2], buyPos[3], 0, 100);
            }
            // 3. 标定数量上限坐标 (buyPos[4],buyPos[5])
            boolean numFind = false;
            while (!numFind) {
                numFind = isMatStae(nummax120, 120, 4, 5);
            }
            if (isMatStateGet(nummax120, buyPos[4], buyPos[5])) {
                AutoClick.service.click(buyPos[4], buyPos[5], 0, 100);
            }
            // 4. 标定购买按钮坐标 (buyPos[6],buyPos[7])
            boolean buyBtnFind = false;
            while (!buyBtnFind) {
                buyBtnFind = isMatStae(buy100, 100, 6, 7);
            }
            if (isMatStateGet(buy100, buyPos[6], buyPos[7])) {
                AutoClick.service.click(buyPos[6], buyPos[7], 0, 100);
            }
         //   sleep(200);
            if (isMatStateGet(buy100, buyPos[6], buyPos[7])) {
                AutoClick.service.click(buyPos[6], buyPos[7], 0, 100);
            }
            // 5. 标定关闭按钮坐标 (buyPos[8],buyPos[9])
            boolean closeFind = false;
            while (!closeFind) {
                closeFind = isMatStae(x100, 100, 8, 9);
            }
            if (isMatStateGet(x100, buyPos[8], buyPos[9])) {
                AutoClick.service.click(buyPos[8], buyPos[9], 0, 100);
            }
// 标定完成 保存坐标到XML
            WritePos(MainActivity.context);
           //获取完成



        }else{
           boolean buystate = false;
        while (!buystate)
            if (isMatStateGet(shop220, buyPos[0], buyPos[1])) {
                AutoClick.service.click(buyPos[0], buyPos[1], 0, 100);
            }
               else {
                sleep(1000);

                if (isMatStateGet(fishfd140, buyPos[2], buyPos[3])) {
                    AutoClick.service.click(buyPos[2], buyPos[3], 0, 100);
                    AutoClick.service.click(buyPos[2], buyPos[3], 0, 100);
                    AutoClick.service.click(buyPos[2], buyPos[3], 0, 100);
                    sleep(1000);
                }
                if (isMatStateGet(nummax120, buyPos[4], buyPos[5])) {
                    AutoClick.service.click(buyPos[4], buyPos[5], 0, 100);
                }
            }

            if (isMatStateGet(nummax120, buyPos[4], buyPos[5])) {
                AutoClick.service.click(buyPos[4], buyPos[5], 0, 100);
            }
            if (isMatStateGet(buy100, buyPos[6], buyPos[7])) {
                AutoClick.service.click(buyPos[6], buyPos[7], 0, 100);
            }
            if (isMatStateGet(x100, buyPos[8], buyPos[9])) {
                AutoClick.service.click(buyPos[8], buyPos[9], 0, 100);
            }

    }
    }
}
