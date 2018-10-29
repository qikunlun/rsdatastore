package ai.geodata.conf;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Map;

public class Configuration {
    protected static Logger log = Logger.getLogger(Configuration.class);
    private static Map<String, String> configs;

    public Configuration() {
        try {
            Yaml yaml = new Yaml();
            URL url = Configuration.class.getClassLoader().getResource("application.yaml");
            if (url != null) {
                //获取test.yaml文件中的配置数据，然后转换为obj，
                Object obj = yaml.load(new FileInputStream(url.getFile()));
                log.info("加载配置文件: " + obj);
                //也可以将值转换为Map
                configs = yaml.load(new FileInputStream(url.getFile()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public String get(String name){
        if (configs.containsKey(name)){
            return configs.get(name);
        }else{
            log.error("不存在的配置项：" + name);
            return null;
        }
    }


    public static void main(String args[]){
        Configuration conf = new Configuration();
    }
}
