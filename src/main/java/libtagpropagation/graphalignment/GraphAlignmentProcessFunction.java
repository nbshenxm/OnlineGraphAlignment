package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.techniqueknowledgegraph.SeedNode;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraphSeedSearching;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
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
    public static int processEventCount = 0;
    public static int initTagCount = 0;
    public static int propagateTagCount = 0;
    public static int multiTagCount = 0;
    public static int tagCount = 0;
    public static int removeTagCount = 0;

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

    // ToDo：考虑边的匹配状态
    // TODO 生产事件去匹配tkg
    @Override
    public void processElement(AssociatedEvent associatedEvent,
                               KeyedProcessFunction<UUID, AssociatedEvent, String>.Context context,
                               Collector<String> collector) throws IOException {
        processEventCount++;
        tagCount = propagateTagCount + initTagCount;
        if (processEventCount % 100000 == 0){
            System.out.println("处理的事件数量：" + processEventCount + "\n多标签数量： " + multiTagCount + "\n标签数量： " + tagCount
                    + "\n初始化标签数量： " + initTagCount + "  传播标签数量： " + propagateTagCount + "  移除标签的数量:  " + removeTagCount
                    + "\n当前时间： " + associatedEvent.timeStamp
                    + "\n...\n"
            );
        }

        if (!this.isInitialized.value()){
            init(knowledgeGraphPath);
            this.isInitialized.update(true);
            System.out.println("\nOnline Alignment start ......\n");
        }
        try {
            tryInitGraphAlignmentTag(associatedEvent);
            propagateGraphAlignmentTag(associatedEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(String knowledgeGraphPath) {
        try {
            loadTechniqueKnowledgeGraphList(knowledgeGraphPath);
            int count = 0;
            String out = "";
            for (TechniqueKnowledgeGraph tkg : this.tkgList.get()){
                count ++;
                out += tkg.techniqueName + "\n";
            }
            System.out.println("TKG 总数是 ：" + count + "\n" + out);
            this.seedSearching.update(new TechniqueKnowledgeGraphSeedSearching(this.tkgList.get()));

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
        Set<Tuple2<SeedNode, TechniqueKnowledgeGraph>> initTkgList = new HashSet<>();

        initTkgList.addAll(this.seedSearching.value().search(associatedEvent.sourceNode));
        // 记录到source上传播到sink上面
        initTkgList.addAll(this.seedSearching.value().search(associatedEvent));

        if (initTkgList.isEmpty()) return null;
        else {
            GraphAlignmentMultiTag multiTag = new GraphAlignmentMultiTag(initTkgList, associatedEvent.sourceNode.getNodeUUID());
            if (this.tagsCacheMap.contains(associatedEvent.sourceNode.getNodeUUID())) {
                this.tagsCacheMap.get(associatedEvent.sourceNode.getNodeUUID()).mergeMultiTag(multiTag);
            }
            else{
                this.tagsCacheMap.put(associatedEvent.sourceNode.getNodeUUID(), multiTag);
                multiTagCount ++;
            }
            return multiTag;
        }
    }

    private GraphAlignmentMultiTag propagateGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        GraphAlignmentMultiTag srcMultiTag = tagsCacheMap.get(associatedEvent.sourceNode.getNodeUUID());
        if (srcMultiTag != null) {
            if (srcMultiTag.getTagMap().size() == 0) {
                this.tagsCacheMap.remove(associatedEvent.sourceNode.getNodeUUID());
            }
            GraphAlignmentMultiTag sinkMultiTag = tagsCacheMap.get(associatedEvent.sinkNode.getNodeUUID());
            GraphAlignmentMultiTag newTags = srcMultiTag.propagate(associatedEvent);

            // merge tag
            if (sinkMultiTag == null) {
                if (newTags.getTagMap().size() > 0){
                    this.tagsCacheMap.put(associatedEvent.sinkNode.getNodeUUID(), newTags);
                    multiTagCount ++;
                }
            } else {
                sinkMultiTag.mergeMultiTag(newTags);
                newTags.mergeMultiTag(sinkMultiTag);
            }
        }
        return tagsCacheMap.get(associatedEvent.sinkNode.getNodeUUID());
    }

    @Override
    public String toString() {
        return "GraphAlignmentProcessFunction{" +
                "tkgList=" + tkgList +
                ", seedSearching=" + seedSearching +
                ", isInitialized=" + isInitialized +
                ", tagsCacheMap=" + tagsCacheMap +
                '}';
    }
}


