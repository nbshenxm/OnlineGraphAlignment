package libtagpropagation.anomalypath;


import libtagpropagation.alert.AlertFormatter;
import libtagpropagation.alert.MergedAlertGraph;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;
import provenancegraph.NodeProperties;
import provenancegraph.datamodel.PDM;

import java.io.*;
import java.util.*;

import static provenancegraph.parser.PDMParser.*;

public class TagBasedAnomalyPathMiningOnFlink extends KeyedProcessFunction<PDM.HostUUID, PDM.Log, String> implements TagBasedAnomalyPathMining {

    public static Double initTagRegularScoreThreshold = 0.1;
    public static Double unseenEventScore = 0.1;
    public static Double seenEventMinimumScore = 0.3;

    private transient MapState<UUID, AnomalyScoreTagCache> tagsCacheMap;
    private transient MapState<UUID, BasicNode> nodeInfoMap;

    private static Map<AssociatedEvent, Double> eventRelativeFrequencyMap;

    public static final String ALERT_FILE_NAME = "alerts_output.txt";
    public static Integer alertCount = 0;
    public static Long eventCount = 0L;
    public static Long lostEventCount = 0L;
    public static Long pipeIOCount = 0L;
    public static HashSet<String> debugOutputSet = new HashSet<>();
    public static String outputTarget = "stdout";

    private transient ValueState<Long> processedEventCountValue;

    private transient MapState<NodeProperties, MergedAlertGraph> mergedAlertGraphMap;

    private static Set<UUID> hostUuidSet = new HashSet<>();
    private static Set<UUID> firstNHostUuidSet = new HashSet<>();
    private transient ValueState<UUID> hostUuid;
    private transient ValueState<String> hostIp;

    private static Long detectionStartTime = System.currentTimeMillis();
    private static Long currentTime = System.currentTimeMillis();

    private transient ValueState<Long> hostEventStartTime;
    private transient ValueState<Long> hostEventLatestTime;


    @Override
    public void open(Configuration parameter) throws Exception {

        StateTtlConfig ttlConfig = StateTtlConfig
                .newBuilder(Time.minutes(10))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
        MapStateDescriptor<UUID, AnomalyScoreTagCache> tagsCacheMapDescriptor
                = new MapStateDescriptor<>("tagsCacheMap", UUID.class, AnomalyScoreTagCache.class);
        tagsCacheMapDescriptor.enableTimeToLive(ttlConfig);
        // map --> uuid, anomalysocre
        this.tagsCacheMap = getRuntimeContext().getMapState(tagsCacheMapDescriptor);

        MapStateDescriptor<UUID, BasicNode> nodeInfoStateDescriptor = new MapStateDescriptor<>("nodeInfoMap", UUID.class, BasicNode.class);
        // map --> nod, BasicNode
        this.nodeInfoMap = getRuntimeContext().getMapState(nodeInfoStateDescriptor);

        ValueStateDescriptor<Long> processedEventCountValueDescriptor
                = new ValueStateDescriptor<>("processedEventCountValue", Long.class, 0L);
        //count process events
        this.processedEventCountValue = getRuntimeContext().getState(processedEventCountValueDescriptor);

        MapStateDescriptor<NodeProperties, MergedAlertGraph> mergedAlertGraphMapDescriptor
                = new MapStateDescriptor<>("mergedAlertGraphMap", NodeProperties.class, MergedAlertGraph.class);
        this.mergedAlertGraphMap = getRuntimeContext().getMapState(mergedAlertGraphMapDescriptor);

        ValueStateDescriptor<UUID> hostUuidValueDescriptor
                = new ValueStateDescriptor<>("hostUuid", UUID.class);
        // get uuid
        this.hostUuid = getRuntimeContext().getState(hostUuidValueDescriptor);
        ValueStateDescriptor<String> hostIpValueDescriptor
                = new ValueStateDescriptor<>("hostIp", String.class, "0.0.0.0");
        //get ip
        this.hostIp = getRuntimeContext().getState(hostIpValueDescriptor);

        ValueStateDescriptor<Long> hostEventStartTimeDescriptor
                = new ValueStateDescriptor<>("hostEventStartTime", Long.class, 0L);
        // get startTime
        this.hostEventStartTime = getRuntimeContext().getState(hostEventStartTimeDescriptor);
        ValueStateDescriptor<Long> hostEventLatestTimeDescriptor
                = new ValueStateDescriptor<>("hostEventLatestTime", Long.class, 0L);
        //get latestTime
        this.hostEventLatestTime = getRuntimeContext().getState(hostEventLatestTimeDescriptor);
    }

