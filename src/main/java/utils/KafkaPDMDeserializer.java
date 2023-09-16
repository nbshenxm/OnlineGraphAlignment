package utils;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import provenancegraph.datamodel.PDM;

import java.io.IOException;

public class KafkaPDMDeserializer implements DeserializationSchema<PDM.LogPack> {
    @Override
    public boolean isEndOfStream(PDM.LogPack logPack) {
        return false;
    }


    @Override
    public PDM.LogPack deserialize(byte[] bytes) throws IOException {
        return PDM.LogPack.parseFrom(bytes);
    }

    @Override
    public TypeInformation<PDM.LogPack> getProducedType() {
        return TypeInformation.of(new TypeHint<PDM.LogPack>() {});
    }
}