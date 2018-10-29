import java.io.IOException;

import ai.geodata.GDAL2Tiles;
import ai.geodata.common.BoundingBox;
import ai.geodata.common.GDAL2Thumbnail;
import ai.geodata.common.coordinate.Coordinate;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;


public class HDFSUpload {
    /**
     * @author kunlun by 2018.9.15
     * 新建文件
     * @param dst
     * @param conf
     * @return
     */
    public static boolean createDir(String dst , Configuration conf){
        Path dstPath = new Path(dst) ;
        try{
            FileSystem dhfs = FileSystem.get(conf);
            dhfs.mkdirs(dstPath);
        }
        catch(IOException ie){
            ie.printStackTrace() ;
            return false ;
        }
        return true ;
    }

    /**
     * @author dcx by 2015.11.19
     * 文件上传
     * @param src
     * @param dst
     * @param conf
     * @return
     */
    public static boolean putToHDFS(String src , String dst , Configuration conf){
        Path dstPath = new Path(dst) ;
        try{
            FileSystem hdfs = dstPath.getFileSystem(conf) ;
            hdfs.copyFromLocalFile(false, new Path(src), dstPath);
        }
        catch(IOException ie){
            ie.printStackTrace() ;
            return false ;
        }
        return true ;
    }

    /**
     *  @author dcx by 2015.11.19
     * 文件下载
     * @param src
     * @param dst
     * @param conf
     * @return
     */
    public static boolean getFromHDFS(String src , String dst , Configuration conf){
        Path dstPath = new Path(dst) ;
        try{
            FileSystem dhfs = dstPath.getFileSystem(conf) ;
            dhfs.copyToLocalFile(false, new Path(src), dstPath) ;
        }catch(IOException ie){
            ie.printStackTrace();
            return false ;
        }
        return true ;
    }


    /**
     * @author dcx by 2015.11.19
     * 文件删除
     * @param path
     * @param conf
     * @return
     */
    public static boolean checkAndDel(final String path , Configuration conf){
        Path dstPath = new Path(path) ;
        try{
            FileSystem dhfs = dstPath.getFileSystem(conf) ;
            if(dhfs.exists(dstPath)){
                dhfs.delete(dstPath, true) ;
            }else{
                return false ;
            }
        }catch(IOException ie ){
            ie.printStackTrace() ;
            return false ;
        }
        return true ;
    }

    public static boolean createThumbnail(String input,String outputFileName, int bandID, float scale){
        gdal.AllRegister();
        Dataset hDataset = gdal.Open(input, gdalconstConstants.GA_ReadOnly);
        if (hDataset == null){
            System.err.println("GDALOpen failed - " + gdal.GetLastErrorNo());
            System.err.println(gdal.GetLastErrorMsg());
            return false;
        }

        int nBand = hDataset.getRasterCount();
        if (nBand < 3){
            System.err.println("The Number of bands smaller than 3");
            return false;
        }
        int nCols = hDataset.getRasterXSize();
        int nRows = hDataset.getRasterYSize();

        float minVal = 0, maxVal = 0;
        float buffer[] = new float[nCols];
        Band band = hDataset.GetRasterBand(bandID);
        for (int i=0; i<nRows; i++){
            if(band.ReadRaster(0, i, nCols, 1, buffer) !=
                    gdalconstConstants.CE_None){
                System.err.println("Fail to read image.");
                return false;
            }
            for(int j=0; j<nCols; j++){
                if((i==0) && (j==0))
                    maxVal = minVal = buffer[0];
                if (buffer[j] < minVal)
                    minVal = buffer[j];
                if (buffer[j] > maxVal)
                    maxVal = buffer[j];
            }
        }

        Driver driver = gdal.GetDriverByName("BMP");
        if (driver == null){
            System.err.println("Fail to create png image driver");
            return false;
        }

        int stepSize = (int) (1.0 / scale);
        int dstCols = nCols / stepSize + ((nCols % stepSize) == 0 ? 0:1);
        int dstRows = nRows / stepSize + ((nRows % stepSize) == 0 ? 0:1 );

        String bmpFileName = outputFileName + ".bmp";
        Dataset bmpDataset = driver.Create(bmpFileName, dstCols, dstRows, 1,
                gdalconstConstants.GDT_Byte);
        if (bmpDataset == null){
            System.err.println("Fail to create png image driver");
            return false;
        }
        byte dstBuffer[] = new byte[ dstCols ];//输出缓存

        Band bandWrite = bmpDataset.GetRasterBand(1);
        Band bandRead = hDataset.GetRasterBand(bandID);
        int offset, offsetY = 0;
        for (int i=0; i<nRows; i += stepSize){
            if(bandRead.ReadRaster(0, i, nCols, 1, buffer) !=
                    gdalconstConstants.CE_None){
                System.err.println("Fail to read image.");
                return false;
            }
            offset = 0;
            for(int j=0; j<nCols; j+=stepSize){
                dstBuffer[offset] = (byte) ((buffer[j] - minVal) * 256 / (maxVal - minVal + 1));
                offset++;
            }
            bandWrite.WriteRaster(0, offsetY, dstCols, 1, dstBuffer);
            offsetY++;
        }

        Driver jpegDriver = gdal.GetDriverByName("JPEG");
        if (jpegDriver == null){
            System.err.println("Fail to create jpeg image driver");
            return false;
        }
        Dataset jpegDataset= jpegDriver.CreateCopy(outputFileName, bmpDataset, 0);

        hDataset.delete();
        bmpDataset.delete();
        jpegDataset.delete();

        return true;
    }

