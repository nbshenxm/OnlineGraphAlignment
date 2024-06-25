package libtagpropagation;

import com.twitter.chill.protobuf.ProtobufSerializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import provenancegraph.AssociatedEvent;
import provenancegraph.datamodel.PDM;
import utils.KafkaPDMDeserializer;
import static libtagpropagation.anomalypath.EventFrequencyDBConstructionWithFlink.EventFrequencyDBConstructionHandler;
import static libtagpropagation.anomalypath.TagBasedAnomalyPathMiningOnFlink.AnomalyPathMiningHandler;

public class Main {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

            String kafkaBroker = "192.168.10.110:9092";
//            String kafkaBroker = "localhost:9092";
//            String kafkaBroker = "10.105.189.7:9092";
//            String kafkaBroker = "192.168.0.173:9092";
//            String kafkaTopic = "topic-HipsToMrd";
//            String kafkaTopic = "topic-Linux-malicious";
//            String kafkaTopic = "topic-CADETS-0-train";
//            String kafkaTopic = "topic-CADETS-0-test";
            String kafkaTopic = "topic-CADETS-0";
            String kafkaGroupId = "mergeAlert";

            env.getConfig().registerTypeWithKryoSerializer(PDM.LogPack.class, ProtobufSerializer.class);
            KafkaSource<PDM.LogPack> source = KafkaSource.<PDM.LogPack>builder()
                    .setBootstrapServers(kafkaBroker)
                    .setTopics(kafkaTopic)
                    .setGroupId(kafkaGroupId)
                    .setStartingOffsets(OffsetsInitializer.earliest())
                    .setValueOnlyDeserializer(new KafkaPDMDeserializer())
                    .build();

            DataStream<PDM.LogPack> logPack_stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
            EventFrequencyDBConstructionHandler(logPack_stream);
//            AnomalyPathMiningHandler(logPack_stream);

//        event_stream.keyBy(associatedEvent -> associatedEvent.hostUUID)
//                .process(new GraphAlignmentProcessFunction());

        // Execute the Flink job
        env.execute("Online Flink");
    }

}
