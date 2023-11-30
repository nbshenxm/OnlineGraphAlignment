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
        Map<String, GraphAlignmentTag> newMultiTagMap = newMultiTag.getTagMap();
        for (Map.Entry entry : newMultiTagMap.entrySet()){
            String key = (String) entry.getKey();
            if (this.tagMap.containsKey(key)) {
                GraphAlignmentTag mergedTag = this.tagMap.get(key).mergeTag((GraphAlignmentTag) entry.getValue());
                if (mergedTag == null) this.tagMap.remove(key);
                else this.tagMap.put(key, mergedTag);
            }
            else
                this.tagMap.put((String) entry.getKey(), (GraphAlignmentTag) entry.getValue());
        }
        return this;
    }

    public GraphAlignmentMultiTag propagate(AssociatedEvent associatedEvent) {
        GraphAlignmentMultiTag newMultiTag = new GraphAlignmentMultiTag(this);
        for (Map.Entry entry : newMultiTag.tagMap.entrySet()) {
            GraphAlignmentTag tag = (GraphAlignmentTag) entry.getValue();
            String techniqueName = (String) entry.getKey();
            GraphAlignmentTag newTag = tag.propagate(associatedEvent);
            if(newTag != null) {
                newMultiTag.tagMap.put(techniqueName, newTag);
            }
            else {
                this.tagMap.remove(techniqueName);
            }
        }

        return newMultiTag;
    }

    public Map<String, GraphAlignmentTag> getTagMap() {
        return tagMap;
    }
}
