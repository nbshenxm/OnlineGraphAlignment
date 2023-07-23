package libtagpropagation;

import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;

import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Main {

    public static void main(String[] args) throws Exception {
        final String inputDirectory = "SystemLog/apt.log"; // Replace with the actual path to your JSON files directory

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        final FileSource<String> source =
                FileSource.forRecordStreamFormat(new TextLineInputFormat(), new Path(inputDirectory) ).build();
        final DataStream<String> stream =
                env.fromSource(source, WatermarkStrategy.noWatermarks(), "file-source");

//        stream.print();
        final DataStream<JsonElement> json_stream = stream.map(Main::convertToJson).name("json-source");
        // TODO: create a KeyedProcessFunction class to process data


        // Execute the Flink job
        env.execute("Read Local JSON Files with Flink");
    }

    private static JsonElement convertToJson(String inputLine) {
        Gson gson = new Gson();

        try {
            JsonElement jsonElement = gson.fromJson(inputLine, JsonElement.class);
            System.out.println(gson.toJson(jsonElement));
            return jsonElement;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
