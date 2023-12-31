package libtagpropagation.graphalignment.alignmentstatus;

public class NodeAlignmentStatus {

    private String type;
    private String alignedString;
    private float alignmentScore;
    private Integer index;

    public NodeAlignmentStatus(String type, String alignedString, Integer index) {
        this.type = type;
        this.alignedString = alignedString;
        this.alignmentScore = 1.0f;
        this.index = index;
    }

    public float getAlignmentScore() {
        return alignmentScore;
    }

    public Integer getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return String.format("[{%s}, {%s}]", this.alignedString, this.type);
    }

}