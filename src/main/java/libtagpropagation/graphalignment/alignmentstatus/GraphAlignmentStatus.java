package libtagpropagation.graphalignment.alignmentstatus;

import com.google.common.collect.Iterators;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import provenancegraph.AssociatedEvent;

import javax.xml.soap.Node;
import java.util.ArrayList;
import java.util.Arrays;

public class GraphAlignmentStatus {
    // 需要从 GraphAlignmentTag 中剥离出来以实现二次索引
    // 用于记录匹配结果，并计算匹配的分数，在不同 Tag 之间共用
    // 用 TechniqueKnowledgeGraph 初始化
    // 输入匹配上的结果，该结果来自于 TechniqueKnowledge 和 ALignmentSearchTree 的输出
    // 输出匹配的最新情况

    public Float alignmentScore = 0.0F;
    public final Float alignmentThresholds = 0.7F;

    private int nodeCount;
    private int edgeCount;

    private NodeAlignmentStatus[] nodeAlignmentStatusList;
    private EdgeAlignmentStatus[] edgeAlignmentStatusList;

    public GraphAlignmentStatus(TechniqueKnowledgeGraph tkg) {
        nodeCount = Iterators.size(tkg.tinkerGraph.getVertices().iterator());
        nodeAlignmentStatusList = new NodeAlignmentStatus[nodeCount];
        Arrays.fill(nodeAlignmentStatusList, null);

        edgeCount = Iterators.size(tkg.tinkerGraph.getEdges().iterator());
        edgeAlignmentStatusList = new EdgeAlignmentStatus[edgeCount];
        Arrays.fill(edgeAlignmentStatusList, null);
    }

    public GraphAlignmentStatus tryUpdateStatus(int nodeIndex, int edgeIndex, NodeAlignmentStatus newNodeAlignmentStatus, ArrayList<AssociatedEvent> cachedPath) {
        if (nodeIndex >= nodeCount || edgeIndex >= edgeCount) throw new RuntimeException("This node or edge seems not in the TKG.");

        // ToDo：考虑边的匹配状态
        if (edgeAlignmentStatusList[edgeIndex] == null){
            this.nodeAlignmentStatusList[nodeIndex] = newNodeAlignmentStatus;//Fixme: 节点未必为空
            this.edgeAlignmentStatusList[edgeIndex] = new EdgeAlignmentStatus(cachedPath);
            this.alignmentScore += newNodeAlignmentStatus.getAlignmentScore() * (1 / cachedPath.size() + 1) / this.edgeCount;
        }
        else{
            Float newEdgeAlignmentScore = newNodeAlignmentStatus.getAlignmentScore() / cachedPath.size();
            Float originalAlignmentScore = this.nodeAlignmentStatusList[nodeIndex].getAlignmentScore() / this.edgeAlignmentStatusList[edgeIndex].getPathLength();
            if (newEdgeAlignmentScore > originalAlignmentScore){
                this.edgeAlignmentStatusList[edgeIndex] = new EdgeAlignmentStatus(cachedPath);
                this.nodeAlignmentStatusList[nodeIndex] = newNodeAlignmentStatus;
            }
            else return null;
        }

        return this;
    }

    public NodeAlignmentStatus[] getNodeAlignmentStatusList() {
        return nodeAlignmentStatusList;
    }

    public EdgeAlignmentStatus[] getEdgeAlignmentStatusList() {
        return edgeAlignmentStatusList;
    }

    public boolean shouldTriggerAlert(){
        return alignmentScore >= alignmentThresholds;
    }

    public String getAlignmentResult() {
        // 格式化的输出对齐的结果，作为告警信息
        StringBuilder alignmentResult = new StringBuilder();

        for (NodeAlignmentStatus nodeAligned : nodeAlignmentStatusList) {
            if (nodeAligned == null) continue;
            else alignmentResult.append(nodeAligned.toString()).append("\n");
        }

        for (EdgeAlignmentStatus edgeAligned : edgeAlignmentStatusList) {
            if (edgeAligned == null) continue;
            else alignmentResult.append(edgeAligned.toString()).append("\n");
        }

        return alignmentResult.toString();
    }
}
