package provenancegraph;

import org.apache.flink.types.Row;

import java.util.Objects;
import java.util.UUID;

public class BasicEdge {
    public UUID edgeId;
    public String edgeType;
    public long timeStamp;

    public UUID sourceNodeId;
    public UUID sinkNodeId;

    public BasicEdge() {}

    public BasicEdge(String edgeType, long timeStamp, UUID sourceNodeId, UUID sinkNodeId) {
        this.edgeId = UUID.randomUUID();
        this.edgeType = edgeType;
        this.timeStamp = timeStamp;
        this.sourceNodeId = sourceNodeId;
        this.sinkNodeId = sinkNodeId;
    }

    public Row getEventRow() {
        Row eventRecord = new Row(5);
        eventRecord.setField(0, edgeId);
        eventRecord.setField(1, edgeType);
        eventRecord.setField(2, timeStamp);
        eventRecord.setField(3, sourceNodeId);
        eventRecord.setField(4, sinkNodeId);

        return eventRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicEdge basicEdge = (BasicEdge) o;
//        return Objects.equals(edgeType, basicEdge.edgeType) && Objects.equals(timeStamp, basicEdge.timeStamp) && Objects.equals(sourceNodeId, basicEdge.sourceNodeId) && Objects.equals(sinkNodeId, basicEdge.sinkNodeId);
        return Objects.equals(edgeType, basicEdge.edgeType) && Objects.equals(sourceNodeId, basicEdge.sourceNodeId) && Objects.equals(sinkNodeId, basicEdge.sinkNodeId);
    }

    @Override
//    public int hashCode() {
//        return Objects.hash(edgeType, timeStamp, sourceNodeId, sinkNodeId);
//    }
    public int hashCode() {
        return Objects.hash(edgeType, sourceNodeId, sinkNodeId);
    }
}
