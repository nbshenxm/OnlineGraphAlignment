package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Vertex;
import libtagpropagation.graphalignment.GraphAlignmentMultiTag;
import libtagpropagation.graphalignment.GraphAlignmentProcessFunction;
import libtagpropagation.graphalignment.GraphAlignmentTag;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.BasicNode;

import java.util.ArrayList;

import static libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph.isVertexAligned;

public class AlignmentSearchTree {
    // 用于加速 TechniqueKnowledgeGraph 的搜索速度
    // 输入：查询的起点，即 Tag 中缓存的 lastAlignedNode，以及新到的节点
    // 输出：匹配的分数（0表示没有匹配上），匹配到节点的新位置
    TechniqueKnowledgeGraph tkg;
    public AlignmentSearchTree(TechniqueKnowledgeGraph tkg) {

    }

    public Tuple2<Integer, NodeAlignmentStatus> nodeAlignmentSearch(int lastAlignedNodeIndex, BasicNode currentNode) {
        GraphAlignmentTag tag;
        do{
            ArrayList<Vertex> vertexList = tkg.getVertexList();
            Vertex vertex = vertexList.get(lastAlignedNodeIndex);
            if (isVertexAligned(vertex, currentNode, currentNode.getProperties())){
                NodeAlignmentStatus nodeAlignmentStatus = new NodeAlignmentStatus(tkg.getKeyPropertiesFromType(vertex.getProperty("type")),
                        vertex.getProperty("type"),
                        );
                return Tuple2.of(lastAlignedNodeIndex, nodeAlignmentStatus);
            }
            GraphAlignmentMultiTag tags = GraphAlignmentProcessFunction.tagsCacheMap.get(currentNode.getNodeId());
            tag = tags.getTagMap().get(tkg.techniqueName);
           lastAlignedNodeIndex = tag.getLastAlignedNodeIndex();
        }while(tag != null);
    }
}
