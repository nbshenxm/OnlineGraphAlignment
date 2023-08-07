package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

public class Utils {
    public static Properties loadProperties(String propertyFilePath) throws IOException {
        InputStream inputStream = Files.newInputStream(Paths.get(propertyFilePath));
        Properties properties = new Properties();
        properties.load(inputStream);
        inputStream.close();

        return properties;
    }

    public static Properties loadInnerProperties(String propertyFilePath) throws IOException {
        InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream(propertyFilePath);
        Properties properties = new Properties();
        properties.load(inputStream);
        inputStream.close();

        return properties;
    }

    public static int iterableCount(Iterator<?> it) {
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    public static String convertIntToIpString(int ip) {
        String ipStr = String.format("%d.%d.%d.%d",
                (ip >> 24 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 8 & 0xff),
                (ip & 0xff));
        return ipStr;
    }

    public static String parseTimeStamp(Long timeStamp) {
        return new Date(timeStamp/1000000).toString();
    }
}

