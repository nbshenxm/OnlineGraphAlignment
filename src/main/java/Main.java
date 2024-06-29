import com.twitter.chill.protobuf.ProtobufSerializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import provenancegraph.AssociatedEvent;
import provenancegraph.datamodel.PDM;
import utils.KafkaConfig;
import utils.KafkaPDMDeserializer;

import static anomalypath.EventFrequencyDBConstructionWithFlink.EventFrequencyDBConstructionHandler;
import static anomalypath.TagBasedAnomalyPathMiningOnFlink.AnomalyPathMiningHandler;

public class Main {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaConfig kafkaConfig = new KafkaConfig();
        String kafkaBroker = kafkaConfig.getKafkaBroker();
        String kafkaGroupId = kafkaConfig.getKafkaGroupID();
        String kafkaTopic = kafkaConfig.getTopic("Topic-win10-0");

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

        // Execute the Flink job
        env.execute("Online Flink");
    }
}
