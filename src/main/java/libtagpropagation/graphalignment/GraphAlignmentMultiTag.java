package libtagpropagation.graphalignment;

import libtagpropagation.graphalignment.techniqueknowledgegraph.TechniqueKnowledgeGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphAlignmentMultiTag {
    private Map<String, GraphAlignmentTag> tagMap;

    public GraphAlignmentMultiTag(List<TechniqueKnowledgeGraph> tkgList) {
        // ToDo
    }

    public GraphAlignmentMultiTag mergeMultiTag(List<TechniqueKnowledgeGraph> tkgList) {
        for (TechniqueKnowledgeGraph tkg : tkgList) {
            if (tagMap.containsKey(tkg.techniqueName)) {

            }
        }
    }

    public GraphAlignmentMultiTag mergeMultiTag(GraphAlignmentMultiTag newMultiTag) {

    }
}
