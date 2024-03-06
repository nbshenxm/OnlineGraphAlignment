package libtagpropagation;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.AssociatedEvent;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static libtagpropagation.anomalypath.TagBasedAnomalyPathMiningOnFlink.calculateRegularScore;

public class ParseTester {
    public static void main(String[] args) throws Exception{

        ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(Paths.get("./SystemLog/cadetsEventFrequencyDB.out")));
        Tuple2<Map, Map> dbstring = (Tuple2<Map, Map>) objectInputStream.readObject();
        Map<AssociatedEvent, HashSet<String>> exactlyMatchEventFrequencyMap = dbstring.f0;
        Map<AssociatedEvent, HashSet<String>> sourceRelationshipMatchEventFrequencyMap = dbstring.f1;
        Map<AssociatedEvent, Double> map = new HashMap<>();
        for (AssociatedEvent key : exactlyMatchEventFrequencyMap.keySet())
        {
            double freq_event = exactlyMatchEventFrequencyMap.get(key).size();
            double freq_src_rel = sourceRelationshipMatchEventFrequencyMap.get(key.ignoreSink()).size();
            double score = freq_event / freq_src_rel;
            map.put(key, score);
        }

        // 指定CSV文件的路径
        String csvFile = "./SystemLog/e3-cadets.csv";

        try (FileWriter writer = new FileWriter(csvFile)) {
            // 写入CSV文件头
            writer.append("Key,Value\n");

            // 遍历Map并写入CSV
            for (Map.Entry<AssociatedEvent, Double> entry : map.entrySet()) {
                writer.append(entry.getKey().toString())
                        .append(",")
                        .append(entry.getValue().toString())
                        .append("\n");
            }

            System.out.println("CSV file was created successfully !!!");

        } catch (IOException e) {
            System.out.println("An error occurred while writing CSV file.");
            e.printStackTrace();
        }


        int a = 0;

    }

    public static void durationTime(String startTime, String endTime){
//         将字符串转换为 BigInteger 对象
        BigInteger num1 = new BigInteger(startTime);
        BigInteger num2 = new BigInteger(endTime);
        String strNum3 = "3600000000000";
        BigInteger num3 = new BigInteger(strNum3);
        String strNum4 = "60000000000";
        BigInteger num4 = new BigInteger(strNum4);

        // 执行大数相减
//        BigInteger result = num1.divide(num2);
        BigInteger result = num2.subtract(num1);
        BigInteger[] results = result.divideAndRemainder(num3);

        // 输出结果
        System.out.println("Result of subtraction: " + results[0] + " 余： " + results[1].divide(num4));
    }
}
