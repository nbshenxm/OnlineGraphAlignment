package libtagpropagation;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Only consider TKG matching, not TKG generation.**/
/*
class TKGNode {
    private int nodeId;
    private UDMProto.LogType nodeType; // Process, File, Network for Linux
    private String tkgRegexRepresentation;

    TKGNode(UDMProto.LogType nodeType, String tkgRegexRepresentation) {
        this.nodeType = nodeType;
        this.tkgRegexRepresentation = tkgRegexRepresentation;
    }

    public boolean nodeMatching (String nodeRepresentation) {
        boolean isMatch = Pattern.matches(this.tkgRegexRepresentation, nodeRepresentation);
        return isMatch;
    }

    public float fuzzyNodeMatching(String nodeRepresentation){
        // ToDo:
        return 0.0F;
    }
}
*/

/**
 * The TechniqueKnowledge and Matching Status */
public class TechniqueKnowledgeGraph {
    // https://github.com/tinkerpop/blueprints/wiki/GraphML-Reader-and-Writer-Library
    public TinkerGraph tinkerGraph;
    public String techniqueName;

    private static final HashMap<String, String> KEY_PROPERTIES_MAP = new HashMap <String, String> (){{
        put("network", "url_ip");
        put("file", "file_path");
        put("process", "process_name");
    }};

    public TechniqueKnowledgeGraph(String gmlFilePath) throws IOException {
        this.loadFromGraphMLFile(gmlFilePath);
        String fileNameWithExtension = new File(gmlFilePath.trim()).getName();
        this.techniqueName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));
    }

    public void loadFromGraphMLFile(String gmlFilePath) throws IOException {
        /**
         * @Description Init TKG with GraphML file */
        if (gmlFilePath == null) {
            throw new FileNotFoundException();
        }

        this.tinkerGraph = new TinkerGraph();
        GraphMLReader reader = new GraphMLReader(this.tinkerGraph);

        InputStream is = new BufferedInputStream(new FileInputStream(gmlFilePath));
        reader.inputGraph(is);
    }

    public String printBasicInfo() {
        return this.tinkerGraph.toString();
    }

    public ArrayList<Object> getSeedObjects() {
        ArrayList<Object> seedObjects = new ArrayList<>();

        for (Vertex v: this.tinkerGraph.getVertices()) {
            try {
                if (v.getProperty("is_seed_node")) {
                    seedObjects.add(v);
                }
            }
            catch (NullPointerException e1) {
            }
        }

        for (Edge e: this.tinkerGraph.getEdges()) {
            try {
                if (e.getProperty("is_seed_edge")) {
                    seedObjects.add(e);
                }
            }
            catch (NullPointerException e2) {
            }
        }

        return seedObjects;
    }

    public static String getKeyPropertiesFromType(String type) {
        return KEY_PROPERTIES_MAP.getOrDefault(type, null);
    }

    public static boolean nodeMatch(Vertex kgNode, Vertex pgNode) {
        if (kgNode.getProperty("type") != pgNode.getProperty("type")) {
            return false;
        }
        String kpKGNode = kgNode.getProperty(getKeyPropertiesFromType(kgNode.getProperty("type")));
        String kpPGNode = pgNode.getProperty(getKeyPropertiesFromType(pgNode.getProperty("type")));
        return Pattern.matches(kpKGNode, kpPGNode);
    }

    public static float fuzzyNodeMatch(Vertex source, Vertex target){
        // ToDo:
        return 0.0F;
    }
}
