package libtagpropagation.graphalignment.alignmentstatus;

import com.google.common.collect.Iterators;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;

import javax.xml.soap.Node;
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

    public NodeAlignmentStatus tryUpdateNode(int index, NodeAlignmentStatus newStatus) {
        if (index >= nodeCount) throw new RuntimeException("This node seems not in the TKG.");

        // 考虑节点匹配状态
        if (nodeAlignmentStatusList[index] == null || newStatus.isABetterAlign(nodeAlignmentStatusList[index])) {
            nodeAlignmentStatusList[index] = newStatus;
            // ToDo: Update alignment score
            return newStatus;
        }

        // ToDo：考虑边的匹配状态

        return null;
    }

    public String tryUpdateEdge(int index, EdgeAlignmentStatus newStatus) {
        if (index >= edgeCount) throw new RuntimeException("This edge seems not in the TKG.");

        if (edgeAlignmentStatusList[index] == null || newStatus.isABetterAlign(edgeAlignmentStatusList[index])) {
            edgeAlignmentStatusList[index] = newStatus;
            // ToDo: Update alignment score
            return "Updated.";
        }

        return "Not accepted.";
    }

    public Float getAlignmentScore() {
        // ToDo: 设计对齐分数的计算过程，目标是能够及时的更新
        return alignmentScore;
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
