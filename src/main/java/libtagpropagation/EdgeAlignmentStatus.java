package libtagpropagation;

import provenancegraph.AssociatedEvent;

import java.util.UUID;

public class EdgeAlignmentStatus {
    private final String type;
    private UUID sourceNodeId;
    private UUID sinkNodeId;

    // private string edgeType;

    private boolean matched;
    private float matchedScore;
    private int distance;

    public EdgeAlignmentStatus(String type) {
        this.type = type;
        this.matched = false;
        this.matchedScore = 0.0F;
        this.distance = 0;
    }

    public void align(AssociatedEvent event) {
        if (isAligned(event)) {
            matched = true;
            matchedScore = 1F;
        }
    }

    public boolean isAligned(AssociatedEvent event) {
        return this.type.equals(event.getRelationship());
    }

    public float getMatchedScore() {
        return matchedScore;
    }

    public boolean isMatched() {
        return matched;
    }

    public void incDist() {
        distance++;
    }
}