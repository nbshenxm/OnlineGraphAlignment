package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphAlignmentMultiTag {
    private Map<String, GraphAlignmentTag> tagMap;

    public GraphAlignmentMultiTag(List<TechniqueKnowledgeGraph> tkgList) {
        // ToDo
        for (TechniqueKnowledgeGraph tkg : tkgList){
            GraphAlignmentTag tag = new GraphAlignmentTag(tkg);
            this.tagMap.put(tkg.techniqueName, tag);
        }
    }

    public GraphAlignmentMultiTag mergeMultiTag(List<TechniqueKnowledgeGraph> tkgList) {
        for (TechniqueKnowledgeGraph tkg : tkgList) {
            if (tagMap.containsKey(tkg.techniqueName)) {

            }
        }
        return;
    }

    public void mergeMultiTag(GraphAlignmentMultiTag newMultiTag) {
        Map<String, GraphAlignmentTag> newMultiTagMap = newMultiTag.getTagMap();
        for (Map.Entry entry : newMultiTagMap.entrySet()){
            this.tagMap.put((String) entry.getKey(), (GraphAlignmentTag) entry.getValue());
        }
    }

    public Map<String, GraphAlignmentTag> getTagMap() {
        return tagMap;
    }
}
