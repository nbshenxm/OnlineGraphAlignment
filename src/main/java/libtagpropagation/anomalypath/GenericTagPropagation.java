package libtagpropagation.anomalypath;


import provenancegraph.BasicEdge;
import provenancegraph.BasicNode;

import java.util.ArrayList;

public interface GenericTagPropagation <NODE extends BasicNode, EDGE extends BasicEdge, TAG_CACHE extends GenericTagCache, ALERT> {

    void setTagCache(NODE node, TAG_CACHE tagCache) throws Exception;

    boolean isNodeTagCached(NODE node) throws Exception;

    TAG_CACHE getTagCache(NODE node) throws Exception;

    ArrayList<TAG_CACHE> getTagCaches(NODE node) throws Exception;

    void removeTagCache(NODE node) throws Exception;

    // ToDo: Critical Error
    // List<TAG_CACHE> getTagCache(NODE node) throws Exception;

    default ALERT processEvent(EDGE edge) throws Exception {
        initTag(edge);
        propagateTag(edge);
        return triggerAlert(edge);
    }

    // Tag Initialization
    void initTag(EDGE edge) throws Exception;

    // Tag Propagation Policy - handle an event single- or multiple- times
    void propagateTag(EDGE edge) throws Exception;

    // Tag Degradation and Removal
    // Combating Dependence Explosion in Forensic Analysis Using Alternative Tag Propagation Semantics, S&P'20
    // Embedded in Tag Propagation process
    void degradeTag(EDGE edge) throws Exception;

    // Alert Trigger
    ALERT triggerAlert(EDGE edge) throws Exception;
}
