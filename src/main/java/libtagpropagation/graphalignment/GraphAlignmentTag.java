package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.alignmentstatus.GraphAlignmentStatus;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import libtagpropagation.graphalignment.techniqueknowledgegraph.AlignmentSearchGraph;
import libtagpropagation.graphalignment.techniqueknowledgegraph.SeedNode;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.*;

import java.util.ArrayList;
import java.util.UUID;

import static libtagpropagation.graphalignment.GraphAlignmentProcessFunction.*;

public class GraphAlignmentTag {

    public UUID tagUuid;

    private int seedNodeID;
    private String seedNodeType;
    private TechniqueKnowledgeGraph tkg; // 用于匹配
    private AlignmentSearchGraph searchGraph;
    private int lastAlignedNodeIndex;
    private BasicNode lastAlignedNode; // 用于记录最近匹配到的节点，便于减少匹配数量，最好是一个树中节点的id

    private int cachedPathLength;
    private ArrayList<AssociatedEvent> cachedPath; // 记录最新匹配到的节点后的传播路径
    
    private GraphAlignmentStatus alignStatus; // 用于记录匹配状态，二次索引

    private boolean isOnTKG;// identify if the tag is on tkg
    private static final int ATTENUATION_THRESHOLD = 6;

    public GraphAlignmentTag(SeedNode seedNode, TechniqueKnowledgeGraph tkg, UUID tagUUID) {
        this.tagUuid = tagUUID;
        this.seedNodeID = seedNode.getId();
        this.seedNodeType = seedNode.getType();
        this.tkg = tkg;
        this.cachedPath = new ArrayList<>();
        this.searchGraph = new AlignmentSearchGraph(seedNode.getAlignedString(), tkg);
        //增加匹配上的信息
        this.alignStatus = new GraphAlignmentStatus(seedNode, tkg);
        this.lastAlignedNodeIndex = seedNode.getId();
        this.isOnTKG = true;
        initTagCount ++;
    }

    public GraphAlignmentTag mergeTag(GraphAlignmentTag anotherAlignmentTag) {
        if (this.seedNodeID == anotherAlignmentTag.seedNodeID){
            if (!this.tagUuid.equals(anotherAlignmentTag.tagUuid)){
                return anotherAlignmentTag;
            }
        }

        this.alignStatus.mergeAlignmentStatus(anotherAlignmentTag.alignStatus.getEdgeAlignmentStatusList(),anotherAlignmentTag.alignStatus.getNodeAlignmentStatusList());

//        System.out.println("merge:" + this.lastAlignedNodeIndex + " " + anotherAlignmentTag.lastAlignedNodeIndex );
//        this.alignStatus.print();

        if (this.alignStatus.shouldTriggerAlert()){
            if(!this.alignStatus.recurringAlert())
                System.out.println(this.alignStatus.getAlignmentResult());
            return null;
        }

        if(this.isOnTKG || anotherAlignmentTag.isOnTKG){
            if (this.cachedPath.size() == 0 || anotherAlignmentTag.cachedPath.size() == 0){
                if (anotherAlignmentTag.cachedPath.size() == 0)
                    this.lastAlignedNodeIndex = anotherAlignmentTag.lastAlignedNodeIndex;
                this.cachedPath = new ArrayList<>();
            }
            else return null;
        }
        else{
            if (this.lastAlignedNodeIndex != anotherAlignmentTag.lastAlignedNodeIndex) {
                this.lastAlignedNodeIndex = -1;   //TODO:bug of assigned cachedPath
            }
            //TODO: time attribution
            if (this.cachedPath.size() > anotherAlignmentTag.cachedPath.size())
                this.cachedPath = anotherAlignmentTag.cachedPath;
        }
        return this;
    }

    public GraphAlignmentTag(GraphAlignmentTag orignalTag){
        this.tagUuid = orignalTag.tagUuid;
        this.seedNodeID = orignalTag.seedNodeID;
        this.seedNodeType = orignalTag.seedNodeType;
        this.tkg = orignalTag.tkg;
        this.searchGraph = orignalTag.searchGraph;
        this.alignStatus = orignalTag.alignStatus;
        propagateTagCount ++;
    }

    public GraphAlignmentTag propagate(AssociatedEvent event){
        GraphAlignmentTag newTag = new GraphAlignmentTag(this);
        newTag.cachedPath = new ArrayList<>(this.cachedPath);
        newTag.cachedPath.add(event);
        newTag.cachedPathLength = this.cachedPathLength + 1;

        Tuple3<NodeAlignmentStatus, NodeAlignmentStatus, Integer> searchResult = this.searchGraph.alignmentSearch(lastAlignedNodeIndex, event);
        if (searchResult == null) {
            if (this.cachedPath.size() > ATTENUATION_THRESHOLD) return null;
            else{
                newTag.lastAlignedNodeIndex = this.lastAlignedNodeIndex;
                newTag.lastAlignedNode = this.lastAlignedNode;
                newTag.isOnTKG = false;
            }

        }
        else {
            newTag.lastAlignedNodeIndex = searchResult.f1.getIndex();
            newTag.lastAlignedNode = event.sinkNode;
            newTag.isOnTKG = true;
            GraphAlignmentStatus graphAlignmentStatus = newTag.alignStatus.tryUpdateStatus(searchResult.f0, searchResult.f1, searchResult.f2, newTag.cachedPath);
            if(graphAlignmentStatus == null) {
                if (this.cachedPath.size() > ATTENUATION_THRESHOLD) return null;
            }
            if (this.alignStatus.shouldTriggerAlert()){
                if(!this.alignStatus.recurringAlert())
                    System.out.println(this.alignStatus.getAlignmentResult());
                return null;
            }
//            if(graphAlignmentStatus != null) {
//                System.out.println("updateStatus:");
//                this.alignStatus.print();
//            }
            // ToDo：cached path也需要更新到alignStatus
            newTag.cachedPath = new ArrayList<>();
            newTag.cachedPathLength = 0;
        }

        return newTag;
    }

    public boolean recurringAlert(){
        return this.alignStatus.recurringAlert();
    }

}
