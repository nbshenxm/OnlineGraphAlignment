package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.alignmentstatus.EdgeAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.GraphAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import libtagpropagation.graphalignment.techniqueknowledgegraph.AlignmentSearchGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.UUID;

public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    public UUID tagUuid;

    private TechniqueKnowledgeGraph tkg; // 用于匹配
    private AlignmentSearchGraph searchTree;
    private int lastAlignedNodeIndex;
    private BasicNode lastAlignedNode; // 用于记录最近匹配到的节点，便于减少匹配数量，最好是一个树中节点的id

    private int cachedPathLength;
    private ArrayList<AssociatedEvent> cachedPath; // 记录最新匹配到的节点后的传播路径
    
    private GraphAlignmentStatus alignStatus; // 用于记录匹配状态，二次索引

    private static final int ATTENUATION_THRESHOLD = 6;

    // ToDo: When to free the memory
    private int occupancyCount = 1;


    public GraphAlignmentTag(Tuple2<Integer, TechniqueKnowledgeGraph> entry) {
        this.tagUuid = UUID.randomUUID();
        this.tkg = entry.f1;
        this.cachedPath = new ArrayList<>();
        this.searchTree = new AlignmentSearchGraph(entry.f1);
        this.alignStatus = new GraphAlignmentStatus(entry.f1);
        this.lastAlignedNodeIndex = entry.f0;
    }

    public GraphAlignmentTag mergeTag(GraphAlignmentTag anotherAlignmentTag) {
        // ToDo: merge alignment status
        // update nodeAlignmentStatus
        NodeAlignmentStatus[] anotherNodeAlignmentStatusList = anotherAlignmentTag.alignStatus.getNodeAlignmentStatusList();
        NodeAlignmentStatus[] nodeAlignmentStatusList = this.alignStatus.getNodeAlignmentStatusList();
        for (int i = 0; i < anotherNodeAlignmentStatusList.length; i ++){
            if (anotherNodeAlignmentStatusList[i] != null && nodeAlignmentStatusList[i] == null)
                nodeAlignmentStatusList[i] = anotherNodeAlignmentStatusList[i];
        }

        // update edgeAlignmentStatus
        EdgeAlignmentStatus[] anotherEdgeAlignmentStatusList = anotherAlignmentTag.alignStatus.getEdgeAlignmentStatusList();
        EdgeAlignmentStatus[] edgeAlignmentStatusList = this.alignStatus.getEdgeAlignmentStatusList();
        for (int i = 0; i < anotherEdgeAlignmentStatusList.length; i ++){
            if(anotherEdgeAlignmentStatusList[i] != null && edgeAlignmentStatusList[i] == null){
                edgeAlignmentStatusList[i] = anotherEdgeAlignmentStatusList[i];
            }
        }
        return this;
    }

    public GraphAlignmentTag(GraphAlignmentTag orignalTag){
        this.tagUuid = UUID.randomUUID();
        this.tkg = orignalTag.tkg;
        this.searchTree = orignalTag.searchTree;
        this.alignStatus = orignalTag.alignStatus;
    }

    public GraphAlignmentTag propagate(AssociatedEvent event){
        GraphAlignmentTag newTag = new GraphAlignmentTag(this);
        newTag.cachedPath = new ArrayList<>(this.cachedPath);

        newTag.cachedPath.add(event);
        newTag.cachedPathLength = this.cachedPathLength + 1;

        Tuple3<Integer, Integer, NodeAlignmentStatus> searchResult = this.searchTree.alignmentSearch(lastAlignedNodeIndex, event);
        if (searchResult == null) {
            if (this.cachedPath.size() > ATTENUATION_THRESHOLD)return null;
            else{
                newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                newTag.lastAlignedNode = this.lastAlignedNode;
            }

        }
        else {
            newTag.lastAlignedNodeIndex = searchResult.f0;
            newTag.lastAlignedNode = event.sinkNode;

            GraphAlignmentStatus graphAlignmentStatus = newTag.alignStatus.tryUpdateStatus(searchResult.f0, searchResult.f1, searchResult.f2, cachedPath);
            if(graphAlignmentStatus == null){
                if (this.cachedPath.size() > ATTENUATION_THRESHOLD)return null;
                else{
                    newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                    newTag.lastAlignedNode = this.lastAlignedNode;
                }
            }
            if (this.alignStatus.shouldTriggerAlert()){
                System.out.println(this.alignStatus.getAlignmentResult());
                return null;
            }
            // ToDo：cached path也需要更新到alignStatus
            newTag.cachedPath = new ArrayList<>();
            newTag.cachedPathLength = 0;
        }

        return newTag;
    }
}