    public static boolean createThumbnail(String input,String output, int redBand, int greenBand,
                                          int blueBand, float scale){
        gdal.AllRegister();
        Dataset hDataset = gdal.Open(input, gdalconstConstants.GA_ReadOnly);
        if (hDataset == null)
        {
            System.err.println("GDALOpen failed - " + gdal.GetLastErrorNo());
            System.err.println(gdal.GetLastErrorMsg());
            return false;
        }

        int nBand = hDataset.getRasterCount();
        if (nBand < 3){
            System.err.println("The Number of bands smaller than 3");
            return false;
        }
        int rgbBand[] = {redBand,greenBand,blueBand};
        int nCols = hDataset.getRasterXSize();
        int nRows = hDataset.getRasterYSize();

        float minVal[] = new float[3];
        float maxVal[] = new float[3];
        float buffer[] = new float[nCols];
        for (int k=0; k<3; k++){
            Band band = hDataset.GetRasterBand(rgbBand[k]);
            for (int i=0; i<nRows; i++){
                if(band.ReadRaster(0, i, nCols, 1, buffer) !=
                        gdalconstConstants.CE_None){
                    System.err.println("Fail to read image.");
                    return false;
                }
                for(int j=0; j<nCols; j++){
                    if((i==0) && (j==0))
                        maxVal[k] = minVal[k] = buffer[0];
                    if (buffer[j] < minVal[k])
                        minVal[k] = buffer[j];
                    if (buffer[j] > maxVal[k])
                        maxVal[k] = buffer[j];
                }
            }
        }

        Driver driver = gdal.GetDriverByName("BMP");
        if (driver == null){
            System.err.println("Fail to create png image driver");
            return false;
        }

        int stepSize = (int) (1.0 / scale);
        int dstCols = nCols / stepSize + ((nCols % stepSize) == 0 ? 0:1);
        int dstRows = nRows / stepSize + ((nRows % stepSize) == 0 ? 0:1 );

        Dataset dstDataset = driver.Create(output, dstCols, dstRows, 3,
                gdalconstConstants.GDT_Byte);
        if (dstDataset == null){
            System.err.println("Fail to create png image driver");
            return false;
        }
        byte dstBuffer[] = new byte[ dstCols ];//输出缓存

        for (int k=0; k<3; k++){
            Band bandWrite = dstDataset.GetRasterBand(k+1);
            Band bandRead = hDataset.GetRasterBand(rgbBand[k]);
            int offset, offsetY = 0;
            for (int i=0; i<nRows; i += stepSize){
                if(bandRead.ReadRaster(0, i, nCols, 1, buffer) !=
                        gdalconstConstants.CE_None){
                    System.err.println("Fail to read image.");
                    return false;
                }
                offset = 0;
                for(int j=0; j<nCols; j+=stepSize){
                    dstBuffer[offset] = (byte) ((buffer[j] - minVal[k]) * 256 / (maxVal[k] - minVal[k] + 1));
                    offset++;
                }
                bandWrite.WriteRaster(0, offsetY, dstCols, 1, dstBuffer);
                offsetY++;
            }
        }

        hDataset.delete();
        dstDataset.delete();

        return true;
    }


    /**
     * @param  args 主函数测试
     */
    public static void main(String[] args) {
        boolean status = false ;
        String imgPath = "D:\\data\\WRJ_430124102214_20170918_DOM.tif";
        String outPath = "D:\\data\\WRJ_430124102214_20170918_DOM.jpg";
        String bmpPath = "D:\\data\\WRJ_430124102214_20170918_DOM.bmp";

        GDAL2Thumbnail thum = new GDAL2Thumbnail();
        double scale = thum.getScale(imgPath,1024);
        status = thum.create(imgPath, outPath, 1, 2, 3, scale);
        status = thum.create(imgPath, bmpPath, 1, 0.05f);

        Configuration conf = new Configuration() ;

//java.lang.IllegalArgumentException: Wrong FS: hdfs://, expected: file:///
//解决这个错误的两个方案：
//方案1：下面这条命令必须加上，否则出现上面这个错误
        conf.set("fs.defaultFS", "hdfs://192.168.1.200:8020"); // "hdfs://master:9000"
//方案2： 将core-site.xml 和hdfs-site.xml放入当前工程中
//        status = createDir( "hdfs://192.168.1.200:8020/abc" ,  conf) ;
//        System.out.println("status="+status);

//        String dst = "hdfs://192.168.1.200:8020/EBLearn_data" ;
//        String src = "D:/Workspace/hdfsupload/lib/gdal.jar" ;

//        status = putToHDFS( src ,  dst ,  conf) ;
//        System.out.println("status="+status) ;

//        src = "hdfs://192.168.1.200:8020/EBLearn_data/1.txt" ;
//        dst = "D:/" ;
//        status = getFromHDFS( src ,  dst ,  conf) ;
//        System.out.println("status="+status) ;

//        dst = "hdfs://192.168.1.225:9000/EBLearn_data/hello.txt" ;
//        status = checkAndDel( dst ,  conf) ;
//        System.out.println("status="+status);
    }
}
