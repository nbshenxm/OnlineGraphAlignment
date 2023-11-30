package libtagpropagation.graphalignment.alignmentstatus;

import provenancegraph.AssociatedEvent;

import java.util.ArrayList;

public class EdgeAlignmentStatus {

    private int pathLength;
//    private NodeAlignmentStatus sinkNodeAlignmentStatus;
    private int nodeAlignmentStatusIndex;
    private float anomlyScore;
    private ArrayList<AssociatedEvent> alignedPath;

    public EdgeAlignmentStatus(ArrayList<AssociatedEvent> cachedPath, int nodeAlignmentStatusIndex) {
        this.alignedPath = cachedPath;
        this.pathLength = cachedPath.size();
        this.nodeAlignmentStatusIndex = nodeAlignmentStatusIndex;
    }

    public int getPathLength() {
        return pathLength;
    }

    public float getAnomlyScore() {
        return anomlyScore;
    }

    public void setAnomlyScore(float anomlyScore) {
        this.anomlyScore = anomlyScore;
    }

    public int getNodeAlignmentStatusIndex(){
        return this.nodeAlignmentStatusIndex;
    }

    @Override
    public String toString() {
        String path = alignedPath.get(alignedPath.size() - 1).toString();
        return String.format("[{%s}]", path);
    }


}