package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;

import java.util.*;

public class TechniqueKnowledgeGraphSeedSearching {

    private Map<SeedNode, TechniqueKnowledgeGraph> seedNodeSearchMap;
    private Map<SeedEdge, TechniqueKnowledgeGraph> seedEdgeSearchMap;

    private Map<UUID, List<TechniqueKnowledgeGraph>> searchedNodeCache;
    private Map<UUID, List<TechniqueKnowledgeGraph>> searchedEdgeCache;

    public TechniqueKnowledgeGraphSeedSearching(Iterable<TechniqueKnowledgeGraph> tkgList) {
        this.seedEdgeSearchMap = new HashMap<>();
        this.seedNodeSearchMap = new HashMap<>();
        this.searchedEdgeCache = new HashMap<>();
        this.searchedNodeCache = new HashMap<>();
        for (TechniqueKnowledgeGraph tkg : tkgList){
            addTechniqueKnowledgeGraph(tkg);
        }
    }

    public void addTechniqueKnowledgeGraph(TechniqueKnowledgeGraph tkg) {
        // 加载
        ArrayList<Object> seedObjects = tkg.getSeedObjects();
        for (Object vertex : seedObjects){
            SeedNode seedNode = new SeedNode((Vertex) vertex);
            this.seedNodeSearchMap.put(seedNode, tkg);
        }

        ArrayList<Edge> edgeList = tkg.getEdgeList();
        for (Edge edge : edgeList){
            SeedEdge seedEdge = new SeedEdge(edge);
            this.seedEdgeSearchMap.put(seedEdge, tkg);
        }
    }

    public List<TechniqueKnowledgeGraph> search(BasicNode candidateNode) {
        // 缓存查询过的节点
        if (this.searchedNodeCache.containsKey(candidateNode.getNodeId())) {
            return this.searchedNodeCache.get(candidateNode.getNodeId());
        }

        else {
            ArrayList<TechniqueKnowledgeGraph> techniqueKnowledgeGraphs = new ArrayList<>();
            for (Map.Entry entry : this.seedNodeSearchMap.entrySet()) {
                SeedNode seedNode = (SeedNode) entry.getKey();
                if (seedNode.isNodeAligned(candidateNode, candidateNode.getProperties())) { // ToDo：不要用全局的函数，改到SeedNode和SeedEdge类里
                    techniqueKnowledgeGraphs.add((TechniqueKnowledgeGraph) entry.getValue());
                }
            }

            this.searchedNodeCache.put(candidateNode.getNodeId(), techniqueKnowledgeGraphs);
            return techniqueKnowledgeGraphs;
        }
    }

    public List<TechniqueKnowledgeGraph> search(AssociatedEvent candidateEdge) {
        // ToDo：加上缓存
        if (this.searchedEdgeCache.containsKey(candidateEdge.edgeId)) {
            return this.searchedEdgeCache.get(candidateEdge.edgeId);
        }
        else {
            ArrayList<TechniqueKnowledgeGraph> techniqueKnowledgeGraphs = new ArrayList<>();
            for (Map.Entry entry : seedEdgeSearchMap.entrySet()) {
                SeedEdge seedEdge = (SeedEdge) entry.getKey();
                if (seedEdge.isEdgeAligned(candidateEdge)) {
                    techniqueKnowledgeGraphs.add((TechniqueKnowledgeGraph) entry.getValue());
                }
            }
            this.searchedEdgeCache.put(candidateEdge.edgeId, techniqueKnowledgeGraphs);
            return techniqueKnowledgeGraphs;
        }
    }
}
