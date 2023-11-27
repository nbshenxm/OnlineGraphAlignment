package libtagpropagation.graphalignment.alignmentstatus;

public class NodeAlignmentStatus {

    private String type;
    private String alignedString;
    private float alignmentScore;

    public NodeAlignmentStatus(String type, String alignedString) {
        this.type = type;
        this.alignedString = alignedString;
        this.alignmentScore = 1.0f;
    }

    public float getAlignmentScore() {
        return alignmentScore;
    }

    @Override
    public String toString() {
        return String.format("[{%s}, {%s}]", this.alignedString, this.type);
    }

}