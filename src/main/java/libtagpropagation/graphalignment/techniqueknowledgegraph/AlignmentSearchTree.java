package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Vertex;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.BasicNode;
import java.util.ArrayList;


public class AlignmentSearchTree {
    // 用于加速 TechniqueKnowledgeGraph 的搜索速度
    // 输入：查询的起点，即 Tag 中缓存的 lastAlignedNode，以及新到的节点
    // 输出：匹配的分数（0表示没有匹配上），匹配到节点的新位置
    private ArrayList<Vertex> vertexList;
    private  TechniqueKnowledgeTree tkgTree;

    public AlignmentSearchTree(TechniqueKnowledgeGraph tkg) {
        this.vertexList = tkg.getVertexList();
        tkgTree = new TechniqueKnowledgeTree(tkg);
    }

    public Tuple2<Integer, NodeAlignmentStatus> nodeAlignmentSearch(int lastAlignedNodeIndex, BasicNode currentNode) {

        do {
            SeedNode seedNode = new SeedNode(vertexList.get(lastAlignedNodeIndex));
            if (seedNode.isVertexAligned(currentNode, currentNode.getProperties())) {
                // tkgTree dynamic or static generation
                NodeAlignmentStatus nodeAlignmentStatus = new NodeAlignmentStatus(seedNode.getKnowledgeGraphNodeRegex(),
                        seedNode.getTkgNode().getProperty("type"),
                        seedNode.getTkgNode().getProperty("type"));//alignedString
                return Tuple2.of(lastAlignedNodeIndex, nodeAlignmentStatus);
            }
            lastAlignedNodeIndex = tkgTree.getSonNode(lastAlignedNodeIndex);
        }while(lastAlignedNodeIndex >= 0);

        return null;
    }
}
