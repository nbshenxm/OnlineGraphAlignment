package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph.isEdgeAligned;
import static libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph.isVertexAligned;

public class TechniqueKnowledgeGraphSeedSearching {

    private Map<SeedNode, TechniqueKnowledgeGraph> seedNodeSearchMap;
    private Map<SeedEdge, TechniqueKnowledgeGraph> seedEdgeSearchMap;

    public TechniqueKnowledgeGraphSeedSearching(List<TechniqueKnowledgeGraph> tkgList) {
        for (TechniqueKnowledgeGraph tkg : tkgList){
            addTechniqueKnowledgeGraph(tkg);
        }
    }

    public void addTechniqueKnowledgeGraph(TechniqueKnowledgeGraph tkg) {
        // 加载
        ArrayList<Object> seedObjects = tkg.getSeedObjects();
        for (Object vertex : seedObjects){
            this.seedNodeSearchMap.put((SeedNode) vertex, tkg);
        }

        ArrayList<Edge> edgeList = tkg.getEdgeList();
        for (Edge edge : edgeList){
            this.seedEdgeSearchMap.put((SeedEdge) edge, tkg);
        }
    }

    public List<TechniqueKnowledgeGraph> search(BasicNode candidateNode) {
        ArrayList<TechniqueKnowledgeGraph> techniqueKnowledgeGraphs = new ArrayList<>();
        for (Map.Entry entry : seedNodeSearchMap.entrySet()){
            if(isVertexAligned((Vertex) entry.getKey(), candidateNode, candidateNode.getProperties())){
                techniqueKnowledgeGraphs.add((TechniqueKnowledgeGraph) entry.getValue());
            }
        }
        return techniqueKnowledgeGraphs;
    }

    public List<TechniqueKnowledgeGraph> search(AssociatedEvent candidateEdge) {
        ArrayList<TechniqueKnowledgeGraph> techniqueKnowledgeGraphs = new ArrayList<>();
        for (Map.Entry entry : seedEdgeSearchMap.entrySet()){
            if(isEdgeAligned((Edge) entry.getKey(), candidateEdge)){
                techniqueKnowledgeGraphs.add((TechniqueKnowledgeGraph) entry.getValue());
            }
        }
        return techniqueKnowledgeGraphs;
    }
}
