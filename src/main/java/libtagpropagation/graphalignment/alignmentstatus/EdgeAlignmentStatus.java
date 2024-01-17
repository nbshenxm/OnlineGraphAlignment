package libtagpropagation.graphalignment.alignmentstatus;

import provenancegraph.AssociatedEvent;

import java.util.ArrayList;

public class EdgeAlignmentStatus {

//    private NodeAlignmentStatus sinkNodeAlignmentStatus;
    private int sinkNodeStatusIndex;
    private int sourceNodeStatusIndex;
    private float anomlyScore;
    private ArrayList<AssociatedEvent> alignedPath;

    public EdgeAlignmentStatus(ArrayList<AssociatedEvent> cachedPath, int sinkNodeStatusIndex, int sourceNodeStatusIndex) {
        this.alignedPath = cachedPath;
        this.sinkNodeStatusIndex = sinkNodeStatusIndex;
        this.sourceNodeStatusIndex = sourceNodeStatusIndex;
    }

    public float getAnomlyScore() {
        return anomlyScore;
    }

    public void setAnomlyScore(float anomlyScore) {
        this.anomlyScore = anomlyScore;
    }

    public int getSinkNodeStatusIndex(){
        return this.sinkNodeStatusIndex;
    }
    public int getSourceNodeStatusIndex() {return this.sourceNodeStatusIndex;}

    @Override
    public String toString() {
//        String path = alignedPath.get(alignedPath.size() - 1).toString();
        StringBuilder path = new StringBuilder();
        for (AssociatedEvent associatedEvent : alignedPath){
            path.append(associatedEvent.toString()).append("(ts:" + associatedEvent.timeStamp +") ");
        }
        return String.format("[{%s}, {cachedPath:%d}]", path, alignedPath.size());
    }


}