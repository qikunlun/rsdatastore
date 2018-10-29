/**
 * @创建人 kay
 * @创建时间 2018-10-20
 * @描述 关于ArcGIS GDB文件的读取
 */
package ai.geodata;

import org.gdal.ogr.*;
import org.gdal.gdal.gdal;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GDAL2GDB {
    protected Logger log = Logger.getLogger(GDAL2GDB.class);

    //遥感影像路径
    private static String FIELD_IMAGE_PATH = " 地址";
    //读取GDB时返回的属性信息Map对应的Key
    public static String[] KEYS = {"imageFilesPath", "boundaryJson", "boundarySql"};

    public GDAL2GDB(){
        ogr.RegisterAll();
    }

    /**
     * 读取GDB文件，并返回对应的多个影像地址和边界信息。
     * @param gdbPath
     * @return
     */
    public Map<String, List<String>> readGDB(String gdbPath){
        if (!(new File(gdbPath)).exists()){
            log.error("GDB文件不存在:" + gdbPath);
            return null;
        }
        Driver driver = ogr.GetDriverByName("OpenFileGDB");
        DataSource gdb;
        try{
            gdb = driver.Open(gdbPath);
        }catch (Exception e){
            log.error(e.getMessage(), e);
            log.error("GDALOpen failed - " + gdal.GetLastErrorNo());
            log.error("缺少OpenFileGDB驱动或文件不存在.");
            return null;
        }

        List<String> imageFilesPath = new LinkedList<String>();
        List<String> boundaryJson = new LinkedList<String>();
        List<String> boundarySql = new LinkedList<String>();
        for (int iLayer=0; iLayer< gdb.GetLayerCount(); iLayer++) {
            Layer featsClass = gdb.GetLayerByIndex(iLayer);
            FeatureDefn layerDefinition = featsClass.GetLayerDefn();

            if (layerDefinition.GetFieldIndex(FIELD_IMAGE_PATH) < 0){
                log.error(FIELD_IMAGE_PATH + " 属性缺失.");
                return null;
            }

            Feature feature;
            while((feature = featsClass.GetNextFeature()) != null){
                imageFilesPath.add(feature.GetFieldAsString(FIELD_IMAGE_PATH));

                Geometry geom = feature.GetGeometryRef();
                for (int igeo = 0; igeo < geom.GetGeometryCount(); igeo++) {
                    Geometry geomi = geom.GetGeometryRef(igeo);
                    boundaryJson.add(geomi.ExportToJson());

                    String pairPoint = "(";
                    Geometry geomj = geomi.GetGeometryRef(0);
                    for (int ipt = 0; ipt < geomj.GetPointCount() - 1; ipt++){
                        double[] pt= geomj.GetPoint_2D(ipt);
                        pairPoint += String.format("(%f, %f)", pt[0], pt[1]) + ", ";
                    }
                    pairPoint = pairPoint.substring(0, pairPoint.length() - 2) + ")";
                    boundarySql.add(pairPoint);
                }
            }
        }
        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
        resultMap.put(KEYS[0], imageFilesPath);
        resultMap.put(KEYS[1], boundaryJson);
        resultMap.put(KEYS[2], boundarySql);

        return resultMap;
    }

    public static void main(String[] args){
//        String gdbPath = "/root/dataimport/data/AR_005_1975_1_TMH.gdb";
        String gdbPath = "C:\\DATA\\AR_005_1975_1_TMH.gdb";
        GDAL2GDB shape = new GDAL2GDB();
        shape.readGDB(gdbPath);
    }
}
