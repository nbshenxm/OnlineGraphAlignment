package libtagpropagation;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import javafx.util.Pair;
import provenancegraph.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.*;
import java.util.regex.Pattern;

import static libtagpropagation.TechniqueKnowledgeGraph.nodeMatch;

public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    private TechniqueKnowledgeGraph tkg;
    private float TECHNIQUE_ACCEPT_THRESHOLD;
    private float matchScore = 0F;
    private float incMatch = 1F;
    private float decayDegree = 1F;
    private float ttl = 30f;
    private Set<UUID> prevNodes;

    // ToDo: When to free the memory
    private int occupancyCount = 1;

    private Map<String, NodeAlignmentStatus> nodeMatchMap;
    private Map<Pair<String, String>, EdgeAlignmentStatus> edgeMatchMap;

    private Object nodeId;
    private String nodeType;
    private String nodeName;
    private BasicNode node;
    private AssociatedEvent event;
    private Edge e;

    private boolean isNode;

    public GraphAlignmentTag(Vertex seedNode, BasicNode alignedElement, TechniqueKnowledgeGraph tkg) {
        init(tkg);
        prevNodes = new HashSet<UUID>();
        this.isNode = true;
        this.nodeId = seedNode.getId();
        this.node = alignedElement;
        alignNode(seedNode, alignedElement);
        this.TECHNIQUE_ACCEPT_THRESHOLD = ((float) tkg.getEdgeList().size() + tkg.getVertexList().size()) * 0.66F;
    }

    public GraphAlignmentTag(Edge seedEdge, AssociatedEvent alignedElement, TechniqueKnowledgeGraph tkg) {
        init(tkg);
        prevNodes = new HashSet<UUID>();
        this.isNode = false;
        alignEvent(seedEdge, alignedElement);
        this.event = alignedElement;
        this.e = seedEdge;
        this.TECHNIQUE_ACCEPT_THRESHOLD = ((float) tkg.getEdgeList().size() + tkg.getVertexList().size()) * 0.66F;
    }

    public GraphAlignmentTag(Vertex seedNode, BasicNode alignedElement, TechniqueKnowledgeGraph tkg, float matchScore) {
        this.ttl = 100;
        init(tkg);
        this.isNode = true;
        this.nodeId = seedNode.getId();
        alignNode(seedNode, alignedElement);
        this.TECHNIQUE_ACCEPT_THRESHOLD = ((float) tkg.getEdgeList().size() + tkg.getVertexList().size()) * 0.66F;
    }

    public GraphAlignmentTag(Edge seedEdge, AssociatedEvent alignedElement, TechniqueKnowledgeGraph tkg, float ttl) {
        this.ttl = 100;
        init(tkg);
        this.isNode = false;
        alignEvent(seedEdge, alignedElement);
        this.event = alignedElement;
        this.e = seedEdge;
    }

    public GraphAlignmentTag copy(AssociatedEvent newAlignedElement){
        GraphAlignmentTag t;
        if(isNode){
            Vertex thisNode = tkg.tinkerGraph.getVertex(this.nodeId);
            t = new GraphAlignmentTag(thisNode, this.node, this.tkg, this.ttl - decayDegree);

        }
        else{
            t = new GraphAlignmentTag(e, newAlignedElement, this.tkg, this.ttl - decayDegree);
        }
        return t;
    }

    //write a copy constructor
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
        String nodeString = (String) this.nodeId;
        if(nodeMatchMap.containsKey(nodeString)){

            NodeAlignmentStatus nodeAlignmentStatus = nodeMatchMap.get(nodeString);

            nodeAlignmentStatus.align(node);
            if(nodeAlignmentStatus.isMatched() && !(prevNodes.contains(node.getNodeId()))){
                prevNodes.add(node.getNodeId());
                this.matchScore += this.incMatch;
                try{
                    File logger = new File("../caused_match_score.log");
                    FileWriter fileReader = new FileWriter(logger, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileReader);
                    bufferedWriter.append(node.toString());
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    // add matchScore initialization
    private void alignEvent(Edge e, AssociatedEvent event) {
//        if (event.sinkNode.getProperties().toShapeAttribution().equals("rect")){
//            if(((FileNodeProperties) event.sinkNodeProperties).getFilePath().equals("/home/dave/Downloads/database_server")){
//                System.out.println("getting alignment for problem");
//            }
//        }
        Vertex srcNode = e.getVertex(Direction.OUT);
        String srcId = (String) srcNode.getId();
        Vertex destNode = e.getVertex(Direction.IN);
        String destId = (String) destNode.getId();


        Pair<String, String> nodePair = new Pair<>(srcId, destId);
        EdgeAlignmentStatus edgeAlignmentStatus = new EdgeAlignmentStatus(e.getProperty("event_type"));
        if (edgeAlignmentStatus.getMatchedScore() < 1F) {
            edgeAlignmentStatus.align(event);
            if (edgeAlignmentStatus.isMatched()) {
                alignNode(srcNode, event.sourceNode);
                alignNode(destNode, event.sinkNode);
                this.matchScore += this.incMatch;
                try{
                    File logger = new File("../caused_match_score.log");
                    FileWriter fileReader = new FileWriter(logger, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileReader);
                    bufferedWriter.append(event.toString());
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
        //if tkg's
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

    public boolean isNode(){
        return this.isNode;
    }

    // Calculate the alignment score
    public float getMatchScore() {
        return this.matchScore;
    }

    public boolean isMatched() {
        return (getMatchScore() >= TECHNIQUE_ACCEPT_THRESHOLD);
    }


    // work on this
    public float updateMatchScore() {
        // ToDo:
        float newMatchScore = 0.0F;
        return newMatchScore;
    }

    // Expending the search
    public void expandSearch(BasicNode node) {

    }

    public void decay(){
        this.ttl -= 1;
    }
    public float getTTL(){
        return this.ttl;
    }
    public Object getNode(){
        return this.nodeId;
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
