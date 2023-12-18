package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.alignmentstatus.GraphAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import libtagpropagation.graphalignment.techniqueknowledgegraph.AlignmentSearchGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.SeedNode;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.UUID;

import static libtagpropagation.graphalignment.GraphAlignmentProcessFunction.*;

public class GraphAlignmentTag {

    // TODO: discuss what information we need: 1) rules to search for candidate nodes/edges for further propagation;
    //                                          2) matched nodes/edge to calculate alignment score;

    public UUID tagUuid;

    private TechniqueKnowledgeGraph tkg; // 用于匹配
    private AlignmentSearchGraph searchGraph;
    private int lastAlignedNodeIndex;
    private BasicNode lastAlignedNode; // 用于记录最近匹配到的节点，便于减少匹配数量，最好是一个树中节点的id

    private int cachedPathLength;
    private ArrayList<AssociatedEvent> cachedPath; // 记录最新匹配到的节点后的传播路径
    
    private GraphAlignmentStatus alignStatus; // 用于记录匹配状态，二次索引

    private boolean IsOnTKG;// identify if the tag is on tkg
    private static final int ATTENUATION_THRESHOLD = 6;

    public GraphAlignmentTag(Tuple2<SeedNode, TechniqueKnowledgeGraph> entry) {
        this.tagUuid = UUID.randomUUID();
        this.tkg = entry.f1;
        this.cachedPath = new ArrayList<>();
        this.searchGraph = new AlignmentSearchGraph(entry.f0.getAlignedString(), entry.f1);
        //增加匹配上的信息
        this.alignStatus = new GraphAlignmentStatus(entry);
        this.lastAlignedNodeIndex = entry.f0.getId();
        this.IsOnTKG = true;
        initTagCount ++;
    }

    public GraphAlignmentTag mergeTag(GraphAlignmentTag anotherAlignmentTag) {
        if (this.alignStatus.recurringAlert()){
            return null;
        }

        // update mergeAlignmentStatus
        this.alignStatus.mergeAlignmentStatus(anotherAlignmentTag.alignStatus.getEdgeAlignmentStatusList(),anotherAlignmentTag.alignStatus.getNodeAlignmentStatusList());

//        System.out.println("merge:" + this.lastAlignedNodeIndex + " " + anotherAlignmentTag.lastAlignedNodeIndex );
//        this.alignStatus.print();

        if (this.alignStatus.shouldTriggerAlert()){
            System.out.println(this.alignStatus.getAlignmentResult());
            return null;
        }
        if(anotherAlignmentTag.IsOnTKG){
            this.lastAlignedNodeIndex = anotherAlignmentTag.lastAlignedNodeIndex;
        }
        else{
            if (!this.IsOnTKG) {
                this.lastAlignedNodeIndex = -1;
            }
        }

        return this;
    }

    public GraphAlignmentTag(GraphAlignmentTag orignalTag){
        this.tagUuid = UUID.randomUUID();
        this.tkg = orignalTag.tkg;
        this.searchGraph = orignalTag.searchGraph;
        this.alignStatus = orignalTag.alignStatus;
        propagateTagCount ++;
    }

    public GraphAlignmentTag propagate(AssociatedEvent event){
        GraphAlignmentTag newTag = new GraphAlignmentTag(this);

        if (this.alignStatus.recurringAlert()){
            return null;
        }
        newTag.cachedPath = new ArrayList<>(this.cachedPath);
        newTag.cachedPath.add(event);
        newTag.cachedPathLength = this.cachedPathLength + 1;

        Tuple3<Integer, Integer, NodeAlignmentStatus> searchResult = this.searchGraph.alignmentSearch(lastAlignedNodeIndex, event);
        if (searchResult == null) {
            if (this.cachedPath.size() > ATTENUATION_THRESHOLD) return null;
            else{
                newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                newTag.lastAlignedNode = this.lastAlignedNode;
                newTag.IsOnTKG = false;
            }

        }
        else {
            newTag.lastAlignedNodeIndex = searchResult.f0;
            newTag.lastAlignedNode = event.sinkNode;
            newTag.IsOnTKG = true;
            GraphAlignmentStatus graphAlignmentStatus = newTag.alignStatus.tryUpdateStatus(searchResult.f0, searchResult.f1, searchResult.f2, newTag.cachedPath);
            if(graphAlignmentStatus == null) {
                if (this.cachedPath.size() > ATTENUATION_THRESHOLD) return null;
                else{
                    newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                    newTag.lastAlignedNode = this.lastAlignedNode;
                }
            }
            if (this.alignStatus.shouldTriggerAlert()){
                System.out.println(this.alignStatus.getAlignmentResult());
                return null;
            }
//            System.out.println("updateStatus:");
//            this.alignStatus.print();
            // ToDo：cached path也需要更新到alignStatus
            newTag.cachedPath = new ArrayList<>();
            newTag.cachedPathLength = 0;
        }

        return newTag;
    }

}
