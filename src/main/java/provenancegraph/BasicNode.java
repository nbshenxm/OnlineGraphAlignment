package provenancegraph;

import org.apache.flink.types.Row;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class BasicNode implements Serializable {
    private UUID nodeUUID;
    private NodeType nodeType;
    private String nodeName;
    private NodeProperties properties;


    public BasicNode(UUID nodeId, String nodeType, String nodeName) {
        this.setNodeUUID(nodeId);
        this.setNodeType(nodeType);
        this.setNodeName(nodeName);
    }
    public BasicNode(){
        this.setNodeUUID(new UUID(0, 0));
        this.setNodeType("");
        this.setNodeName("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicNode basicNode = (BasicNode) o;
        return Objects.equals(nodeUUID, basicNode.nodeUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeUUID);
    }

    public Row getNodeRow() {
        Row nodeRecord = new Row(3);
        nodeRecord.setField(0, nodeUUID);
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

    public UUID getNodeUUID() {
        return nodeUUID;
    }

    public void setNodeUUID(UUID nodeUUID) {
        this.nodeUUID = nodeUUID;
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

    public String toString(){
        return "UUID: " + this.nodeUUID.toString() + ", Node Type: " + this.nodeType.toString() + ", Node Name: " + this.nodeName;
    }
}
