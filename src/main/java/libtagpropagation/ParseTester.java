package libtagpropagation;
import com.google.gson.Gson;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import com.google.gson.JsonElement;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;
import provenancegraph.NodeProperties;
import com.google.gson.JsonParser;
import provenancegraph.parser.LocalParser;


public class ParseTester {
    public static void main(String[] args) throws Exception{
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<AssociatedEvent> event_stream;
        final String inputDirectory = "SystemLog/apt.log";
        final FileSource<String> source =
                FileSource.forRecordStreamFormat(new TextLineInputFormat(), new Path(inputDirectory) ).build();
        final DataStream<String> stream =
                env.fromSource(source, WatermarkStrategy.noWatermarks(), "file-source");
        final DataStream<JsonElement> json_stream = stream.map(ParseTester::convertToJson).name("json-source");
        event_stream = json_stream.map(LocalParser::initAssociatedEvent);
        event_stream.keyBy(associatedEvent -> associatedEvent.hostUUID)
                .process(new GraphAlignmentLocalProcessFunction());
        env.execute("Test reading in Local JSON Files with Flink");
    }
    private static JsonElement convertToJson(String inputLine) {
        Gson gson = new Gson();

        try {
//            System.out.println(inputLine);
            JsonElement jsonElement = gson.fromJson(inputLine, JsonElement.class);
//            System.out.println(gson.toJson(jsonElement));
            return jsonElement;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("oopsies");
        }
        return null;
    }
}
