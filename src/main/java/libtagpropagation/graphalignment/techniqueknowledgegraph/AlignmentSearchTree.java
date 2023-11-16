package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import libtagpropagation.graphalignment.alignmentstatus.NodeAlignmentStatus;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.BasicNode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class AlignmentSearchTree {
    // 用于加速 TechniqueKnowledgeGraph 的搜索速度
    // 输入：查询的起点，即 Tag 中缓存的 lastAlignedNode，以及新到的节点
    // 输出：匹配的分数（0表示没有匹配上），匹配到节点的新位置
    private  ArrayList<Tuple2<Vertex, ArrayList<Integer>>> graphSearch;

    public AlignmentSearchTree(TechniqueKnowledgeGraph tkg) {
        // set size of dynamic graphSearch
        this.graphSearch = new ArrayList<>(tkg.getVertexList().size());
        this.graphSearch.addAll(null);

        // init graphSearch via edgeList
        ArrayList<Edge> edgeList = tkg.getEdgeList();
        for (Edge edge : edgeList){
            Vertex src = edge.getVertex(Direction.OUT);
            Vertex dest = edge.getVertex(Direction.IN);
            Integer src_index = (Integer) src.getId();
            Integer dest_index = (Integer) dest.getId();
            if (this.graphSearch.get(src_index) != null){
                Tuple2<Vertex, ArrayList<Integer>> element = this.graphSearch.get(src_index);
                element.f1.add(dest_index);
            }else{
                ArrayList<Integer> nebor = new ArrayList<>();
                nebor.add(dest_index);
                Tuple2<Vertex, ArrayList<Integer>> element = Tuple2.of(src, nebor);
                this.graphSearch.set(src_index, element);
            }
        }
    }

    public Tuple2<Integer, NodeAlignmentStatus> nodeAlignmentSearch(int lastAlignedNodeIndex, BasicNode currentNode) {

        // store the first skip nodes
        Tuple2<Vertex, ArrayList<Integer>> entry = this.graphSearch.get(lastAlignedNodeIndex);
        Queue<Integer> queue = new LinkedList<>();
        int twoSearch = initQueue(queue, entry.f1);

        if (!queue.isEmpty()){
            Integer index = queue.poll();
            // store the second skip nodes
            if (twoSearch -- > 0){
                initQueue(queue, this.graphSearch.get(index).f1);
            }

            // node aligned
            SeedNode seedNode = new SeedNode(this.graphSearch.get(index).f0);
            String alignedString = seedNode.getKeyPropertiesFromType(seedNode.getTkgNode().getProperty("type"));
            if (seedNode.isNodeAligned(currentNode, currentNode.getProperties())) {
                NodeAlignmentStatus nodeAlignmentStatus = new NodeAlignmentStatus(
                        seedNode.getKnowledgeGraphNodeRegex(),
                        seedNode.getTkgNode().getProperty("type"),
                        seedNode.getTkgNode().getProperty(alignedString));
                return Tuple2.of(index, nodeAlignmentStatus);
            }
        }

        return null;
    }

    public int initQueue(Queue<Integer> queue, ArrayList<Integer> indexs){
        int count = 0;
        for (Integer index : indexs){
            queue.offer(index);
            count ++;
        }
        return count;
    }
}
