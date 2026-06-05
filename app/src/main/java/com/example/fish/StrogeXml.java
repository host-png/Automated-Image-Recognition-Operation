package com.example.fish;
import android.content.Context;
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

public class StrogeXml {
    // XML 文件名
    private static final String XML_FILE = "pos.xml";


    /**
     * 写入三组坐标 (x1,y1)(x2,y2)(x3,y3)
     */
    public static void writePoint(Context context, int x1, int y1, int x2, int y2, int x3, int y3) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("root");
            doc.appendChild(root);

            // 第一组坐标
            Element group1 = doc.createElement("group1");
            Element g1x = doc.createElement("x");
            g1x.setTextContent(String.valueOf(x1));
            Element g1y = doc.createElement("y");
            g1y.setTextContent(String.valueOf(y1));
            group1.appendChild(g1x);
            group1.appendChild(g1y);
            root.appendChild(group1);

            // 第二组坐标
            Element group2 = doc.createElement("group2");
            Element g2x = doc.createElement("x");
            g2x.setTextContent(String.valueOf(x2));
            Element g2y = doc.createElement("y");
            g2y.setTextContent(String.valueOf(y2));
            group2.appendChild(g2x);
            group2.appendChild(g2y);
            root.appendChild(group2);

            // 新增第三组坐标
            Element group3 = doc.createElement("group3");
            Element g3x = doc.createElement("x");
            g3x.setTextContent(String.valueOf(x3));
            Element g3y = doc.createElement("y");
            g3y.setTextContent(String.valueOf(y3));
            group3.appendChild(g3x);
            group3.appendChild(g3y);
            root.appendChild(group3);

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
     * 读取三组坐标
     * @return 二维数组 {{x1,y1}, {x2,y2},{x3,y3}}
     */
    public static int[][] readPoint(Context context) {
        int[][] points = {{0, 0}, {0, 0},{0,0}};
        try {
            FileInputStream fis = context.openFileInput(XML_FILE);
            InputSource source = new InputSource(new InputStreamReader(fis));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
            doc.getDocumentElement().normalize();

            // 读取第一组
            Element g1 = (Element) doc.getElementsByTagName("group1").item(0);
            points[0][0] = Integer.parseInt(g1.getElementsByTagName("x").item(0).getTextContent());
            points[0][1] = Integer.parseInt(g1.getElementsByTagName("y").item(0).getTextContent());

            // 读取第二组
            Element g2 = (Element) doc.getElementsByTagName("group2").item(0);
            points[1][0] = Integer.parseInt(g2.getElementsByTagName("x").item(0).getTextContent());
            points[1][1] = Integer.parseInt(g2.getElementsByTagName("y").item(0).getTextContent());

            //读取第三组
            Element g3 = (Element) doc.getElementsByTagName("group3").item(0);
            points[2][0] = Integer.parseInt(g3.getElementsByTagName("x").item(0).getTextContent());
            points[2][1] = Integer.parseInt(g3.getElementsByTagName("y").item(0).getTextContent());

            fis.close();
        } catch (Exception e) {
            // 文件不存在/解析失败，返回默认 0
        }
        return points;
    }
}
