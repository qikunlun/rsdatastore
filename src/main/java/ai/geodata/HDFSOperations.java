package ai.geodata;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;

public class HDFSOperations {
    protected Logger log = Logger.getLogger(GDAL2GDB.class);
    private Configuration conf = new Configuration();

    public HDFSOperations(){
        conf.set("fs.defaultFS", "hdfs://192.168.1.200:8020");
    }

    public boolean putToHDFS(String localFilePath , String hdfsURL){
        InputStream inputLocal = null; //本地文件输入流
        OutputStream outHDFS = null; //HDFS输出流
        try {
            inputLocal = new BufferedInputStream(new FileInputStream(localFilePath));
            if (inputLocal == null){
                log.error(localFilePath + "不存在.");
                return false;
            }
            //获取HDFS文件系统
            FileSystem hdfs = FileSystem.get(URI.create("hdfs://192.168.174.128:9000"),conf);
            if (hdfs.exists(new Path(hdfsURL))){
                log.error(hdfsURL + "已经存在.");
                return false;
            }
            outHDFS = hdfs.create(new Path(hdfsURL));
            IOUtils.copyBytes(inputLocal,outHDFS,4096,true);
            log.info(localFilePath + "上传成功.");
        }catch (IOException e){
            IOUtils.closeStream(inputLocal);
            IOUtils.closeStream(outHDFS);
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    public boolean getFromHDFS(String hdfsURL, String localFilePath){
        try{
            //构建FileSystem
            OutputStream outStream = new FileOutputStream(new File(localFilePath));
            FileSystem hdfs = FileSystem.get(URI.create(hdfsURL), conf);
            //读取HDFS文件
            InputStream inStream = hdfs.open(new Path(hdfsURL));
            IOUtils.copyBytes(inStream, outStream,2048, true);//保存到本地
            inStream.close(); //关闭输入流
            outStream.close(); //关闭输出流
            log.info(hdfsURL + "下载成功.");
        }catch (IOException e){
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
