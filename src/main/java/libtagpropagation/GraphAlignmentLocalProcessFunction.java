package libtagpropagation;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import provenancegraph.*;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map;
import java.util.regex.Pattern;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import static libtagpropagation.TechniqueKnowledgeGraph.isEdgeAligned;
import static libtagpropagation.TechniqueKnowledgeGraph.isVertexAligned;


public class GraphAlignmentLocalProcessFunction
        extends KeyedProcessFunction<UUID, AssociatedEvent, String>{

    private static String knowledgeGraphPath = "TechniqueKnowledgeGraph/ManualCrafted";
    private transient ValueState<Boolean> isInitialized;
    private transient MapState<UUID, GraphAlignmentTagList> tagsCacheMap;
    private transient MapState<UUID, BasicNode> nodeInfoMap;
    private transient ListState<TechniqueKnowledgeGraph> tkgList;
    private transient MapState<Edge, TechniqueKnowledgeGraph> seedEventMap;
    private transient MapState<Vertex, TechniqueKnowledgeGraph> seedNodeMap; // TODO: further divide nodes by types


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

        MapStateDescriptor<Edge, TechniqueKnowledgeGraph> seedEventMapDescriptor =
                new MapStateDescriptor<>("seedEventMap", Edge.class, TechniqueKnowledgeGraph.class);
        this.seedEventMap = getRuntimeContext().getMapState(seedEventMapDescriptor);

        MapStateDescriptor<Vertex, TechniqueKnowledgeGraph> seedNodeMapDescriptor =
                new MapStateDescriptor<Vertex, TechniqueKnowledgeGraph>("seedNodeMap", Vertex.class, TechniqueKnowledgeGraph.class);
        this.seedNodeMap = getRuntimeContext().getMapState(seedNodeMapDescriptor);
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
//            System.out.println(kgFileName);
            TechniqueKnowledgeGraph tkg = new TechniqueKnowledgeGraph(kgFileName);
            this.tkgList.add(tkg);
        }
        Iterable<TechniqueKnowledgeGraph> iter = this.tkgList.get();
        System.out.println(iter.toString());
        Iterator<TechniqueKnowledgeGraph> i = iter.iterator();
        int count = 0;
        while(i.hasNext()){
            count ++;
            TechniqueKnowledgeGraph x = i.next();
            System.out.println(x.tinkerGraph);
            System.out.println(x.techniqueName);
        }
        System.out.println(count);

    }

    private void initTKGMatchingSeeds() {
        try {
//            System.out.println("Printing out tkgs");
            Iterable<TechniqueKnowledgeGraph> iter = this.tkgList.get();
//            System.out.println(iter.toString());
            Iterator<TechniqueKnowledgeGraph> i = iter.iterator();
            while (i.hasNext()) {
                TechniqueKnowledgeGraph tkg = i.next();
//                System.out.println(tkg.techniqueName);
//                System.out.println(tkg.getSeedObjects());
                ArrayList<Object> seedObjects = tkg.getSeedObjects();
                for (Object seedObject: seedObjects) {
                    if (seedObject instanceof Vertex) {
                        this.seedNodeMap.put((Vertex) seedObject, tkg);
                    } else if (seedObject instanceof Edge) {
                        this.seedEventMap.put((Edge) seedObject, tkg);
                    }
                }
            }
            System.out.println("Printing out seed nodes");
            for(Vertex v : this.seedNodeMap.keys()){
                System.out.println(this.seedNodeMap.get(v).techniqueName);
            }
            System.out.println("Done");
        }
        catch (Exception e) {
            System.out.println("Something wrong in TechniqueKnowledgeGraph ListState.");
            e.printStackTrace();
        }
    }

    private void initGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        Iterable<Map.Entry<Vertex, TechniqueKnowledgeGraph>> node_entries = seedNodeMap.entries();
        for (Map.Entry<Vertex, TechniqueKnowledgeGraph> entry : node_entries) {
            Vertex seedNode = entry.getKey();
            TechniqueKnowledgeGraph tkg = entry.getValue();
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

        Iterable<Map.Entry<Edge, TechniqueKnowledgeGraph>> edge_entries = seedEventMap.entries();
        for (Map.Entry<Edge, TechniqueKnowledgeGraph> entry : edge_entries) {
            Edge seedEdge = entry.getKey();
            TechniqueKnowledgeGraph tkg = entry.getValue();
            if (isEdgeAligned(seedEdge, associatedEvent)) {
                GraphAlignmentTag tag = new GraphAlignmentTag(seedEdge, associatedEvent, tkg);
                addTagToCache(tag, associatedEvent.sourceNodeId);
                addTagToCache(tag, associatedEvent.sinkNodeId);
            }
        }
    }

    private void propGraphAlignmentTag(AssociatedEvent associatedEvent) throws Exception {
        GraphAlignmentTagList srcTagList = tagsCacheMap.get(associatedEvent.sourceNode.getNodeId());
        GraphAlignmentTagList destTagList = tagsCacheMap.get(associatedEvent.sinkNode.getNodeId());
//        System.out.println(associatedEvent);
//        System.out.println("heyo");
//        System.out.println(tagsCacheMap.isEmpty());
//        for(Map.Entry<UUID, GraphAlignmentTagList> e : tagsCacheMap.entries()){
//            System.out.println(e.getKey());
//            System.out.println(e.getValue());
//        }
//        System.out.println("done");
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
                if(srcTagList != null){
                    for (GraphAlignmentTag anotherTag : srcTagList.getTagList()) { // This srcTagList can be null rn. Happens when there is no tagsCacheMap key for the source node id.
                        if (tag.sameAs(anotherTag)) {
                            tag.mergeStatus(tag);
                            anotherTag.mergeStatus(tag);
                        }
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
