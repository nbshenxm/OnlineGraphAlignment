package provenancegraph;

import com.google.gson.Gson;
import anomalypath.AnomalyScoreTagCache;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// TODO: discuss what information we need: 1) if we still need variables in BasicEdge; 2) anything we need to add
public class AssociatedEvent extends BasicEdge implements Serializable {
    public AnomalyScoreTagCache sourceNodeTag;
    public AnomalyScoreTagCache sinkNodeTag;
    public BasicNode sourceNode;
    public BasicNode sinkNode;

    public NodeProperties sourceNodeProperties;
    public NodeProperties sinkNodeProperties;

    private String relationship;
    private UUID eventUUID;
    public  Long timeStamp;

    public UUID hostUUID;

    private AssociatedEvent generalizedEvent = null;
    public AssociatedEvent(){
        this.sourceNode = new BasicNode();
        this.sinkNode = new BasicNode();
        this.sourceNodeProperties = new FileNodeProperties("");
        this.sinkNodeProperties = new FileNodeProperties("");
        this.relationship = "";
        this.timeStamp = Long.valueOf(0);
        this.hostUUID = new UUID(0, 0);

    }
    public AssociatedEvent(UUID hostUUID, String relationship, Long timeStamp, UUID eventUUID){
        this.hostUUID = hostUUID;
        this.relationship = relationship;
        this.timeStamp = timeStamp;
        this.eventUUID = eventUUID;
    }

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
        return fullEventJson.toJson(this);
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

    public UUID getEventUUID(){return this.eventUUID;}

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public void setSourceNode(BasicNode sourceNode) {
        this.sourceNode = sourceNode;
        this.sourceNodeProperties = sourceNode.getProperties();
    }

    public void setSinkNode(BasicNode sinkNode) {
        this.sinkNode = sinkNode;
        this.sinkNodeProperties = sinkNode.getProperties();
    }

    public void setTimeStamp(Long timeStamp){ this.timeStamp = timeStamp; }

    public void setHostUUID(UUID hostUUID){ this.hostUUID = hostUUID; };

    public void setEventUUID(UUID eventUUID){this.eventUUID = eventUUID; }
}
