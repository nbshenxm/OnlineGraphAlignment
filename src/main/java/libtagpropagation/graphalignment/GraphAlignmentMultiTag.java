package libtagpropagation.graphalignment;

import java.util.ArrayList;

public class GraphAlignmentMultiTag {
    private ArrayList<GraphAlignmentTag> tagList;
    public GraphAlignmentMultiTag() {
        tagList = new ArrayList<>();
    }

    public void addTag(GraphAlignmentTag tag) {
        tagList.add(tag);
    }

    public ArrayList<GraphAlignmentTag> getTagList() {
        return tagList;
    }

}
