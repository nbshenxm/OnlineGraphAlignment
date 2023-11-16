package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import provenancegraph.*;

import java.util.HashMap;
import java.util.regex.Pattern;

public class SeedNode{
    private Vertex tkgNode;
    private String knowledgeGraphNodeRegex;

    private final HashMap<String, String> KEY_PROPERTIES_MAP = new HashMap <String, String> (){{
        put("Network", "url_ip");
        put("File", "file_path");
        put("Process", "process_name");
    }};

    public SeedNode(Vertex tkgNode) {
        this.tkgNode = tkgNode;
        this.knowledgeGraphNodeRegex = getKeyPropertiesFromType(tkgNode.getProperty("type"));
    }

    public String getKeyPropertiesFromType(String type) {
        return this.KEY_PROPERTIES_MAP.getOrDefault(type, null);
    }

    public boolean nodeMatch(Vertex pgNode) {
        String kgNodeType = this.tkgNode.getProperty("type");
        String pgNodeType = pgNode.getProperty("type");
        if (!kgNodeType.equals(pgNodeType)) {
            return false;
        }
        String kpKGNode = tkgNode.getProperty(getKeyPropertiesFromType(tkgNode.getProperty("type")));
        String kpPGNode = pgNode.getProperty(getKeyPropertiesFromType(pgNode.getProperty("type")));
        return Pattern.matches(kpKGNode, kpPGNode);
    }

    public boolean isNodeAligned(BasicNode n, NodeProperties np) {
        TinkerGraph graph = new TinkerGraph();
        Vertex temp_node = graph.addVertex("1");
        switch (n.getNodeType()) {
            case "File":
                temp_node.setProperty("type", "File");
                temp_node.setProperty("file_path", ((FileNodeProperties) np).getFilePath());
                break;
            case "Process":
                temp_node.setProperty("type", "Process");
                temp_node.setProperty("process_name", ((ProcessNodeProperties) np).getProcessName());
                break;
            case "Network":
                temp_node.setProperty("type", "Network");
                temp_node.setProperty("url_ip", ((NetworkNodeProperties) np).getRemoteIp());
                break;
            default:
                break;
        }
        return nodeMatch(temp_node);
    }

    public Vertex getTkgNode() {
        return tkgNode;
    }

    public String getKnowledgeGraphNodeRegex() {
        return knowledgeGraphNodeRegex;
    }
}
