package libtagpropagation.graphalignment;

import com.google.protobuf.DoubleArrayList;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraphSeedSearching;
import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import provenancegraph.*;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;


public class GraphAlignmentProcessFunction
        extends KeyedProcessFunction<UUID, AssociatedEvent, String>{

    private static final String knowledgeGraphPath = "TechniqueKnowledgeGraph/ManualCrafted";
    private transient ListState<TechniqueKnowledgeGraph> tkgList;
    private transient ValueState<TechniqueKnowledgeGraphSeedSearching> seedSearching;

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
                new ValueStateDescriptor<>("seedSearching", TechniqueKnowledgeGraphSeedSearching.class, false);
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
            propGraphAlignmentTag(associatedEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(String knowledgeGraphPath) {
        try {
            loadTechniqueKnowledgeGraphList(knowledgeGraphPath);
            this.seedSearching.update(new TechniqueKnowledgeGraphSeedSearching((List<TechniqueKnowledgeGraph>) this.tkgList));
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
        List<TechniqueKnowledgeGraph> initTkgList = new ArrayList<>();
        initTkgList.addAll(this.seedSearching.value().search(associatedEvent.sourceNode));
        initTkgList.addAll(this.seedSearching.value().search(associatedEvent));

        if (initTkgList.isEmpty()) return null;
        else {
            GraphAlignmentMultiTag multiTag = new GraphAlignmentMultiTag(initTkgList);
            if (this.tagsCacheMap.contains(associatedEvent.sourceNodeId)) {
                this.tagsCacheMap.get(associatedEvent.sourceNodeId).mergeMultiTag(multiTag);
            }
            else{
                CacheMultiTag(associatedEvent.sourceNodeId, multiTag);
            }
            return multiTag;
        }
    }

    private GraphAlignmentMultiTag propGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        if ()

        GraphAlignmentMultiTag srcTagList = tagsCacheMap.get(associatedEvent.sourceNodeId);
        GraphAlignmentMultiTag destTagList = tagsCacheMap.get(associatedEvent.sinkNodeId);
        // iterate through tagsCacheMap to check if any existing tags can be propagated
        Iterable<Map.Entry<UUID, GraphAlignmentMultiTag>> entries = tagsCacheMap.entries();
        for (Map.Entry<UUID, GraphAlignmentMultiTag> entry : entries) {
            UUID nodeUUID = entry.getKey();
            GraphAlignmentMultiTag graphAlignmentTagList = entry.getValue();
            // if there is a match in GraphAlignmentTag with current associatedEvent, prop the tag
            ArrayList<GraphAlignmentTag> tagList = graphAlignmentTagList.getTagList();
            for (GraphAlignmentTag tag: tagList) {
                // tag align
                tag.propagate(associatedEvent);

                // tag merge
                for (GraphAlignmentTag anotherTag : srcTagList.getTagList()) {
                    if (tag.sameAs(anotherTag)) {
                        tag.mergeStatus(tag);
                        anotherTag.mergeStatus(tag);
                    }
                }

                // score update
                tag.updateMatchScore();
                if (tag.isMatched()) {
                    System.out.println("Technique detected.");
                }
            }
        }
    }



    private void CacheMultiTag(UUID nodeId, GraphAlignmentMultiTag multiTag) throws Exception {
        if (!this.tagsCacheMap.contains(nodeId)) this.tagsCacheMap.put(nodeId, multiTag);
        else this.tagsCacheMap.get(nodeId).mergeMultiTag(multiTag);
    }

}
