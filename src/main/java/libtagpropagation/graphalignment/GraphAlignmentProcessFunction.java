package libtagpropagation.graphalignment;

import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import provenancegraph.*;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import static libtagpropagation.graphalignment.TechniqueKnowledgeGraph.isEdgeAligned;
import static libtagpropagation.graphalignment.TechniqueKnowledgeGraph.isVertexAligned;


public class GraphAlignmentProcessFunction
        extends KeyedProcessFunction<UUID, AssociatedEvent, String>{

    private static String knowledgeGraphPath = "TechniqueKnowledgeGraph/ManualCrafted";
    private transient ValueState<Boolean> isInitialized;
    private transient MapState<UUID, GraphAlignmentTagList> tagsCacheMap;
    private transient MapState<UUID, BasicNode> nodeInfoMap;
    private transient ListState<TechniqueKnowledgeGraph> tkgList;
    private transient ListState<Vertex> seedNodeList;
    private transient ListState<Edge> seedEdgeList;
    private transient MapState<Object, TechniqueKnowledgeGraph> seedEventMap;
    private transient MapState<Object, TechniqueKnowledgeGraph> seedNodeMap; // TODO: further divide nodes by types


    @Override
    public void open(Configuration parameter) {
        ValueStateDescriptor<Boolean> isInitializedDescriptor =
                new ValueStateDescriptor<>("isInitialized", Boolean.class, false);
        this.isInitialized = getRuntimeContext().getState(isInitializedDescriptor);

        MapStateDescriptor<UUID, GraphAlignmentTagList> tagCacheStateDescriptor =
                new MapStateDescriptor<>("tagsCacheMap", UUID.class, GraphAlignmentTagList.class);
        tagsCacheMap = getRuntimeContext().getMapState(tagCacheStateDescriptor);

        MapStateDescriptor<UUID, BasicNode> nodeInfoStateDescriptor =
                new MapStateDescriptor<>("nodeInfoMap", UUID.class, BasicNode.class);
        nodeInfoMap = getRuntimeContext().getMapState(nodeInfoStateDescriptor);

        ListStateDescriptor<TechniqueKnowledgeGraph> tkgListDescriptor =
                new ListStateDescriptor<>("tkgListStatus", TechniqueKnowledgeGraph.class);
        this.tkgList = getRuntimeContext().getListState(tkgListDescriptor);

        MapStateDescriptor<Object, TechniqueKnowledgeGraph> seedEventMapDescriptor =
                new MapStateDescriptor<>("seedEventMap", Object.class, TechniqueKnowledgeGraph.class);
        this.seedEventMap = getRuntimeContext().getMapState(seedEventMapDescriptor);

        MapStateDescriptor<Object, TechniqueKnowledgeGraph> seedNodeMapDescriptor =
                new MapStateDescriptor<>("seedNodeMap", Object.class, TechniqueKnowledgeGraph.class);
        this.seedNodeMap = getRuntimeContext().getMapState(seedNodeMapDescriptor);

        ListStateDescriptor<Vertex> seedNodeListDescriptor =
                new ListStateDescriptor<>("seedNodeList", Vertex.class);
        this.seedNodeList = getRuntimeContext().getListState(seedNodeListDescriptor);

        ListStateDescriptor<Edge> seedEdgeListDescriptor =
                new ListStateDescriptor<>("seedEdgeList", Edge.class);
        this.seedEdgeList = getRuntimeContext().getListState(seedEdgeListDescriptor);
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
            propGraphAlignmentTag(associatedEvent);
            initGraphAlignmentTag(associatedEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(String knowledgeGraphPath) {
        try {
            initTechniqueKnowledgeGraphList(knowledgeGraphPath);
            initTKGMatchingSeeds();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTechniqueKnowledgeGraphList(String kgFilesPath) throws Exception {
        File file = new File(kgFilesPath);
        File[] kgFileList = file.listFiles();
        assert kgFileList != null;
        for (File kgFile : kgFileList) {
            String kgFileName = kgFile.toString();
            TechniqueKnowledgeGraph tkg = new TechniqueKnowledgeGraph(kgFileName);
            this.tkgList.add(tkg);
        }
    }

    private void initTKGMatchingSeeds() {
        try {
            for (TechniqueKnowledgeGraph tkg : this.tkgList.get()) {
                ArrayList<Object> seedObjects = tkg.getSeedObjects();
                for (Object seedObject: seedObjects) {
                    if (seedObject instanceof Vertex) {
                        this.seedNodeMap.put(((Vertex) seedObject).getId(), tkg);
                        this.seedNodeList.add((Vertex) seedObject);
                    } else if (seedObject instanceof Edge) {
                        this.seedEventMap.put(((Edge) seedObject).getId(), tkg);
                        this.seedEdgeList.add((Edge) seedObject);
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Something wrong in TechniqueKnowledgeGraph ListState.");
            e.printStackTrace();
        }
    }

    private void initGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
//        Iterable<Map.Entry<Vertex, TechniqueKnowledgeGraph>> node_entries = seedNodeMap.entries();
        for (Vertex seedNode : this.seedNodeList.get()) {
            TechniqueKnowledgeGraph tkg = this.seedNodeMap.get(seedNode.getId());
            BasicNode srcNode = associatedEvent.sourceNode;
            NodeProperties srcNodeProperties = associatedEvent.sourceNodeProperties;
            if (isVertexAligned(seedNode, srcNode, srcNodeProperties)){
                GraphAlignmentTag tag = new GraphAlignmentTag(seedNode, srcNode, tkg);
                addTagToCache(tag, srcNode.getNodeId());
            }
            BasicNode destNode = associatedEvent.sinkNode;
            NodeProperties destNodeProperties = associatedEvent.sinkNodeProperties;
            if (isVertexAligned(seedNode, destNode, destNodeProperties)){
                GraphAlignmentTag tag = new GraphAlignmentTag(seedNode, destNode, tkg);
                addTagToCache(tag, destNode.getNodeId());
            }
        }

//        Iterable<Map.Entry<Edge, TechniqueKnowledgeGraph>> edge_entries = seedEventMap.entries();
        for (Edge seedEdge : this.seedEdgeList.get()) {
            TechniqueKnowledgeGraph tkg = this.seedEventMap.get(seedEdge.getId());
            if (isEdgeAligned(seedEdge, associatedEvent)) {
                GraphAlignmentTag tag = new GraphAlignmentTag(seedEdge, associatedEvent, tkg);
                addTagToCache(tag, associatedEvent.sourceNodeId);
                addTagToCache(tag, associatedEvent.sinkNodeId);
            }
        }
    }

    private void propGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        GraphAlignmentTagList srcTagList = tagsCacheMap.get(associatedEvent.sourceNodeId);
        GraphAlignmentTagList destTagList = tagsCacheMap.get(associatedEvent.sinkNodeId);
        // iterate through tagsCacheMap to check if any existing tags can be propagated
        Iterable<Map.Entry<UUID, GraphAlignmentTagList>> entries = tagsCacheMap.entries();
        for (Map.Entry<UUID, GraphAlignmentTagList> entry : entries) {
            UUID nodeUUID = entry.getKey();
            GraphAlignmentTagList graphAlignmentTagList = entry.getValue();
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



    private void addTagToCache(GraphAlignmentTag tag, UUID nodeId) throws Exception {
        if (!this.tagsCacheMap.contains(nodeId)){
            GraphAlignmentTagList tagList = new GraphAlignmentTagList();
            this.tagsCacheMap.put(nodeId, tagList);
        }
        this.tagsCacheMap.get(nodeId).addTag(tag);
    }

}