    public static DataStream AnomalyPathMiningHandler(DataStream<PDM.LogPack> ds) throws Exception {
        TagBasedAnomalyPathMiningOnFlink detector = new TagBasedAnomalyPathMiningOnFlink();
        calculateRegularScore();
        DataStream<String> dsStringOutput =  ds
                .flatMap(new FlatMapFunction<PDM.LogPack, PDM.Log>() {
                    public void flatMap(PDM.LogPack logPack, Collector<PDM.Log> collector) throws Exception {
                        int logInfoCount = -1;
                        try {
                            for (PDM.Log log : logPack.getDataList()) {
                                logInfoCount++;
                                collector.collect(log);
                            }
                        } catch (Exception exception) {
                            PDM.Log tmp = logPack.getDataList().get(logInfoCount);
                            System.out.println(tmp);
                            System.out.println("--" + logInfoCount + "/" + logPack.getDataList().size() + exception);
                        }
                    }
                })
                .keyBy(log -> log.getUHeader().getClientID())
                .process(detector).name("TagBasedAnomalyPathMining");

        return dsStringOutput;
    }

    @Override
    public void processElement(PDM.Log log,
                               KeyedProcessFunction<PDM.HostUUID, PDM.Log, String>.Context ctx,
                               Collector<String> out) throws Exception {

        if (hostUuid == null) initProcessing(log);

        if (log.getUHeader().getType() == PDM.LogType.EVENT) {
            if (log.hasEventData()) {
                Long processedEventCount = processedEventCountValue.value() + 1;
                processedEventCountValue.update(processedEventCount);
                eventCount += 1;
                System.out.println(eventCount);
                if (eventCount % 100000L == 0) {
                    System.out.println(lostEventCount + "/" + processedEventCount + " of Events lost!");
                    updateStatisticInfo(log.getEventData().getEHeader().getTs());
                    printRegularInformation();
                }

                AnomalyScoreTagCache tag;
                try {
                    AssociatedEvent associatedEvent = initAssociatedEvent(log);

                    if (isNodeTagCached(associatedEvent.sourceNode)) associatedEvent.sourceNodeTag = getTagCache(associatedEvent.sourceNode);
                    if (isNodeTagCached(associatedEvent.sinkNode)) associatedEvent.sinkNodeTag = getTagCache(associatedEvent.sinkNode);

                    tag = processEvent(associatedEvent);
                }
                catch(Exception exception) {
                    return;
                }

                if (tag == null) return;
                else {
                    String alertJsonString = alertGeneration(tag);
                    if (outputTarget.equals("kafka"))
                        out.collect(alertJsonString);
                    else if (outputTarget.equals("stdout"))
                        System.out.println(alertJsonString);
                }
            }
        }
    }

    private void initProcessing(PDM.Log log) throws Exception {
        UUID thisHostUuid = hostUuidToUuid(log.getUHeader().getClientID());
        hostUuidSet.add(thisHostUuid);
        hostUuid.update(thisHostUuid);

        Long eventTime = log.getEventData().getEHeader().getTs();
        updateStatisticInfo(eventTime);

        printRegularInformation();
        System.out.println(hostUuidSet.size());
    }

    private String alertGeneration(AnomalyScoreTagCache tag) throws Exception {
        alertCount += 1;
        Long eventTime = tag.anomalyPath.get(tag.anomalyPath.size()-1).f2;
        updateStatisticInfo(eventTime);

        printRegularInformation();
        System.out.println(tag.toString());

        MergedAlertGraph singleAlertPath = new MergedAlertGraph();
        singleAlertPath.insertPath(tag.anomalyPath);

        AlertFormatter anomalyAlertFormatter = new AlertFormatter(
                System.currentTimeMillis(),
                "AnomalyPathMining",
                "SinglePath",
                tag.getAnomalyScore(),
                hostUuid.value(),
                hostIp.value(),
                singleAlertPath);
        String alertJsonString = anomalyAlertFormatter.toJsonString();
        return alertJsonString;
    }

    private void removeOutDatedTags(Long currentTime) throws Exception {
        // FixMe: https://blog.csdn.net/baifanwudi/article/details/103958682
        for (Map.Entry<UUID, AnomalyScoreTagCache> keyValue : tagsCacheMap.entries()) {
             if (keyValue.getValue().shouldDecayed(currentTime)) {
                 tagsCacheMap.remove(keyValue.getKey());
                 AnomalyScoreTagCache.decayedTagCount += 1;
             }
         }
    }


    @Override
    public void setTagCache(BasicNode node, AnomalyScoreTagCache tagCache) throws Exception {
        tagsCacheMap.put(node.getNodeId(), tagCache);
    }

