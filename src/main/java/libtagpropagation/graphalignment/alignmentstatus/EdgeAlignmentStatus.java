package libtagpropagation.graphalignment.alignmentstatus;

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
}