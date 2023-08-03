package libtagpropagation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import provenancegraph.parser.LocalParser;

import java.util.UUID;

public class GraphAlignmentLocalProcessFunction
        extends KeyedProcessFunction<String, JsonElement, String>{

    private transient MapState<UUID, GraphAlignmentTag> tagsCacheMap;
    private transient MapState<UUID, BasicNode> nodeInfoMap;

    @Override
    public void open(Configuration parameter) {
        MapStateDescriptor<UUID, GraphAlignmentTag> tagCacheStateDescriptor = new MapStateDescriptor<>("tagsCacheMap", UUID.class, GraphAlignmentTag.class);
        tagsCacheMap = getRuntimeContext().getMapState(tagCacheStateDescriptor);
        MapStateDescriptor<UUID, BasicNode> nodeInfoStateDescriptor = new MapStateDescriptor<>("nodeInfoMap", UUID.class, BasicNode.class);
        nodeInfoMap = getRuntimeContext().getMapState(nodeInfoStateDescriptor);
    }

    @Override
    public void processElement(JsonElement jsonElement, KeyedProcessFunction<String, JsonElement, String>.Context context, Collector<String> collector) throws Exception {
        JsonObject rootObject = jsonElement.getAsJsonObject();
        int eventType = rootObject.get("event_type").getAsInt();
        AssociatedEvent associatedEvent = LocalParser.initAssociatedEvent(jsonElement);
        // TODO: initialize and propagation tag
        GraphAlignmentTag graphAlignmentTag;
//        switch (eventType) {
//            case 1: // open
//            case 2: // openat
////                BasicNode node;
//                AssociatedEvent associatedEvent;
//                GraphAlignmentTag graphAlignmentTag;
//                break;
//            case 4: // read
//            case 5: // readv
//            case 6: // pread
//            case 7: // preadv
//                break;
//            default:
//                throw new IllegalStateException("Unexpected value: " + eventType);
//        }
    }
}
