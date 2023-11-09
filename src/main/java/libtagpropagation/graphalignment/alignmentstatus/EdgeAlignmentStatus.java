package libtagpropagation.graphalignment.alignmentstatus;

import provenancegraph.AssociatedEvent;

import java.util.ArrayList;
import java.util.UUID;

public class EdgeAlignmentStatus {
    private final String type;

    private float alignmentScore;
    private int distance;
    private ArrayList<AssociatedEvent> alignedPath;

    public EdgeAlignmentStatus(String type) {
        this.type = type;
        this.alignmentScore = 0.0F;
        this.distance = 0;
    }

    public void align(AssociatedEvent event) {
        if (isAligned(event)) {
            alignmentScore = 1F;
        }
    }

    public boolean isAligned(AssociatedEvent event) {
        return this.type.equals(event.getRelationship());
    }

    public float getAlignmentScore() {
        return alignmentScore;
    }

    public boolean isABetterAlign(EdgeAlignmentStatus anotherStatus) {
        return this.getAlignmentScore() > anotherStatus.getAlignmentScore();
    }


    public void incDist() {
        distance++;
    }
}