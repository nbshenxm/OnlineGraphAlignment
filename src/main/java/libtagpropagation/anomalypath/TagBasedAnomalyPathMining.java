package libtagpropagation.anomalypath;


import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;

import java.util.UUID;

import static libtagpropagation.anomalypath.TagBasedAnomalyPathMiningOnFlink.initTagRegularScoreThreshold;

public interface TagBasedAnomalyPathMining extends GenericTagPropagation <BasicNode, AssociatedEvent, AnomalyScoreTagCache, AnomalyScoreTagCache> {
    
//    static Double INIT_TAG_REGULAR_SCORE_THRESHOLD = 0.1;
    
    BasicNode getNodeInfo(UUID uuid) throws Exception;

    Double getEventRegularScore(AssociatedEvent associatedEvent) throws Exception;

    @Override
    default void initTag(AssociatedEvent associatedEvent) throws Exception {
        // FixMe: return if sourceNode have anomaly tag, it is unnecessary to init a new tag

        if (associatedEvent.sourceNodeTag != null && ((AnomalyScoreTagCache) associatedEvent.sourceNodeTag).getRegularScore() <= 1) return;

        AssociatedEvent generalizedEvent = associatedEvent.copyGeneralize();
        double eventRegularScore = getEventRegularScore(generalizedEvent);
//        eventRegularScore = eventRegularScore * 0.001;
        if (eventRegularScore <= initTagRegularScoreThreshold){
            AnomalyScoreTagCache newTag = new AnomalyScoreTagCache(associatedEvent, eventRegularScore);
            if (associatedEvent.sinkNodeTag == null || ((AnomalyScoreTagCache)associatedEvent.sinkNodeTag).shouldReplaceTag(newTag)) {
                setTagCache(associatedEvent.sinkNode, newTag);
            }
        }
        else return;
    }

    @Override
    default void propagateTag(AssociatedEvent associatedEvent) throws Exception {
        AssociatedEvent generalizedEvent = associatedEvent.copyGeneralize();
        double eventRegularScore = getEventRegularScore(generalizedEvent);
//        // Debug Output
//        Double sourceTagScore = 0.0;
//        if (!isNodeTagCached(sourceNode)) ;
//        else sourceTagScore = getTagCache(sourceNode).getRegularScore();
//        String debugOutput = parseTimeStamp(edge.timeStamp) + " - " + fullEvent.toString()
//                + " - EventRegularScore: " + eventRegularScore
//                + " - SourceNodeTagRegularScore: " + sourceTagScore;
//        System.out.println(debugOutput);

        if (associatedEvent.sourceNodeTag == null) return;
        else
        {
            AnomalyScoreTagCache sourceTag = (AnomalyScoreTagCache) associatedEvent.sourceNodeTag;
            if (sourceTag.shouldDecayed(associatedEvent.timeStamp)) {
                removeTagCache(associatedEvent.sourceNode);
                AnomalyScoreTagCache.decayedTagCount += 1;
                return;
            }

            // ToDo: Is it necessary to avoid loop?
            //  - 1/10 FP
            if (associatedEvent.sourceNode == associatedEvent.sinkNode
                    || sourceTag.LastSourceNode.copyGeneralize().equals(generalizedEvent.sinkNodeProperties)) {
                return;
            }

            AnomalyScoreTagCache newTag = sourceTag.propagate(associatedEvent, eventRegularScore);

            if (associatedEvent.sinkNodeTag == null || ((AnomalyScoreTagCache)associatedEvent.sinkNodeTag).shouldReplaceTag(newTag)) {
                setTagCache(associatedEvent.sinkNode, newTag);
            }
        }
    }

    @Override
    default void degradeTag(AssociatedEvent associatedEvent) throws Exception {
        if(associatedEvent.sinkNodeTag == null) return;

        AnomalyScoreTagCache sinkTag = (AnomalyScoreTagCache) associatedEvent.sinkNodeTag;
        if(sinkTag.shouldAttenuated()) {
            removeTagCache(associatedEvent.sinkNode);
            AnomalyScoreTagCache.attenuatedTagCount += 1;
        }
    }

    @Override
    default AnomalyScoreTagCache triggerAlert(AssociatedEvent associatedEvent) throws Exception {
        if(associatedEvent.sinkNodeTag == null) return null;

        AnomalyScoreTagCache sinkTag = (AnomalyScoreTagCache) associatedEvent.sinkNodeTag;
        if (sinkTag.shouldTriggerAlert()) {
            // FixMe: Avoid repeated alert
            //  - 1/100 FP
            //  - An trade-off here is that either you can choose longer attack chain with better semantic or
            //  - shorter attack chain with higher TP
            removeTagCache(associatedEvent.sinkNode);
            return sinkTag.triggerAlert();
        }
        else
            return null;
    }
}
