package provenancegraph;

import org.apache.flink.types.Row;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class BasicNode implements Serializable {
    private UUID nodeId;
    private NodeType nodeType;
    private String nodeName;
    private NodeProperties properties;


    public BasicNode(UUID nodeId, String nodeType, String nodeName) {
        this.setNodeId(nodeId);
        this.setNodeType(nodeType);
        this.setNodeName(nodeName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicNode basicNode = (BasicNode) o;
        return Objects.equals(nodeId, basicNode.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeId);
    }

    public Row getNodeRow() {
        Row nodeRecord = new Row(3);
        nodeRecord.setField(0, nodeId);
        nodeRecord.setField(1, nodeType);
        nodeRecord.setField(2, nodeName);

        return nodeRecord;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeType() {
        return nodeType.toString();
    }

    public void setNodeType(String nodeType) {
        switch (nodeType) {
            case "File":
                this.nodeType = NodeType.File;
                break;
            case "Process":
                this.nodeType = NodeType.Process;
                break;
            case "Network":
                this.nodeType = NodeType.Network;
                break;
            default:
                this.nodeType = NodeType.Unknown;
        }
    }

    public void setProperties(NodeProperties properties) {
        this.properties = properties;
    }

    public NodeProperties getProperties() {
        return this.properties;
    }
}
