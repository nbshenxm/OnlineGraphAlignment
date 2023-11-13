package libtagpropagation.graphalignment.techniqueknowledgegraph;

import provenancegraph.AssociatedEvent;
import provenancegraph.BasicEdge;
import provenancegraph.BasicNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TechniqueKnowledgeGraphSeedSearching {

    private Map<SeedNode, TechniqueKnowledgeGraph> seedNodeSearchMap;
    private Map<SeedEdge, TechniqueKnowledgeGraph> seedEdgeSearchMap;

    private Map<UUID, List<TechniqueKnowledgeGraph>> searchedNodeCache;

    public TechniqueKnowledgeGraphSeedSearching(List<TechniqueKnowledgeGraph> tkgList) {

    }

    public void addTechniqueKnowledgeGraph(TechniqueKnowledgeGraph tkg) {
        // 加载
    }

    public List<TechniqueKnowledgeGraph> search(BasicNode candidateNode) {
        // 缓存查询过的节点
        if (searchedNodeCache.containsKey(candidateNode.getNodeId())) {
            return searchedNodeCache.get(candidateNode.getNodeId());
        }

        else
    }

    public List<TechniqueKnowledgeGraph> search(AssociatedEvent candidateEdge) {

    }
}
