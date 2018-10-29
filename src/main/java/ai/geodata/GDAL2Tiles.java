package ai.geodata;

import ai.geodata.common.BoundingBox;
import ai.geodata.common.Dimensions;
import ai.geodata.g2t.GeoTransformation;
import ai.geodata.util.GlobalGeodetic;
import org.apache.log4j.Logger;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;

import java.util.*;

public class GDAL2Tiles {
    Logger log = Logger.getLogger(GDAL2Tiles.class);

    public static int TILE_SIZE = 256;
    public static int MAX_ZOOM_LEVEL = 32;

    private GlobalGeodetic geodetic = null;
    private int tsize = TILE_SIZE;

    private double[] out_gt = null;
    private Dataset outDataset = null;
    private BoundingBox bbox = null;


    public GDAL2Tiles(String inputPath){
        gdal.AllRegister();
        if (transformRaster(inputPath)){
            this.bbox = new GeoTransformation(this.out_gt).getBounds(
                    new Dimensions<Integer>(outDataset.getRasterXSize(),
                            outDataset.getRasterYSize()));
        }
        this.geodetic = new GlobalGeodetic(null, TILE_SIZE);
    }

    /**
     * 将影像投影转为EPSG:4326（WGS84）
     * @param inputImgPath 输入图像路径
     * @return 投影转换是否成功
     */
    private boolean transformRaster(String inputImgPath){
        Dataset inDataset = gdal.Open(inputImgPath, gdalconstConstants.GA_ReadOnly);
        if (inDataset == null){
            log.error("GDALOpen failed - " + gdal.GetLastErrorNo());
            log.error(gdal.GetLastErrorMsg());
            return false;
        }

        String inSrsWkt = inDataset.GetProjection();
        if (inSrsWkt != null && inDataset.GetGCPCount() > 0)
            inSrsWkt = inDataset.GetGCPProjection();

        SpatialReference inSrs = new SpatialReference();
        if (inSrsWkt != null)
            inSrs.ImportFromWkt(inSrsWkt);

        SpatialReference outSrs = new SpatialReference();
        outSrs.ImportFromEPSG(4326);  //geodetic

        double initGeoTrans[] = {0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
        if (Arrays.equals(inDataset.GetGeoTransform(), initGeoTrans) & inDataset.GetGCPCount() == 0) {
            log.error("There is no georeference - neither affine transformation (worldfile) nor GCPs. You can generate only 'raster' profile tiles." +
                    "Either gdal2tiles with parameter -p 'raster' or use another GIS software for georeference e.g. gdal_transform -gcp / -a_ullr / -a_srs");
            return false;
        }

        if (inSrs != null){
            if (inSrs.ExportToProj4() != outSrs.ExportToProj4() || inDataset.GetGCPCount() > 0)
                outDataset = gdal.AutoCreateWarpedVRT(inDataset,inSrsWkt, outSrs.ExportToWkt());
        }
        else {
            log.error("Input file has unknown SRS. Use --s_srs ESPG:xyz (or similar) to provide source reference system.");
            return false;
        }

        if (outDataset == null)
            outDataset = inDataset;

        this.out_gt = outDataset.GetGeoTransform();

        return true;
    }

    /**
     * 返回所有缩放级别的瓦片范围
     */
    public List<int[]> getZoomLevels(){
//        this.tileswne = this.geodetic.tileLatLonBounds();
        List<int[]> tminmax = new LinkedList<int[]>();
        for (int tz = 0; tz < MAX_ZOOM_LEVEL; tz++) {
            int[] tminxy = this.geodetic.lonlatToTile(this.bbox.getMinimumX(), this.bbox.getMinimumY(), tz);
            int[] tmaxxy = this.geodetic.lonlatToTile(this.bbox.getMaximumX(), this.bbox.getMaximumY(), tz);

            tminxy = new int[]{Math.max(0, tminxy[0]), Math.max(0, tminxy[1])};
            tmaxxy = new int[]{Math.min((int) Math.pow(2, tz + 1) - 1, tmaxxy[0]),
                    (int) Math.min(Math.pow(2, tz) - 1, tmaxxy[1])};

            tminmax.add(tz, new int[]{tminxy[0], tminxy[1], tmaxxy[0], tmaxxy[1]});
        }
        return tminmax;
    }

    /**
     * 影像的Bounding Box
     */
    public BoundingBox getBounds() {
        return this.bbox;
    }

    /**
     * 影像瓦片的最小缩放级别
     */
    public int getTileMinZoom(){
        return this.geodetic.zoomForPixelSize(this.out_gt[1] * Math.max(this.outDataset.getRasterYSize(),
                this.outDataset.getRasterYSize()) / (float) (this.tsize));
    }
    /**
     * 影像瓦片的最大缩放级别
     */
    public int getTileMaxZoom(){
        return this.geodetic.zoomForPixelSize(this.out_gt[1]);
    }

    public static void main(String [] args){
        String imgPath = "D:\\data\\WRJ_430124102214_20170918_DOM.tif";
        GDAL2Tiles tiles = new GDAL2Tiles(imgPath);
        BoundingBox bbox = tiles.getBounds();
        System.out.println(bbox.getCenter().toString());
        System.out.println(tiles.getTileMinZoom());
        System.out.println(tiles.getTileMaxZoom());
        System.out.println(tiles.getZoomLevels());
    }
}
