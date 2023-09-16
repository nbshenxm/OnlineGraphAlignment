package libtagpropagation;

import java.util.ArrayList;

public class GraphAlignmentTagList {
    private ArrayList<GraphAlignmentTag> tagList;
    public GraphAlignmentTagList() {
        tagList = new ArrayList<>();
    }

    public void addTag(GraphAlignmentTag tag) {
        tagList.add(tag);
    }

    public ArrayList<GraphAlignmentTag> getTagList() {
        return tagList;
    }

}
