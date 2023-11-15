package libtagpropagation.graphalignment.techniqueknowledgegraph;

import java.util.ArrayList;

public class TechniqueKnowledgeTree {
    private  ArrayList<Integer> tkgTree;

    public TechniqueKnowledgeTree(TechniqueKnowledgeGraph tkg){
        this.tkgTree = new ArrayList<>();

    }

    public int getSonNode(int index){
        return tkgTree.get(index);
    }

    public void setSonNode(int index){
        tkgTree.add(index);
    }
}
