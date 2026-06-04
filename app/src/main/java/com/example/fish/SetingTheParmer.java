package com.example.fish;

import android.content.Context;

import androidx.annotation.NonNull;

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

public class SetingTheParmer {

    private static final String XML_FILE = "setParm.xml";
    public static int clickTime = 500;//点击时间

    public static int allTheardTime = 23;//全局刷新率用户输入帧率，自动转换成时间

    public static int expendTheSechArea = 2;


    public static void saveFile(Context context) {
        SetingTheParmer.writePoint(context,SetingTheParmer.clickTime,allTheardTime,expendTheSechArea);
    }


    public static void writePoint(Context context, int x1, int y1, int x2) {//县高出一个文件来，然后把文件里的参数天好最后读取
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("root");
            doc.appendChild(root);

            // 第一组坐标
            Element group1 = doc.createElement("setTimeRun");
            Element g1x = doc.createElement("clickTime");//点击时间
            g1x.setTextContent(String.valueOf(x1));
            Element g1y = doc.createElement("allTheardTime");
            g1y.setTextContent(String.valueOf(y1));
            group1.appendChild(g1x);
            group1.appendChild(g1y);
            root.appendChild(group1);

            // 第二组坐标
            Element group2 = doc.createElement("mainTinteface");
            Element g2x = doc.createElement("expendTheSechArea");//扩大范围额
            g2x.setTextContent(String.valueOf(x2));
            group2.appendChild(g2x);
            root.appendChild(group2);

            // 写入文件（通过 context 调用）
            FileOutputStream fos = context.openFileOutput(XML_FILE, Context.MODE_PRIVATE);
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
     * 读取两组坐标
     * @return 二维数组 {{x1,y1}, {x2,y2}}
     */
    public static int[] readPoint(Context context) {
        //默认兜底
        int[] data = {clickTime,allTheardTime,expendTheSechArea};
        try {
            FileInputStream fis = context.openFileInput(XML_FILE);
            InputSource source = new InputSource(new InputStreamReader(fis));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
            doc.getDocumentElement().normalize();

            //读取setTimeRun
            Element setTimeRun = (Element) doc.getElementsByTagName("setTimeRun").item(0);
            data[0] = Integer.parseInt(setTimeRun.getElementsByTagName("clickTime").item(0).getTextContent());
            data[1] = Integer.parseInt(setTimeRun.getElementsByTagName("allTheardTime").item(0).getTextContent());

            //读取mainTinteface
            Element mainTin = (Element) doc.getElementsByTagName("mainTinteface").item(0);
            data[2] = Integer.parseInt(mainTin.getElementsByTagName("expendTheSechArea").item(0).getTextContent());

            fis.close();
        } catch (Exception e) {
            //文件不存在 保持默认值
            e.printStackTrace();
        }
        return data;
    }

    public static void init(Context context){
        int[] arr = readPoint(context);
        //读到有效数据就赋值，没文件arr还是默认静态值
        clickTime = arr[0];
        allTheardTime = arr[1];
        expendTheSechArea = arr[2];
    }


}
