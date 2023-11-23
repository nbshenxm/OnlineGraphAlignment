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
    private static int processEventCount = 0;
    private static int multiTagCount = 0;

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
        processEventCount++;
        System.out.println(String.format("\n########################################第%d事件正在处理############################################################", processEventCount));
        System.out.println("---------------------tryInitGraphAlignmentTag---------------------");
        Set<Tuple2<SeedNode, TechniqueKnowledgeGraph>> initTkgList = new HashSet<>();

        initTkgList.addAll(this.seedSearching.value().search(associatedEvent.sourceNode));
        // 记录到source上传播到sink上面
        initTkgList.addAll(this.seedSearching.value().search(associatedEvent)); // 即匹配事件又匹配节点是为了减少标签初始化的量

        if (initTkgList.isEmpty()) {
            System.out.println("该事件没有匹配到seedNode and seedEdge\n");
        }else{
            System.out.println("搜索到的seed节点:");
            for (Tuple2<SeedNode, TechniqueKnowledgeGraph> item : initTkgList){
                System.out.println("node " + item.f0.getId() + "  tkg: " + item.f1.techniqueName);
            }
            System.out.println();
        }

        if (initTkgList.isEmpty()) return null;
        else {
            GraphAlignmentMultiTag multiTag = new GraphAlignmentMultiTag(initTkgList);
            if (this.tagsCacheMap.contains(associatedEvent.sourceNode.getNodeId())) {
                this.tagsCacheMap.get(associatedEvent.sourceNode.getNodeId()).mergeMultiTag(multiTag); // ToDo：直接用tkgList合并是否会更快
            }
            else{
                this.tagsCacheMap.put(associatedEvent.sourceNode.getNodeId(), multiTag);
                multiTagCount ++;
            }

            return multiTag;
        }
    }

    // TODo 标签统计，处理的事件量  关键的状态变化，初始化的标签
    private GraphAlignmentMultiTag propagateGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {

        System.out.println("---------------------propagateGraphAlignmentTag------------------------");
        if (processEventCount != 0){
            System.out.println("multiTagCount: " + multiTagCount +
                    "\tprocessEventCount: " + processEventCount +
                    "\ncontinue..."
            );
        }

        GraphAlignmentMultiTag srcMultiTag = tagsCacheMap.get(associatedEvent.sourceNode.getNodeId());
        if (srcMultiTag != null) {
            GraphAlignmentMultiTag sinkMultiTag = tagsCacheMap.get(associatedEvent.sinkNode.getNodeId());
            GraphAlignmentMultiTag newTags = srcMultiTag.propagate(associatedEvent);

            // merge tag
            if (sinkMultiTag == null) {
                System.out.println("\nsinkMultiTag为null无需Merge");
                this.tagsCacheMap.put(associatedEvent.sinkNode.getNodeId(), newTags);
            } else {
                System.out.println("\n--------------------merge Tag-------------------------");
                sinkMultiTag.mergeMultiTag(newTags);
                newTags.mergeMultiTag(sinkMultiTag);
            }
        }
        System.out.println("##########################################end##########################################################");
        return tagsCacheMap.get(associatedEvent.sinkNode.getNodeId());
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


