package libtagpropagation.graphalignment.alignmentstatus;

import provenancegraph.BasicNode;

import java.util.regex.Pattern;

public class NodeAlignmentStatus {
    private String knowledgeGraphNodeRegex;

    private String type;
    private String alignedString;
    private float alignmentScore;

    public NodeAlignmentStatus(String knowledgeGraphNodeRegex, String type, String alignedString) {
        this.type = type;
        this.alignedString = alignedString;
        this.knowledgeGraphNodeRegex = knowledgeGraphNodeRegex;
    }

    public float getAlignmentScore() {
        return alignmentScore;
    }

    public boolean isABetterAlign(NodeAlignmentStatus anotherStatus) {
        return this.getAlignmentScore() > anotherStatus.getAlignmentScore();
    }

    @Override
    public String toString() {
        return String.format("[{}, {}]", this.alignedString, this.alignmentScore);
    }
}