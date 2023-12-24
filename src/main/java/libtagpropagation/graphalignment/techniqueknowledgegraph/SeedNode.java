package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Vertex;
import provenancegraph.*;

import java.util.HashMap;
import java.util.regex.Pattern;

public class SeedNode{
    private String matchedString;
    private String alignedString;
    private String type;
    private int id;

    private final HashMap<String, String> KEY_PROPERTIES_MAP = new HashMap <String, String> (){{
        put("Network", "url_ip");
        put("File", "file_path");
        put("Process", "process_name");
    }};

    public SeedNode(Vertex tkgNode) {
        this.type = tkgNode.getProperty("type");
        this.matchedString = tkgNode.getProperty(getKeyPropertiesFromType(this.type));
        this.id = Integer.parseInt(((String) tkgNode.getId()).substring(1));
    }

    public String getKeyPropertiesFromType(String type) {
        return this.KEY_PROPERTIES_MAP.getOrDefault(type, null);
    }

    public boolean isNodeAligned(BasicNode n, NodeProperties np) {
        String alignedString = null;
        String type = null;
        switch (n.getNodeType()) {
            case "File":
                type = "File";
                alignedString = ((FileNodeProperties) np).getFilePath();
                break;
            case "Process":
                type = "Process";
                alignedString = ((ProcessNodeProperties) np).getProcessName();
                break;
            case "Network":
                type = "Network";
                alignedString = ((NetworkNodeProperties) np).getRemoteIp();
                break;
            default:
                break;
        }

        if (this.type.equals(type)){
            if(Pattern.matches(this.matchedString, alignedString)){
                this.alignedString = alignedString;
                return true;
            }
        }
        return false;
    }

    public String getAlignedString() {
        return this.alignedString;
    }

    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.type + ": "+ this.matchedString;
    }
}
