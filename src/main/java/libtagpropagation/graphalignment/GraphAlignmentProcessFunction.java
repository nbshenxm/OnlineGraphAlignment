package libtagpropagation.graphalignment;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraphSeedSearching;
import org.apache.commons.math3.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.A;
import provenancegraph.*;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class GraphAlignmentProcessFunction
        extends KeyedProcessFunction<UUID, AssociatedEvent, String>{

    private static final String knowledgeGraphPath = "TechniqueKnowledgeGraph/ManualCrafted";
    private transient ListState<TechniqueKnowledgeGraph> tkgList;
    private transient ValueState<TechniqueKnowledgeGraphSeedSearching> seedSearching; // ToDo：和直接存变量有什么区别？

    private transient ValueState<Boolean> isInitialized;
    private transient MapState<UUID, GraphAlignmentMultiTag> tagsCacheMap;

    @Override
    public void open(Configuration parameter) {
        ValueStateDescriptor<Boolean> isInitializedDescriptor =
                new ValueStateDescriptor<>("isInitialized", Boolean.class, false);
        this.isInitialized = getRuntimeContext().getState(isInitializedDescriptor);

        ListStateDescriptor<TechniqueKnowledgeGraph> tkgListDescriptor =
                new ListStateDescriptor<>("tkgListStatus", TechniqueKnowledgeGraph.class);
        this.tkgList = getRuntimeContext().getListState(tkgListDescriptor);

        ValueStateDescriptor<TechniqueKnowledgeGraphSeedSearching> seedSearchingDescriptor =
                new ValueStateDescriptor<>("seedSearching", TechniqueKnowledgeGraphSeedSearching.class, null);
        this.seedSearching = getRuntimeContext().getState(seedSearchingDescriptor);

        MapStateDescriptor<UUID, GraphAlignmentMultiTag> tagCacheStateDescriptor =
                new MapStateDescriptor<>("tagsCacheMap", UUID.class, GraphAlignmentMultiTag.class);
        tagsCacheMap = getRuntimeContext().getMapState(tagCacheStateDescriptor);
    }

    @Override
    public void processElement(AssociatedEvent associatedEvent,
                               KeyedProcessFunction<UUID, AssociatedEvent, String>.Context context,
                               Collector<String> collector) throws IOException {
        if (!this.isInitialized.value()){
            init(knowledgeGraphPath);
            this.isInitialized.update(true);
        }
        try {
            tryInitGraphAlignmentTag(associatedEvent); // 先将标签初始化到SourceNode上，再考虑是不是需要传播
            propagateGraphAlignmentTag(associatedEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(String knowledgeGraphPath) {
        try {
            loadTechniqueKnowledgeGraphList(knowledgeGraphPath);
//            ArrayList<TechniqueKnowledgeGraph> techniqueKnowledgeGraphs = new ArrayList<>();
//            this.tkgList.get().forEach(techniqueKnowledgeGraphs::add);
            TechniqueKnowledgeGraphSeedSearching techniqueKnowledgeGraphSeedSearching = new TechniqueKnowledgeGraphSeedSearching(this.tkgList.get());
            this.seedSearching.update(techniqueKnowledgeGraphSeedSearching);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTechniqueKnowledgeGraphList(String kgFilesPath) throws Exception {
        File file = new File(kgFilesPath);
        File[] kgFileList = file.listFiles();
        assert kgFileList != null;
        for (File kgFile : kgFileList) {
            String kgFileName = kgFile.toString();
            TechniqueKnowledgeGraph tkg = new TechniqueKnowledgeGraph(kgFileName);
            this.tkgList.add(tkg);
        }
    }

    private GraphAlignmentMultiTag tryInitGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        Set<TechniqueKnowledgeGraph> initTkgList = new HashSet<>();

        initTkgList.addAll(this.seedSearching.value().search(associatedEvent.sourceNode));
        initTkgList.addAll(this.seedSearching.value().search(associatedEvent)); // 即匹配事件又匹配节点是为了减少标签初始化的量 ToDo：事件匹配时，标签是否应该放到两个节点上

        if (initTkgList.isEmpty()) return null;
        else {
            GraphAlignmentMultiTag multiTag = new GraphAlignmentMultiTag(initTkgList);
            if (this.tagsCacheMap.contains(associatedEvent.sourceNodeId)) {
                this.tagsCacheMap.get(associatedEvent.sourceNodeId).mergeMultiTag(multiTag); // ToDo：直接用tkgList合并是否会更快
            }
            else{
                this.tagsCacheMap.put(associatedEvent.sourceNodeId, multiTag);
            }

            return multiTag;
        }
    }

    private GraphAlignmentMultiTag propagateGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {

        GraphAlignmentMultiTag destMultiTag = tagsCacheMap.get(associatedEvent.sinkNodeId);
        // iterate through tagsCacheMap to check if any existing tags can be propagated
        Iterable<Map.Entry<UUID, GraphAlignmentMultiTag>> entries = tagsCacheMap.entries();
        for (Map.Entry<UUID, GraphAlignmentMultiTag> entry : entries) {
            GraphAlignmentMultiTag graphAlignmentMultiTag = entry.getValue();
            UUID uuid = entry.getKey();
            // if there is a match in GraphAlignmentTag with current associatedEvent, prop the tag
            if (uuid.equals(associatedEvent.sourceNodeId)) {
                GraphAlignmentMultiTag newTags = graphAlignmentMultiTag.propagate(associatedEvent);

                //multiTag merge
                if(destMultiTag == null)
                    this.tagsCacheMap.put(associatedEvent.sinkNodeId, newTags);
                else destMultiTag.mergeMultiTag(newTags);
            }
        }

        return tagsCacheMap.get(associatedEvent.sinkNodeId);
    }

}
