package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;

import java.util.*;

public class TechniqueKnowledgeGraphSeedSearching {

    private Map<SeedNode, TechniqueKnowledgeGraph> seedNodeSearchMap;
    private Map<SeedEdge, TechniqueKnowledgeGraph> seedEdgeSearchMap;

    private Map<UUID, List<Tuple2<Integer, TechniqueKnowledgeGraph>>> searchedNodeCache;
    private Map<UUID, List<Tuple2<Integer, TechniqueKnowledgeGraph>>> searchedEdgeCache;

    public TechniqueKnowledgeGraphSeedSearching(Iterable<TechniqueKnowledgeGraph> tkgList) {
        this.seedEdgeSearchMap = new HashMap<>();
        this.seedNodeSearchMap = new HashMap<>();
        this.searchedEdgeCache = new HashMap<>();
        this.searchedNodeCache = new HashMap<>();
        for (TechniqueKnowledgeGraph tkg : tkgList){
            addTechniqueKnowledgeGraph(tkg);
        }

        // Print seedNodes and seedEdges
        System.out.println("Print seedNodes and seedEdges in TKGs");
        for(Map.Entry entry : seedNodeSearchMap.entrySet()){
            SeedNode seedNode = (SeedNode) entry.getKey();
            TechniqueKnowledgeGraph tkg = (TechniqueKnowledgeGraph) entry.getValue();
            System.out.println(seedNode.toString() + " " + tkg.techniqueName);
        }

        for(Map.Entry entry : seedEdgeSearchMap.entrySet()){
            SeedEdge seedNode = (SeedEdge) entry.getKey();
            TechniqueKnowledgeGraph tkg = (TechniqueKnowledgeGraph) entry.getValue();
            System.out.println(seedNode.toString() + " " + tkg.techniqueName);
        }
    }

    public void addTechniqueKnowledgeGraph(TechniqueKnowledgeGraph tkg) {
        // 加载
        ArrayList<Object> seedObjects = tkg.getSeedObjects();
        for (Object seedObject: seedObjects) {
            if (seedObject instanceof Vertex) {
                SeedNode seedNode = new SeedNode((Vertex) seedObject);
                this.seedNodeSearchMap.put(seedNode, tkg);
            } else if (seedObject instanceof Edge) {
                SeedEdge seedEdge = new SeedEdge((Edge) seedObject);
                this.seedEdgeSearchMap.put(seedEdge, tkg);
            }
        }
    }

    public List<Tuple2<Integer, TechniqueKnowledgeGraph>> search(BasicNode candidateNode) {
        // 缓存查询过的节点
        if (this.searchedNodeCache.containsKey(candidateNode.getNodeId())) {
            return this.searchedNodeCache.get(candidateNode.getNodeId());
        }

        else {
            ArrayList<Tuple2<Integer, TechniqueKnowledgeGraph>> techniqueKnowledgeGraphs = new ArrayList<>();
            for (Map.Entry entry : this.seedNodeSearchMap.entrySet()) {
                SeedNode seedNode = (SeedNode) entry.getKey();
                if (seedNode.isNodeAligned(candidateNode, candidateNode.getProperties())) { // ToDo：不要用全局的函数，改到SeedNode和SeedEdge类里
                    techniqueKnowledgeGraphs.add(Tuple2.of(seedNode.getId(), (TechniqueKnowledgeGraph) entry.getValue()));
                }
            }

            this.searchedNodeCache.put(candidateNode.getNodeId(), techniqueKnowledgeGraphs);
            return techniqueKnowledgeGraphs;
        }
    }

    public List<Tuple2<Integer, TechniqueKnowledgeGraph>> search(AssociatedEvent candidateEdge) {
        // ToDo：加上缓存
        if (this.searchedEdgeCache.containsKey(candidateEdge.edgeId)) {
            return this.searchedEdgeCache.get(candidateEdge.edgeId);
        }
        else {
            ArrayList<Tuple2<Integer, TechniqueKnowledgeGraph>> techniqueKnowledgeGraphs = new ArrayList<>();
            for (Map.Entry entry : seedEdgeSearchMap.entrySet()) {
                SeedEdge seedEdge = (SeedEdge) entry.getKey();
                if (seedEdge.isEdgeAligned(candidateEdge)) {
                    techniqueKnowledgeGraphs.add(Tuple2.of(seedEdge.getId(), (TechniqueKnowledgeGraph) entry.getValue()));
                }
            }
            this.searchedEdgeCache.put(candidateEdge.edgeId, techniqueKnowledgeGraphs);
            return techniqueKnowledgeGraphs;
        }
    }
}
