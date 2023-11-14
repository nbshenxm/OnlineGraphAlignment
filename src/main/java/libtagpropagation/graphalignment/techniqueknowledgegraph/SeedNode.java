package libtagpropagation.graphalignment.techniqueknowledgegraph;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import provenancegraph.*;

import java.util.HashMap;
import java.util.regex.Pattern;

public class SeedNode{
    private Vertex kgNode;

    private final HashMap<String, String> KEY_PROPERTIES_MAP = new HashMap <String, String> (){{
        put("Network", "url_ip");
        put("File", "file_path");
        put("Process", "process_name");
    }};

    public SeedNode(Vertex kgNode) {
        this.kgNode = kgNode;
    }

    public String getKeyPropertiesFromType(String type) {
        return this.KEY_PROPERTIES_MAP.getOrDefault(type, null);
    }

    public boolean nodeMatch(Vertex pgNode) {
        String kgNodeType = this.kgNode.getProperty("type");
        String pgNodeType = pgNode.getProperty("type");
        if (!kgNodeType.equals(pgNodeType)) {
            return false;
        }
        String kpKGNode = kgNode.getProperty(getKeyPropertiesFromType(kgNode.getProperty("type")));
        String kpPGNode = pgNode.getProperty(getKeyPropertiesFromType(pgNode.getProperty("type")));
        return Pattern.matches(kpKGNode, kpPGNode);
    }

    public boolean isVertexAligned(BasicNode n, NodeProperties np) {
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
}
