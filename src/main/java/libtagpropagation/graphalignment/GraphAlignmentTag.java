package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.alignmentstatus.GraphAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import libtagpropagation.graphalignment.techniqueknowledgegraph.AlignmentSearchGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.UUID;


public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    public UUID tagUuid;

    private TechniqueKnowledgeGraph tkg; // 用于匹配
    private AlignmentSearchGraph searchGraph;
    private int lastAlignedNodeIndex;
    private BasicNode lastAlignedNode; // 用于记录最近匹配到的节点，便于减少匹配数量，最好是一个树中节点的id

    private int cachedPathLength; // 在性能要求比较高的时候可以用来取代cachedPath
    private ArrayList<AssociatedEvent> cachedPath; // 记录最新匹配到的节点后的传播路径
    
    private GraphAlignmentStatus alignStatus; // 用于记录匹配状态，二次索引

    private static final int ATTENUATION_THRESHOLD = 6;

    public GraphAlignmentTag(TechniqueKnowledgeGraph tkg) {
        this.tagUuid = UUID.randomUUID();

        this.tkg = tkg;
        this.searchGraph = new AlignmentSearchGraph(tkg);
        this.alignStatus = new GraphAlignmentStatus(tkg);
        this.lastAlignedNodeIndex = -1;
        this.lastAlignedNode = null;
    }

    public boolean sameAs(GraphAlignmentTag anotherAlignmentTag) {
        return this.tkg.techniqueName.equals(anotherAlignmentTag.tkg.techniqueName);
    }

    public GraphAlignmentTag mergeTag(GraphAlignmentTag anotherAlignmentTag) {
        // ToDo: merge alignment status

        for ()
        this.cachedPathLength += anotherAlignmentTag.cachedPathLength;
        this.lastAlignedNodeIndex = anotherAlignmentTag.lastAlignedNodeIndex;
        this.lastAlignedNode = anotherAlignmentTag.lastAlignedNode;

        return null;
    }

    public GraphAlignmentTag(GraphAlignmentTag orignalTag){
        this.tagUuid = UUID.randomUUID();
        this.tkg = orignalTag.tkg;
        this.searchGraph = orignalTag.searchGraph;
        this.alignStatus = orignalTag.alignStatus;
    }

    public GraphAlignmentTag propagate(AssociatedEvent event){

        GraphAlignmentTag newTag = new GraphAlignmentTag(this);

        newTag.cachedPath = new ArrayList<>(this.cachedPath); // ToDo：有优化空间，可以复用同一个List，记录起点和终点
        newTag.cachedPath.add(event);
        newTag.cachedPathLength = this.cachedPathLength + 1;

        //node align
        Tuple3<Integer, Integer, NodeAlignmentStatus> searchResult = this.searchGraph.nodeAlignmentSearch(lastAlignedNodeIndex, event.sinkNode);
        if (searchResult == null) {
            if (this.cachedPath.size() > ATTENUATION_THRESHOLD) return null;
            else {
                newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                newTag.lastAlignedNode = this.lastAlignedNode;
            }
        }
        else {
            GraphAlignmentStatus graphAlignmentStatus = newTag.alignStatus.tryUpdateStatus(searchResult.f0, searchResult.f1, searchResult.f2, cachedPath);
            if (graphAlignmentStatus == null) {
                if (this.cachedPath.size() > ATTENUATION_THRESHOLD) return null;
                else {
                    newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                    newTag.lastAlignedNode = this.lastAlignedNode;
                }
            }
            else {
                newTag.lastAlignedNodeIndex = searchResult.f0;
                newTag.lastAlignedNode = event.sinkNode;

                if (this.alignStatus.shouldTriggerAlert()) {
                    System.out.println(this.alignStatus.getAlignmentResult());
                    return null;
                }

                newTag.cachedPath = new ArrayList<>();
                newTag.cachedPathLength = 0;
            }
        }

        return newTag;
    }

}
