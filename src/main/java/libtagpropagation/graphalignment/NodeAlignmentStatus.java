package libtagpropagation.graphalignment;

import provenancegraph.BasicNode;

import java.util.regex.Pattern;

public class NodeAlignmentStatus {
    private final String type;
    private final String kpKGNodeRegex;
    private String kpPGNodeString;
    private boolean matched;
    private float matchedScore;

    public NodeAlignmentStatus(String type, String kpKGNodeRegex) {
        this.type = type;
        this.kpKGNodeRegex = kpKGNodeRegex;
        this.kpPGNodeString = "";
        this.matched = false;
        this.matchedScore = 0.0F;
    }

    public void align(BasicNode node) {
        if (isAligned(node)) {
            matched = true;
            matchedScore = 1F;
            kpPGNodeString = node.getNodeName();
        }
    }

    public boolean isAligned(BasicNode node) {
        if (this.type.equals(node.getNodeType())) { // ToDo: check these two kind of node type
            return Pattern.matches(this.kpKGNodeRegex, node.getNodeName());
        }
        return false;
    }

    public float getMatchedScore() {
        return matchedScore;
    }

    public boolean isMatched() {
        return matched;
    }
}