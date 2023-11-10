package libtagpropagation.graphalignment.techniqueknowledgegraph;

import provenancegraph.AssociatedEvent;
import provenancegraph.BasicEdge;
import provenancegraph.BasicNode;

import java.util.List;
import java.util.Map;

public class TechniqueKnowledgeGraphSeedSearching {

    private Map<SeedNode, TechniqueKnowledgeGraph> seedNodeSearchMap;
    private Map<SeedEdge, TechniqueKnowledgeGraph> seedEdgeSearchMap;

    public TechniqueKnowledgeGraphSeedSearching(List<TechniqueKnowledgeGraph> tkgList) {

    }

    public void addTechniqueKnowledgeGraph(TechniqueKnowledgeGraph tkg) {
        // 加载
    }

    public List<TechniqueKnowledgeGraph> search(BasicNode candidateNode) {

    }

    public List<TechniqueKnowledgeGraph> search(AssociatedEvent candidateEdge) {

    }
}
