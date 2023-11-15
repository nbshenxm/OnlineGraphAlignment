package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.alignmentstatus.GraphAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import libtagpropagation.graphalignment.techniqueknowledgegraph.AlignmentSearchTree;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.flink.api.java.tuple.Tuple2;
import org.checkerframework.checker.units.qual.A;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    public UUID tagUuid;

    private TechniqueKnowledgeGraph tkg; // 用于匹配
    private AlignmentSearchTree searchTree;
    private int lastAlignedNodeIndex;
    private BasicNode lastAlignedNode; // 用于记录最近匹配到的节点，便于减少匹配数量，最好是一个树中节点的id

    private int cachedPathLength;
    private ArrayList<AssociatedEvent> cachedPath; // 记录最新匹配到的节点后的传播路径
    
    private GraphAlignmentStatus alignStatus; // 用于记录匹配状态，二次索引

    private static final float TECHNIQUE_ACCEPT_THRESHOLD = 0.66F;
    private float matchScore = 0F;

    // ToDo: When to free the memory
    private int occupancyCount = 1;


    public GraphAlignmentTag(TechniqueKnowledgeGraph tkg) {
        this.tagUuid = UUID.randomUUID();

        this.tkg = tkg;
        this.searchTree = new AlignmentSearchTree(tkg);
        this.alignStatus = new GraphAlignmentStatus(tkg);
    }

    private NodeAlignmentStatus alignNode(BasicNode node) {
        // 需要取调用 AlignmentSearchTree
        Tuple2<Integer, NodeAlignmentStatus> alignStatusTuple = this.searchTree.nodeAlignmentSearch(this.lastAlignedNodeIndex, node);
        if (alignStatusTuple == null) return null;
        else {
            if (this.alignStatus.tryUpdateNode(alignStatusTuple.f0, alignStatusTuple.f1) == null) return null; // 和当前已有的匹配情况比较
            else {
                this.lastAlignedNode = node;
                this.lastAlignedNodeIndex = alignStatusTuple.f0;
                // 返回值存入 GraphAlignmentStatus
                return alignStatusTuple.f1;
            }
        }
    }

    public boolean sameAs(GraphAlignmentTag anotherAlignmentTag) {
        return this.tkg.techniqueName.equals(anotherAlignmentTag.tkg.techniqueName);
    }

    public GraphAlignmentTag mergeTag(GraphAlignmentTag anotherAlignmentTag) {
        // ToDo: merge alignment status and 
        this.occupancyCount += anotherAlignmentTag.occupancyCount;

        return null;
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

    public GraphAlignmentTag(GraphAlignmentTag orignalTag){
        this.tagUuid = orignalTag.tagUuid;
        this.tkg = orignalTag.tkg;
        this.searchTree = orignalTag.searchTree;
        this.alignStatus = orignalTag.alignStatus;
    }

    public GraphAlignmentTag propagate(AssociatedEvent event){
        GraphAlignmentTag newTag = new GraphAlignmentTag(this);

        newTag.cachedPathLength = this.cachedPathLength + 1;
        newTag.cachedPath.add(event);

        Tuple2<Integer, NodeAlignmentStatus> searchResult = this.searchTree.nodeAlignmentSearch(lastAlignedNodeIndex, event.sinkNode);
        if (searchResult == null) {

            return null;
        }
        else {
            newTag.lastAlignedNodeIndex = searchResult.f0;
            newTag.lastAlignedNode = event.sinkNode;
            newTag.alignStatus.tryUpdateNode(newTag.lastAlignedNodeIndex, searchResult.f1);
            // ToDo：cached path也需要更新到alignStatus
            newTag.cachedPath = new ArrayList<>();
            newTag.cachedPathLength = 0;
        }

        return newTag;
    }

    public int getLastAlignedNodeIndex() {
        return lastAlignedNodeIndex;
    }
}
