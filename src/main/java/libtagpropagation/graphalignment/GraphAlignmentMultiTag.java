package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.techniqueknowledgegraph.SeedNode;
import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.flink.api.java.tuple.Tuple2;
import provenancegraph.AssociatedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphAlignmentMultiTag {
    private Map<String, GraphAlignmentTag> tagMap;

    public GraphAlignmentMultiTag(Set<Tuple2<SeedNode, TechniqueKnowledgeGraph>> tkgList) {
        tagMap = new HashMap<>();

        for (Tuple2<SeedNode, TechniqueKnowledgeGraph> entry : tkgList){
            GraphAlignmentTag tag = new GraphAlignmentTag(entry);
            this.tagMap.put(entry.f1.techniqueName, tag);
        }
    }

    public GraphAlignmentMultiTag(GraphAlignmentMultiTag originalTag) {
        this.tagMap = new HashMap<>(originalTag.tagMap);
    }

    public GraphAlignmentMultiTag mergeMultiTag(GraphAlignmentMultiTag newMultiTag) {
        System.out.println("-----------current info of MultiTag-------------");
        for (Map.Entry entry : this.tagMap.entrySet()){
            System.out.println((String) entry.getKey() + ":  " + ((GraphAlignmentTag)entry.getValue()).tagUuid);
        }
        System.out.println();
        Map<String, GraphAlignmentTag> newMultiTagMap = newMultiTag.getTagMap();
        for (Map.Entry entry : newMultiTagMap.entrySet()){
            String key = (String) entry.getKey();
            if (this.tagMap.containsKey(key)) {
                GraphAlignmentTag mergedTag = this.tagMap.get(key).mergeTag((GraphAlignmentTag) entry.getValue());
                this.tagMap.put((String) entry.getKey(), mergedTag);
            }
            else
                this.tagMap.put((String) entry.getKey(), (GraphAlignmentTag) entry.getValue());
        }

        System.out.println("-----------current info of MultiTag-------------\n");
        return this;
    }

    public GraphAlignmentMultiTag propagate(AssociatedEvent associatedEvent) {
        int count = 0;
        GraphAlignmentMultiTag newMultiTag = new GraphAlignmentMultiTag(this);
        for (Map.Entry entry : newMultiTag.tagMap.entrySet()) {
            count ++;
            GraphAlignmentTag tag = (GraphAlignmentTag) entry.getValue();
            String techniqueName = (String) entry.getKey();
            System.out.println("\ntag:" + count);
            GraphAlignmentTag newTag = tag.propagate(associatedEvent);
            if(newTag != null) {
                newMultiTag.tagMap.put(techniqueName, newTag);
            }
        }

        return newMultiTag;
    }

    public Map<String, GraphAlignmentTag> getTagMap() {
        return tagMap;
    }
}
