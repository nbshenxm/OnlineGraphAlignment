package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.AssociatedEvent;
import java.util.ArrayList;


public class AlignmentSearchGraph {
    // 用于加速 TechniqueKnowledgeGraph 的搜索速度
    // 输入：查询的起点，即 Tag 中缓存的 lastAlignedNode，以及新到的节点
    // 输出：匹配的分数（0表示没有匹配上），匹配到节点的新位置
    private ArrayList<ArrayList<Edge>> edgeSearch;
    private ArrayList<Edge> edgeList;

    public AlignmentSearchGraph(TechniqueKnowledgeGraph tkg) {
        // set size of dynamic graphSearch
        this.edgeSearch = new ArrayList<>(tkg.getVertexList().size());
        for (int i = 0; i < tkg.getVertexList().size(); i ++ ){
            edgeSearch.add(new ArrayList<Edge>());
        }
        // init nodeSearch via edgeList
        this.edgeList = tkg.getEdgeList();
        for (Edge edge : edgeList){
            Vertex src = edge.getVertex(Direction.OUT);
            Integer src_index = Integer.parseInt(((String) src.getId()).substring(1));
            edgeSearch.get(src_index).add(edge);
        }
    }

    public Tuple3<Integer, Integer, NodeAlignmentStatus> alignmentSearch(int lastAlignedNodeIndex, AssociatedEvent currentEdege) {
        ArrayList<Edge> edges;
        if (lastAlignedNodeIndex < 0){
            edges = edgeList;
        }else{
            edges = this.edgeSearch.get(lastAlignedNodeIndex);
        }

        if (lastAlignedNodeIndex < 0) edges = edgeList;
            for (Edge edge : edges){
            SeedEdge seedEdge = new SeedEdge(edge);
            if (seedEdge.isNextEdgeAligned(currentEdege)){
                NodeAlignmentStatus nodeAlignmentStatus = new NodeAlignmentStatus(
                        seedEdge.getSinkNode().getType(),
                        seedEdge.getSinkNode().getAlignedString());
                return Tuple3.of(seedEdge.getSinkNode().getId(), seedEdge.getId(), nodeAlignmentStatus);
            }
        }
        return null;
    }

}
