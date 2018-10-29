/**
 * @创建人 kay
 * @创建时间 2018-10-20
 * @描述 关于影像的Bounding Box和瓦片级别的相关信息。
 */

package ai.geodata;
import org.apache.log4j.Logger;
import org.gdal.gdal.gdal;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GDALTranslate {
    protected Logger log = Logger.getLogger(GDALTranslate.class);

    public GDALTranslate(){
        gdal.AllRegister();
    }

    /**
     * 将其他格式的影像转为GeoTiff格式
     * @param imgPath 其他格式的影像
     * @param tiffPath GeoTiff格式的影像
     * @return 是否成功执行
     */
    public boolean image2GTiff(String imgPath, String tiffPath){
        String[] cmd = {"gdal_translate", "-of", "GTiff", imgPath, tiffPath};
        Process process = null;
        BufferedReader stderrReader, stdoutReader;
        try {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(cmd);
            stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = stdoutReader.readLine()) != null)
                log.info(line);
            while ((line = stderrReader.readLine()) != null)
                log.error(line);

            int exitValue = process.waitFor();
            if (exitValue != 0){
                log.error("gdal_translate命令执行失败.");
                return false;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            return false;
        }  finally {
            if (process != null)
                process.destroy();
        }
        return true;
    }

    public boolean erdas2GTiff(String imgPath, String tiffPath){
        Driver tiffDriver = gdal.GetDriverByName("GTiff");
        Dataset inDataset = gdal.Open(imgPath, 0);
        if (inDataset == null) {
            System.err.println("GDALOpen failed - " + gdal.GetLastErrorNo());
            System.err.println(gdal.GetLastErrorMsg());
            return false;
        }

        Dataset outDataset = tiffDriver.CreateCopy(tiffPath, inDataset);
        if (outDataset == null) {
            System.err.println("Transform failed - " + gdal.GetLastErrorNo());
            System.err.println(gdal.GetLastErrorMsg());
            return false;
        }
        return true;
    }

    public static void main(String[] args){
        GDALTranslate gdaltrans = new GDALTranslate();
        gdaltrans.image2GTiff("F:\\TestData\\AR_005_1\\AR_005_1.img",
                "F:\\TestData\\AR_005_1\\AR_005_1.tif");
    }
}
