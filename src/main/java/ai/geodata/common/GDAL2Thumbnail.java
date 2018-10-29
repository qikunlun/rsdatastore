/**
 * @创建人 kay
 * @创建时间 2018-10-20
 * @描述 生成影像缩略图
 */

package ai.geodata.common;

import org.apache.log4j.Logger;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import java.io.File;
import java.util.UUID;

public class GDAL2Thumbnail {
    protected Logger log = Logger.getLogger(GDAL2Thumbnail.class);

    private static int TRANSPARE_COLOR = -256*256*256;

    public GDAL2Thumbnail(){
        gdal.AllRegister();
    }

    /**
     * 根据影像和目标缩略图大小，计算要缩放的尺度
     * @param inputFileName 输入影像路径
     * @param size 生成影像的长和宽的最大值
     * @return
     */
    public double getScale(String inputFileName, int size){
        Dataset inDataset = gdal.Open(inputFileName, gdalconstConstants.GA_ReadOnly);
        if (inDataset == null) {
            log.error("GDALOpen failed - " + gdal.GetLastErrorNo());
            log.error(gdal.GetLastErrorMsg());
            return -1.0;
        }

        double nCols = inDataset.getRasterXSize();
        double nRows = inDataset.getRasterYSize();

        return Math.min(size/nCols, size/nRows);
    }

    /**
     * 生成缩略图
     * @param inputFileName 输入影像路径
     * @param outputFileName 输出缩略图路径
     * @param band 选择单个波段
     * @param scale 选择缩放的尺度
     * @return 是否执行成功
     */
    public boolean create(String inputFileName,String outputFileName, int band, double scale) {
        int bands[] = {band};
        return createThumbnail(inputFileName, outputFileName, bands, scale);
    }

    /**
     * 生成缩略图
     * @param inputFileName 输入影像路径
     * @param outputFileName 输出缩略图路径
     * @param redBand 选择红色波段
     * @param greenBand 选择蓝色波段
     * @param blueBand 选择绿色波段
     * @param scale 选择缩放的尺度
     * @return 是否执行成功
     */
    public boolean create(String inputFileName,String outputFileName, int redBand, int greenBand,
                                       int blueBand, double scale) {
        int bands[] = {redBand, greenBand, blueBand};
        return createThumbnail(inputFileName, outputFileName, bands, scale);
    }

    private String getImageTypeGDAL(String inputFileName){
        inputFileName = inputFileName.toLowerCase();
        if (inputFileName.endsWith(".bmp")){
            return "BMP";
        }else if (inputFileName.endsWith(".jpg")){
            return "JPEG";
        }else if (inputFileName.endsWith(".png")){
            return "PNG";
        }else if (inputFileName.endsWith(".gif")){
            return "GIF";
        }
        return null;
    }

    private boolean createThumbnail(String inputFileName,String outputFileName, int [] bands, double scale){
        if (bands.length != 1 && bands.length != 3){
            log.error("Parameter bands must have 1 or 3 elements.");
            return false;
        }
        String gType = getImageTypeGDAL(outputFileName);
        if(gType == null){
            log.error("GDAL2Thumbnail only support format of bmp, jpg, png or gif.");
            return false;
        }

        Dataset inDataset = gdal.Open(inputFileName, gdalconstConstants.GA_ReadOnly);
        if (inDataset == null) {
            log.error("GDALOpen failed - " + gdal.GetLastErrorNo());
            log.error(gdal.GetLastErrorMsg());
            return false;
        }

        int nBand = inDataset.getRasterCount();
        if (nBand < bands.length){
            log.error("The Number of bands smaller than length of bands");
            return false;
        }
        int nCols = inDataset.getRasterXSize();
        int nRows = inDataset.getRasterYSize();

        float minVal[] = new float[3];
        float maxVal[] = new float[3];
        float buffer[] = new float[nCols];
        for (int k=0; k<bands.length; k++){
            Band band = inDataset.GetRasterBand(bands[k]);
            for (int i=0; i<nRows; i++){
                if(band.ReadRaster(0, i, nCols, 1, buffer) !=
                        gdalconstConstants.CE_None){
                    log.error("Fail to read image.");
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
            log.error("Fail to create bmp image driver");
            return false;
        }

        int stepSize = (int) (1.0 / scale);
        int dstCols = nCols / stepSize + ((nCols % stepSize) == 0 ? 0:1);
        int dstRows = nRows / stepSize + ((nRows % stepSize) == 0 ? 0:1 );

        String bmpFileName = outputFileName;
        if (!gType.equals("BMP")){
            UUID uuid = UUID.randomUUID();

            String tempPath =System.getProperty("java.io.tmpdir");
            bmpFileName = tempPath + File.separator +  uuid.toString() + ".bmp";
        }

        Dataset bmpDataset = driver.Create(bmpFileName, dstCols, dstRows, bands.length,
                gdalconstConstants.GDT_Byte);
        if (bmpDataset == null){
            log.error("Fail to create bmp image driver");
            return false;
        }
        byte dstBuffer[] = new byte[ dstCols ];//输出缓存

        for (int k=0; k<bands.length; k++){
            Band bandWrite = bmpDataset.GetRasterBand(k+1);
            Band bandRead = inDataset.GetRasterBand(bands[k]);
            int offset, offsetY = 0;
            for (int i=0; i<nRows; i += stepSize){
                if(bandRead.ReadRaster(0, i, nCols, 1, buffer) !=
                        gdalconstConstants.CE_None){
                    log.error("Fail to read image.");
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
        bmpDataset.delete();
        driver.delete();

        if(!gType.equals("BMP")) {
            Driver outDriver = gdal.GetDriverByName(gType);
            if (outDriver == null) {
                log.error("Fail to create " + gType + " image driver");
                return false;
            }
            Dataset outDataset = outDriver.CreateCopy(outputFileName, bmpDataset, 0);
            outDataset.delete();
        }

        inDataset.delete();
        return true;
    }

    public static void main(String[] args){
        String imgPath = "D:\\data\\WRJ_430124102214_20170918_DOM.tif";
        String outPath = "D:\\data\\WRJ_430124102214_20170918_DOM.png";
        String bmpPath = "D:\\data\\WRJ_430124102214_20170918_DOM.bmp";

        GDAL2Thumbnail thum = new GDAL2Thumbnail();
        double scale = thum.getScale(imgPath,1024);
        boolean status = thum.create(imgPath, outPath, 1, 2, 3, scale);
    }

}
