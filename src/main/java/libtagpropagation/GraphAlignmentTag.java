package libtagpropagation;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import javafx.util.Pair;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static libtagpropagation.TechniqueKnowledgeGraph.nodeMatch;

public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    private TechniqueKnowledgeGraph tkg;
    private static final float TECHNIQUE_ACCEPT_THRESHOLD = 0.66F;
    private float matchScore = 0F;
    private float decayDegree = 1F;

    // ToDo: When to free the memory
    private int occupancyCount = 1;

    private Map<String, NodeAlignmentStatus> nodeMatchMap;
    private Map<Pair<String, String>, EdgeAlignmentStatus> edgeMatchMap;

    public GraphAlignmentTag(Vertex seedNode, BasicNode alignedElement, TechniqueKnowledgeGraph tkg) {
        init(tkg);
        System.out.println("Made it");
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
            nodeMatchMap.put(vertex.getProperty("id"), new NodeAlignmentStatus(
                    vertex.getProperty("type"),
                    vertex.getProperty(TechniqueKnowledgeGraph.getKeyPropertiesFromType(vertex.getProperty("type")))));

            for (Edge edge : vertex.getEdges(Direction.IN)) {
                Vertex src = edge.getVertex(Direction.OUT);
                String srcId = src.getId().toString();
                String destId = vertex.getId().toString();
                Pair<String, String> nodePair = new Pair<>(srcId, destId);
                edgeMatchMap.put(nodePair, new EdgeAlignmentStatus( // TODO: change to source and target
                        // CURRENT: THIS IS BUGGING OUT
                        edge.getProperty("event_type")));

            }
        }
    }

    private void alignNode(Vertex v, BasicNode node) {
        String nodeId = v.getProperty("id");
        NodeAlignmentStatus nodeAlignmentStatus = nodeMatchMap.get(nodeId);
        nodeAlignmentStatus.align(node);
        System.out.println(v);
        for (String s : v.getPropertyKeys()){
            System.out.println(s + ": " + v.getProperty(s));
        }
    }

    private void alignEvent(Edge e, AssociatedEvent event) {
        Vertex srcNode = e.getVertex(Direction.OUT);
        String srcId = srcNode.getId().toString();
        Vertex destNode = e.getVertex(Direction.IN);
        String destId = destNode.getId().toString();
        Pair<String, String> nodePair = new Pair<>(srcId, destId);
        EdgeAlignmentStatus edgeAlignmentStatus = edgeMatchMap.get(nodePair);
        if (edgeAlignmentStatus.getMatchedScore() < 1F) {
            edgeAlignmentStatus.align(event);
            if (edgeAlignmentStatus.isMatched()) {
                alignNode(srcNode, event.sourceNode);
                alignNode(destNode, event.sinkNode);
            }
        }
        for (String s : e.getPropertyKeys()){
            System.out.println(s + ": " + e.getProperty(s));
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