    @Override
    public boolean isNodeTagCached(BasicNode node) throws Exception {
        return tagsCacheMap.contains(node.getNodeId());
    }

    @Override
    public AnomalyScoreTagCache getTagCache(BasicNode node) throws Exception {
        return tagsCacheMap.get(node.getNodeId());
    }

    @Override
    public ArrayList getTagCaches(BasicNode node) throws Exception {
        return null;
    }

    @Override
    public void removeTagCache(BasicNode node) throws Exception {
        tagsCacheMap.remove(node.getNodeId());
    }

    @Override
    public BasicNode getNodeInfo(UUID uuid) throws Exception {
        if (nodeInfoMap.contains(uuid)) {
            return nodeInfoMap.get(uuid);
        }
        return null;
    }

    public static void setEventRelativeFrequencyMap(Map eventRelativeFrequencyMap) {
        TagBasedAnomalyPathMiningOnFlink.eventRelativeFrequencyMap = eventRelativeFrequencyMap;
    }

    @Override
    public Double getEventRegularScore(AssociatedEvent associatedEvent) throws Exception {
        if (eventRelativeFrequencyMap.containsKey(associatedEvent)) {
            return (eventRelativeFrequencyMap.get(associatedEvent)) * (1 - seenEventMinimumScore) + seenEventMinimumScore;
        }

        else return unseenEventScore;
    }

    public void printRegularInformation() throws Exception {
        Long processingTimeSpan = (currentTime - detectionStartTime) / 1000;
        String overallProcessingInfo = String.format("Overall Processing Info: [ProcessingStartTime: %d (s), CurrentTime: %d (s), " +
                        " \n\tEvent Process Rate: %f (event per second) = %d (event) / %d (s), " +
                        " \n\tAlert Generation Rate: %f (event per alert) = %d (event) / %d (alert)]",
                detectionStartTime/1000,
                currentTime/1000,
                ((float) eventCount) / processingTimeSpan,
                eventCount,
                processingTimeSpan,
                ((float) eventCount) / alertCount,
                eventCount,
                alertCount
        );
        System.out.println(overallProcessingInfo);

//        float hostEventTimeSpan = (int) ((hostEventLatestTime.value() - hostEventStartTime.value()) / 1000000000);
        Long hostEventTimeSpan = processingTimeSpan;
        String singleHostProcessingInfo = String.format("Single Host Processing Info: [CurrentEventTime: %d (s)" +
                        "\n\tAlert Generation Rate: %f (second per alert) = %d (second) / %d (alert)]",
                hostEventLatestTime.value() / 1000000000,
                ((float) hostEventTimeSpan) / alertCount,
                hostEventTimeSpan,
                alertCount
        );
        System.out.println(singleHostProcessingInfo);

        String tagStatInfo = String.format("Anomaly Tag Status Info: [TotalEventCount: %d, TotalTagCount: %d = InitialTagCount: %d + PropagationCount: %d, DecayedTagCount: %d, TotalAlertCount: %d]",
                eventCount,
                AnomalyScoreTagCache.totalTagCount,
                AnomalyScoreTagCache.initialTagCount,
                AnomalyScoreTagCache.propagationCount,
                AnomalyScoreTagCache.decayedTagCount,
                AnomalyScoreTagCache.totalAlertCount
        );
        System.out.println(tagStatInfo);
    }

    private void updateStatisticInfo(Long eventTime) throws IOException {
        currentTime = System.currentTimeMillis();
        if (eventTime == 0L) return;
        if (hostEventStartTime.value() == 0L) hostEventStartTime.update(eventTime);
        hostEventLatestTime.update(eventTime);
    }

    public static void calculateRegularScore() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("SystemLog\\EventFrequencyDB.out"));
        Tuple2<Map, Map> dbstring = (Tuple2<Map, Map>) objectInputStream.readObject();
        Map<AssociatedEvent, HashSet<String>> exactlyMatchEventFrequencyMap = dbstring.f0;
        Map<AssociatedEvent, HashSet<String>> sourceRelationshipMatchEventFrequencyMap = dbstring.f1;
        Map<AssociatedEvent, Double> map = new HashMap<>();
        for (AssociatedEvent key : exactlyMatchEventFrequencyMap.keySet())
        {
            double fre_e = exactlyMatchEventFrequencyMap.get(key).size();
            double fre_src_rel = sourceRelationshipMatchEventFrequencyMap.get(key.ignoreSink()).size();
            double score = fre_e / fre_src_rel;
            map.put(key, score);
        }

        setEventRelativeFrequencyMap(map);
    }

}
