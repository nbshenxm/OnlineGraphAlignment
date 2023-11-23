package libtagpropagation.graphalignment.alignmentstatus;

import org.apache.kafka.common.protocol.types.Field;
import provenancegraph.AssociatedEvent;

import java.util.ArrayList;

public class EdgeAlignmentStatus {

    private int pathLength;
    private ArrayList<AssociatedEvent> alignedPath;

    public EdgeAlignmentStatus(ArrayList<AssociatedEvent> cachedPath) {
        this.alignedPath = cachedPath;
        this.pathLength = cachedPath.size();
    }

    public int getPathLength() {
        return pathLength;
    }

    @Override
    public String toString() {
        String path = alignedPath.get(alignedPath.size() - 1).toString();
        return String.format("[{%s}]", path);
    }

}