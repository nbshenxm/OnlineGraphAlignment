package provenancegraph;

import java.io.Serializable;
import java.util.Objects;

enum NodeType {File, Process, Network, Any, Unknown}

public class NodeProperties implements Serializable {
    public NodeType type;

    public NodeProperties() {
        this.type = NodeType.Any;
    }

    public NodeType getType() {
        return this.type;
    }

    public boolean haveSameProperties(NodeProperties that) {
        return type == that.type;
    }

    @Override
    public String toString() {
        return String.format("[%s]", this.type.toString());
    }

    public String toShapeAttribution() {
        String shapeInString;
        switch (type) {
            case Any:
                shapeInString = "circle";
                break;
            case File:
                shapeInString = "rect";
                break;
            case Process:
                shapeInString = "ellipse";
                break;
            case Network:
                shapeInString = "diamond";
                break;
            case Unknown:
            default:
                shapeInString = "";
                break;
        }
        return shapeInString;
    }

    public String toColorAttribution() {
        return "black";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        NodeProperties that = (NodeProperties) o;
        if (this.type.equals(NodeType.Any) || that.type.equals(NodeType.Any)) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return haveSameProperties(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    public void generalize() {}

    public NodeProperties copyGeneralize() {
        return new NodeProperties();
    }
}
