package provenancegraph;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.Objects;

public class AssociatedEvent extends BasicEdge implements Serializable {
    public BasicNode sourceNode;
    public BasicNode sinkNode;

    public NodeProperties sourceNodeProperties;
    public NodeProperties sinkNodeProperties;

//    public GenericTagCache sourceNodeTag = null;
//    public GenericTagCache sinkNodeTag = null;

    private String relationship;
    public Long timeStamp;

    private AssociatedEvent generalizedEvent = null;

    public AssociatedEvent(BasicNode sourceNode, BasicNode sinkNode, String relationship, Long timeStamp) {
        this(sourceNode.getProperties(), sinkNode.getProperties(), relationship, timeStamp);
        this.sourceNode = sourceNode;
        this.sinkNode = sinkNode;
    }

    public AssociatedEvent(NodeProperties sourceNodeProperties, NodeProperties sinkNodeProperties, String relationship, Long timeStamp) {
        this.sourceNodeProperties = sourceNodeProperties;
        this.sinkNodeProperties = sinkNodeProperties;
        this.relationship = relationship;
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssociatedEvent associatedEvent = (AssociatedEvent) o;
        return Objects.equals(sourceNodeProperties, associatedEvent.sourceNodeProperties)
                && Objects.equals(sinkNodeProperties, associatedEvent.sinkNodeProperties)
                && Objects.equals(relationship, associatedEvent.relationship)
//                && Objects.equals(timeStamp, fullEvent.timeStamp)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeProperties, sinkNodeProperties, relationship);
    }

    public AssociatedEvent generalize() {
        this.sourceNodeProperties.generalize();
        this.sinkNodeProperties.generalize();
//        this.timeStamp = generalizeTime(this.timeStamp);
        return this;
    }

    public AssociatedEvent copyGeneralize() {
        if(this.generalizedEvent != null) {
            return this;
        }

        AssociatedEvent generalizedEvent = new AssociatedEvent(
                this.sourceNodeProperties.copyGeneralize(),
                this.sinkNodeProperties.copyGeneralize(),
                this.relationship,
                this.timeStamp
        );

        this.generalizedEvent = generalizedEvent;
        return generalizedEvent;
    }

    @Override
    public String toString() {
        return String.format("%s->%s->%s", sourceNodeProperties.toString(), relationship, sinkNodeProperties.toString());
    }

    public String toJsonString() {
        Gson fullEventJson = new Gson();
        String fullEventJsonString = fullEventJson.toJson(this);
        return fullEventJsonString;
    }

    public static Long generalizeTime(Long timeStamp) {
        Long timeWindowsLength = 60 * 1 * 1000000000L;
        return timeStamp / timeWindowsLength * timeWindowsLength;
    }

    public AssociatedEvent ignoreSink() {
        return new AssociatedEvent(this.sourceNodeProperties, new NodeProperties(), this.relationship, this.timeStamp);
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
