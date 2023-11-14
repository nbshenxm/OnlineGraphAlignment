package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import provenancegraph.AssociatedEvent;

import java.util.regex.Pattern;

public class SeedEdge {
    private Edge seedEdge;

    public SeedEdge(Edge seedEdge) {
        this.seedEdge = seedEdge;
    }

    public boolean isEdgeAligned(AssociatedEvent e) {
        String event = e.getRelationship();
        if (Pattern.matches(this.seedEdge.getProperty("event_type"), event)) {
            Vertex src = this.seedEdge.getVertex(Direction.OUT);
            Vertex dest = this.seedEdge.getVertex(Direction.IN);
            SeedNode sourceNode = new SeedNode(src);
            SeedNode sinkNode = new SeedNode(dest);
            if (sourceNode.isVertexAligned(e.sourceNode, e.sourceNodeProperties)){
                return sinkNode.isVertexAligned(e.sinkNode, e.sinkNodeProperties);
            }
        }
        return false;
    }
}
