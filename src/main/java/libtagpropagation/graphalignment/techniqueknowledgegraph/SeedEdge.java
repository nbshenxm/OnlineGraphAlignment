package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import provenancegraph.AssociatedEvent;

import java.util.regex.Pattern;

public class SeedEdge {
    private Edge seedEdge;
    private int id;
    private String type;
    public SeedEdge(Edge seedEdge) {
        this.seedEdge = seedEdge;
        this.id = seedEdge.getProperty("sequence_num");
        this.type = (String)seedEdge.getProperty("event_type");
    }

    public boolean isEdgeAligned(AssociatedEvent e) {
        String event = e.getRelationship();
        if (Pattern.matches(this.seedEdge.getProperty("event_type"), event)) {
            Vertex src = this.seedEdge.getVertex(Direction.OUT);
            Vertex dest = this.seedEdge.getVertex(Direction.IN);
            SeedNode sourceNode = new SeedNode(src);
            SeedNode sinkNode = new SeedNode(dest);
            if (sourceNode.isNodeAligned(e.sourceNode, e.sourceNodeProperties)){
                return sinkNode.isNodeAligned(e.sinkNode, e.sinkNodeProperties);
            }
        }
        return false;
    }

    public int getId(){
        return this.id;
    }

    @Override
    public String toString() {
        return "SeedEdge{" +
                "id=" + id +
                ", type='" + type + '\'' +
                '}';
    }
}
