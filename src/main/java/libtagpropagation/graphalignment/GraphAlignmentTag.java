package libtagpropagation.graphalignment;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import javafx.util.Pair;
import libtagpropagation.graphalignment.alignmentstatus.EdgeAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.GraphAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    private TechniqueKnowledgeGraph tkg; // 用于匹配
    private BasicNode lastAlignedNode; // 用于记录最近匹配到的节点，便于减少匹配数量，最好是一个树中节点的id
    private GraphAlignmentStatus alignStatus; // 用于记录匹配状态，二次索引

    private static final float TECHNIQUE_ACCEPT_THRESHOLD = 0.66F;
    private float matchScore = 0F;

    // ToDo: When to free the memory
    private int occupancyCount = 1;

    private Map<String, NodeAlignmentStatus> nodeMatchMap;
    private Map<Pair<String, String>, EdgeAlignmentStatus> edgeMatchMap;

    public GraphAlignmentTag(Vertex seedNode, BasicNode alignedElement, TechniqueKnowledgeGraph tkg) {
        init(tkg);
        alignNode(seedNode, alignedElement);
    }

    public GraphAlignmentTag(Edge seedEdge, AssociatedEvent alignedElement, TechniqueKnowledgeGraph tkg) {
        init(tkg);
        alignEvent(seedEdge, alignedElement);
    }

    private void init(TechniqueKnowledgeGraph tkg){
        this.tkg = tkg;
        nodeMatchMap = new HashMap<>();
        edgeMatchMap = new HashMap<>();
        for (Vertex vertex : tkg.tinkerGraph.getVertices()) {
            // TODO change getProperty("id") to getId()
            nodeMatchMap.put((String)vertex.getId(), new NodeAlignmentStatus(
                    vertex.getProperty("type"),
                    vertex.getProperty(TechniqueKnowledgeGraph.getKeyPropertiesFromType(vertex.getProperty("type")))));

            for (Edge edge : vertex.getEdges(Direction.IN)) {
                Vertex src = edge.getVertex(Direction.OUT);
                String srcId = (String) src.getId();
                String destId = (String) vertex.getId();
//                String srcId = src.getProperty("id");
//                String destId = vertex.getProperty("id");
                Pair<String, String> nodePair = new Pair<>(srcId, destId);
                edgeMatchMap.put(nodePair, new EdgeAlignmentStatus( // TODO: change to source and target
                        edge.getProperty("type")));
            }
        }
    }

    private void alignNode(Vertex v, BasicNode node) {
//        String nodeId = v.getProperty("id");
        String nodeId = (String) v.getId();
        NodeAlignmentStatus nodeAlignmentStatus = nodeMatchMap.get(nodeId);
        nodeAlignmentStatus.align(node);
    }

    private void alignEvent(Edge e, AssociatedEvent event) {
        Vertex srcNode = e.getVertex(Direction.OUT);
        String srcId = (String) srcNode.getId();
        Vertex destNode = e.getVertex(Direction.IN);
        String destId = (String) destNode.getId();


        Pair<String, String> nodePair = new Pair<>(srcId, destId);
        EdgeAlignmentStatus edgeAlignmentStatus = edgeMatchMap.get(nodePair);
        if (edgeAlignmentStatus.getMatchedScore() < 1F) {
            edgeAlignmentStatus.align(event);
            if (edgeAlignmentStatus.isMatched()) {
                alignNode(srcNode, event.sourceNode);
                alignNode(destNode, event.sinkNode);
            }
        }
    }

    public void propagate(AssociatedEvent event) {
        // tag align
        ArrayList<Edge> edge_list = tkg.getEdgeList();
        for (Edge e : edge_list) {
            alignEvent(e, event);
        }
    }



    public boolean sameAs(GraphAlignmentTag anotherAlignmentTag) {
        return this.tkg.techniqueName.equals(anotherAlignmentTag.tkg.techniqueName);
    }

    public GraphAlignmentTag mergeStatus(GraphAlignmentTag anotherAlignmentTag) {
        // ToDo: merge alignment status and 
        this.occupancyCount += anotherAlignmentTag.occupancyCount;

        return null;
    }

    // Record tag binding this status.
    public boolean isStatusFree() {
        if (occupancyCount == 0)
            return true;
        else
            return false;
    }

    public int bindTheStatus() {
        this.occupancyCount += 1;
        return this.occupancyCount;
    }

    public int looseTheStatus() {
        this.occupancyCount -= 1;
        return this.occupancyCount;
    }

    // Calculate the alignment score
    public float getMatchScore() {
        return this.matchScore;
    }

    public boolean isMatched() {
        return (getMatchScore() >= TECHNIQUE_ACCEPT_THRESHOLD);
    }

    public float updateMatchScore() {
        // ToDo:
        float newMatchScore = 0.0F;
        return newMatchScore;
    }

    // Expending the search
    public void expandSearch(BasicNode node) {

    }

    public String getNodeId(BasicNode node) {
        for (Map.Entry<String, NodeAlignmentStatus> entryIter : nodeMatchMap.entrySet()) {
            if (entryIter.getValue().isAligned(node)) {
                return entryIter.getKey();
            }
        }
        return null;
    }
}
