package libtagpropagation.graphalignment.techniqueknowledgegraph;

import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.BasicNode;

public class AlignmentSearchTree {
    // 用于加速 TechniqueKnowledgeGraph 的搜索速度
    // 输入：查询的起点，即 Tag 中缓存的 lastAlignedNode，以及新到的节点
    // 输出：匹配的分数（0表示没有匹配上），匹配到节点的新位置

    public AlignmentSearchTree(TechniqueKnowledgeGraph tkg) {

    }

    public Tuple2<Integer, NodeAlignmentStatus> nodeAlignmentSearch(int lastAlignedNodeIndex, BasicNode currentNode) {

    }
}
