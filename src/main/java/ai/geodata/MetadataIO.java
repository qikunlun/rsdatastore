package ai.geodata;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MetadataIO {
    private static String itemName = "元数据项";
    private static String itemValueName = "元数据值";
    private String[] stringItems = null;
    private String[] dateItems = null;
    public MetadataIO(){
        stringItems = new String[]{"idDataName", "idEd", "idFormatName", "idRk",
                "idProject", "idOwnEntity", "idFormDep", "idCoordUnit",
                "idCodingNum", "idStandardNum", "idDictStandardNum",
                "crsVertDatum", "crsGeoDatum",  "imaCoverage", "secClass", "pubRang"};
        dateItems = new String[] {"idFormData", "idUpdateData", "tePosition"};
    }

    public boolean read(String xmlPath){
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(new File(xmlPath));
        }catch (DocumentException e){
            e.printStackTrace();
            return false;
        }
        //获取文档根节点(MetaDataItems)
        Element root = document.getRootElement();

        //获取根节点下面的所有子节点（不包过子节点的子节点）
        List<Element> childNodes = root.elements();
        for(Element childNode : childNodes){
            Element item = childNode.element(itemName);
            if (item instanceof Node){
                String itemID = item.getStringValue();
                Element itemValue = childNode.element(itemValueName);
                if (itemValue instanceof Node){
                    String strValue = itemValue.getStringValue();
                    if (strValue != null && !"".equals(strValue)) {
                        //数据项是日期型
                        if (Arrays.binarySearch(dateItems, itemID) >= 0) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/mm/dd");
                            try {
                                Date dateVal = sdf.parse(strValue);
                                System.out.println(dateVal);
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return false;
                            }
                        }

                    }
                }
            }
        }
        return true;
    }

    public static void main(String[] args){
        String xmlPath = "D:\\data\\RS_020_1.xml";
        MetadataIO metadata = new MetadataIO();
        metadata.read(xmlPath);
    }
}
