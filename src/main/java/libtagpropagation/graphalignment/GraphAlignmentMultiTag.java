package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;
import org.apache.kafka.common.protocol.types.Field;
import provenancegraph.AssociatedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphAlignmentMultiTag {
    private Map<String, GraphAlignmentTag> tagMap;

    public GraphAlignmentMultiTag(Set<TechniqueKnowledgeGraph> tkgList) {
        tagMap = new HashMap<>();

        for (TechniqueKnowledgeGraph tkg : tkgList){
            GraphAlignmentTag tag = new GraphAlignmentTag(tkg);
            this.tagMap.put(tkg.techniqueName, tag);
        }
    }

    public GraphAlignmentMultiTag(GraphAlignmentMultiTag originalTag) {
        this.tagMap = new HashMap<>(originalTag.tagMap);
    }

    public GraphAlignmentMultiTag mergeMultiTag(GraphAlignmentMultiTag newMultiTag) {
        Map<String, GraphAlignmentTag> newMultiTagMap = newMultiTag.getTagMap();
        for (Map.Entry entry : newMultiTagMap.entrySet()){
            if (this.tagMap.containsKey(entry.getKey())) {
                GraphAlignmentTag mergedTag = this.tagMap.get(entry.getKey()).mergeTag((GraphAlignmentTag) entry.getValue());
                this.tagMap.put((String) entry.getKey(), mergedTag);
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
                newMultiTag.tagMap.put(techniqueName, newTag);
        }

        return newMultiTag;
    }

    public Map<String, GraphAlignmentTag> getTagMap() {
        return tagMap;
    }
}
