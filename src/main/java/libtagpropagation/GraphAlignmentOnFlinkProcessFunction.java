package libtagpropagation;


import provenancegraph.BasicEdge;
import provenancegraph.BasicNode;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import provenancegraph.datamodel.PDM;
import provenancegraph.parser.PDMParser;

import java.util.ArrayList;
import java.util.UUID;


public class GraphAlignmentOnFlinkProcessFunction
        extends KeyedProcessFunction<PDM.HostUUID, PDM.Log, String> {

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
    public void processElement(PDM.Log log,
                               KeyedProcessFunction<PDM.HostUUID, PDM.Log, String>.Context context,
                               Collector<String> collector) throws Exception {
        if (log.getUHeader().getType() == PDM.LogType.ENTITY) {
            BasicNode node = PDMParser.initBasicNode(log);
            if (node == null) {
                return;
            }
            nodeInfoMap.put(node.getNodeId(), node);
        }
        else if (log.getUHeader().getType() == PDM.LogType.ENTITY) {
            if (log.hasEventData()) {

            }
        }
    }
}
