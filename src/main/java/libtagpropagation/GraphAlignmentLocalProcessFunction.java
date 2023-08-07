package libtagpropagation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import provenancegraph.parser.LocalParser;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.Map;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import static utils.Utils.loadProperties;

public class GraphAlignmentLocalProcessFunction
        extends KeyedProcessFunction<UUID, AssociatedEvent, String>{

    private static String knowledgeGraphPath = "TechniqueKnowledgeGraph/ManualCrafted";

    private transient MapState<UUID, GraphAlignmentTag> tagsCacheMap;
    private transient MapState<UUID, BasicNode> nodeInfoMap;
    private transient ListState<TechniqueKnowledgeGraph> tkgList;
    private transient MapState<Edge, TechniqueKnowledgeGraph> seedEventMap;
    private transient MapState<Vertex, TechniqueKnowledgeGraph> seedNodeMap;

    public GraphAlignmentLocalProcessFunction() throws IOException {
        init(knowledgeGraphPath);
    }

    @Override
    public void open(Configuration parameter) {
        MapStateDescriptor<UUID, GraphAlignmentTag> tagCacheStateDescriptor = new MapStateDescriptor<>("tagsCacheMap", UUID.class, GraphAlignmentTag.class);
        tagsCacheMap = getRuntimeContext().getMapState(tagCacheStateDescriptor);
        MapStateDescriptor<UUID, BasicNode> nodeInfoStateDescriptor = new MapStateDescriptor<>("nodeInfoMap", UUID.class, BasicNode.class);
        nodeInfoMap = getRuntimeContext().getMapState(nodeInfoStateDescriptor);
        ListStateDescriptor<TechniqueKnowledgeGraph> tkgListDescriptor =
                new ListStateDescriptor<>("tkgListStatus", TechniqueKnowledgeGraph.class);
        this.tkgList = getRuntimeContext().getListState(tkgListDescriptor);

        MapStateDescriptor<Edge, TechniqueKnowledgeGraph> seedEventMapDescriptor =
                new MapStateDescriptor<>("seedEventMap", Edge.class, TechniqueKnowledgeGraph.class);
        this.seedEventMap = getRuntimeContext().getMapState(seedEventMapDescriptor);

        MapStateDescriptor<Vertex, TechniqueKnowledgeGraph> seedNodeMapDescriptor =
                new MapStateDescriptor<Vertex, TechniqueKnowledgeGraph>("seedNodeMap", Vertex.class, TechniqueKnowledgeGraph.class);
        this.seedNodeMap = getRuntimeContext().getMapState(seedNodeMapDescriptor);
    }

    @Override
    public void processElement(AssociatedEvent associatedEvent, KeyedProcessFunction<UUID, AssociatedEvent, String>.Context context, Collector<String> collector) throws Exception {
        try {
            propGraphAlignmentTag(associatedEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            initGraphAlignmentTag(associatedEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GraphAlignmentTag graphAlignmentTag;

    }

    private void init(String knowledgeGraphPath) {

    }

    private void initGraphAlignmentTag(AssociatedEvent associatedEvent) {

    }

    private void propGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        Iterable<Map.Entry<UUID, GraphAlignmentTag>> entries = tagsCacheMap.entries();
        for (Map.Entry<UUID, GraphAlignmentTag> entry : entries) {
            UUID key = entry.getKey();
            GraphAlignmentTag value = entry.getValue();
        }

    }
}
