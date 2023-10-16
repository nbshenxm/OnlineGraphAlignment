package libtagpropagation.nodoze;


import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import provenancegraph.AssociatedEvent;
import provenancegraph.BasicEdge;
import provenancegraph.BasicNode;
import provenancegraph.NodeProperties;
import provenancegraph.datamodel.PDM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static provenancegraph.AssociatedEvent.generalizeTime;
import static provenancegraph.datamodel.PDM.NetEvent.Direction.IN;
import static provenancegraph.parser.PDMParser.*;
import static provenancegraph.parser.PDMParser.networkToUuid;

public class EventFrequencyDBConstructionWithFlink extends KeyedProcessFunction<PDM.HostUUID, PDM.Log, Row> {
    public static Long dbDumpEventCount = 300000L;
    private transient MapState<UUID, BasicNode> nodeInfoMap;

    private transient MapState<AssociatedEvent, HashSet<String>> exactlyMatchEventFrequencyMap;
    private transient MapState<AssociatedEvent, HashSet<String>> sourceRelationshipMatchEventFrequencyMap;  // Event -> Set{Time-Host}

    private transient ValueState<Long> processedEventCountValue;
    private transient ValueState<Long> lostEventCountValue;

    public static boolean isLocalTask = false;
    public static boolean isCreated = false;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        MapStateDescriptor<UUID, BasicNode> nodeInfoStateDescriptor =
                new MapStateDescriptor<>("nodeInfoMap", UUID.class, BasicNode.class);
        this.nodeInfoMap = getRuntimeContext().getMapState( new MapStateDescriptor<>("nodeInfoMap", UUID.class, BasicNode.class));

        MapStateDescriptor<AssociatedEvent, HashSet<String>> exactlyMatchEventFrequencyMapDescriptor =
                new MapStateDescriptor<>("exactlyMatchEventFrequencyMap",
                        TypeInformation.of(AssociatedEvent.class),
                        TypeInformation.of(new TypeHint<HashSet<String>>() {
                        }));
        exactlyMatchEventFrequencyMap = getRuntimeContext().getMapState(exactlyMatchEventFrequencyMapDescriptor);

        MapStateDescriptor<AssociatedEvent, HashSet<String>> sourceRelationshipMatchEventFrequencyMapDescriptor =
                new MapStateDescriptor<>("sourceRelationshipMatchEventFrequencyMap",
                        TypeInformation.of(AssociatedEvent.class),
                        TypeInformation.of(new TypeHint<HashSet<String>>() {
                        }));
        sourceRelationshipMatchEventFrequencyMap = getRuntimeContext().getMapState(sourceRelationshipMatchEventFrequencyMapDescriptor);

        ValueStateDescriptor<Long> processedEventCountValueDescriptor =
                new ValueStateDescriptor<>("processedEventCountValue", Long.class, 0L);
        processedEventCountValue = getRuntimeContext().getState(processedEventCountValueDescriptor);

        ValueStateDescriptor<Long> lostEventCountValueDescriptor =
                new ValueStateDescriptor<>("lostEventCountValue", Long.class, 0L);
        lostEventCountValue = getRuntimeContext().getState(lostEventCountValueDescriptor);
    }

    public static DataStream EventFrequencyDBConstructionHandler(DataStream<PDM.LogPack> ds) {
        DataStream<Row> dsOutput = ds.flatMap(new FlatMapFunction<PDM.LogPack, PDM.Log>() {
                    public void flatMap(PDM.LogPack logpack, Collector<PDM.Log> collector) throws Exception {
                        int logCount = -1;
                        try {
                            for (PDM.Log log : logpack.getDataList()) {
                                logCount++;
                                collector.collect(log);
                            }
                        } catch (Exception exception) {
                            PDM.Log tmp = logpack.getDataList().get(logCount);
                            System.out.println(tmp);
                            System.out.println("--" + logCount + "/" + logpack.getDataList().size() + exception);
                        }
                    }
                })
                .keyBy(log -> getHostType(log.getUHeader().getClientID()))
                .process(new EventFrequencyDBConstructionWithFlink()).name("EventFrequencyDBConstructionHandler");

        return dsOutput;
    }

    @Override
    public void processElement(PDM.Log log, KeyedProcessFunction<PDM.HostUUID, PDM.Log, Row>.Context context, Collector<Row> collector) throws Exception {
        //count the number of process events
        Long processedEventCount = processedEventCountValue.value() + 1;
        Long lostEventCount = lostEventCountValue.value();

        if (processedEventCount % dbDumpEventCount == 0) {
            System.out.println(String.format("[ProcessedEventCount: %d, LostEventCount: %d, EventTime: %s]",
                    processedEventCount,
                    this.lostEventCountValue.value(),
                    new Date(log.getEventData().getEHeader().getTs() / 1000000)));

            //dumpEventFrequencyDBToFile(); //String.valueOf(processedEventCount / DB_DUMP_EVENT_COUNT)
            System.out.println("Continuing ...");
        }

        processedEventCountValue.update(processedEventCount);
        System.out.println(processedEventCountValue.value());

        // process entities.
        if (log.getUHeader().getType() == PDM.LogType.EVENT
                || log.getUHeader().getContent() == PDM.LogContent.NET_CONNECT) {
            BasicNode node = initBasicSinkNode(log);
            NodeProperties properties = initSinkNodeProperties(log);
            if (node == null || properties == null) {
                lostEventCountValue.update(lostEventCountValue.value() + 1);
                return;
            }
            node.setProperties(properties);
            nodeInfoMap.put(node.getNodeId(), node);
        }

        // process events.
        if (log.getUHeader().getType() == PDM.LogType.EVENT)
        {

            if (log.hasEventData())
            {
                //
//                BasicEdge edge = UDMTools.initBasicEdge(log);
                BasicEdge edge = initBasicEdge(log);
                if (edge == null) {
//                    lostEventCountValue.update(lostEventCountValue.value() + 1);
                    return;
                }

                // judge if event lost
                if (!(nodeInfoMap.contains(edge.sourceNodeId) && nodeInfoMap.contains(edge.sinkNodeId))) {
                    lostEventCountValue.update(lostEventCountValue.value() + 1);
                    return;
                }

                // generate associatedevent
                NodeProperties sourceNodeProperties = nodeInfoMap.get(edge.sourceNodeId).getProperties();
                NodeProperties sinkNodeProperties = nodeInfoMap.get(edge.sinkNodeId).getProperties();
                String eventType = edge.edgeType;

                Long timeStamp = edge.timeStamp;
                AssociatedEvent associatedEvent = new AssociatedEvent(sourceNodeProperties, sinkNodeProperties, eventType, timeStamp);
                String item = log.getUHeader().getClientID().toString() + generalizeTime(timeStamp);


                // add item of event
                AssociatedEvent eEvent = associatedEvent.copyGeneralize();
                if (!exactlyMatchEventFrequencyMap.contains(eEvent)) {
                    exactlyMatchEventFrequencyMap.put(eEvent, new HashSet<>());
                }
                exactlyMatchEventFrequencyMap.get(eEvent).add(item);

                AssociatedEvent srEvent = eEvent.ignoreSink();
                if (!sourceRelationshipMatchEventFrequencyMap.contains(srEvent)) {
                    sourceRelationshipMatchEventFrequencyMap.put(srEvent, new HashSet<>());
                }
                sourceRelationshipMatchEventFrequencyMap.get(srEvent).add(item);
            }
        }

    }

    public static String serializeObjectToString(Object input) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(input);
        String output = byteOut.toString("ISO-8859-1");
        return output;
    }

    public Map covertMapStateToMap(MapState<AssociatedEvent, HashSet<String>> mapState) throws Exception {
        HashMap map = new HashMap<>();
        for (AssociatedEvent key : mapState.keys()) {
            map.put(key, mapState.get(key));
        }
        System.out.println("[Map size: " + map.size() + "]");

        return map;
    }

    public Tuple2<String, String> dumpEventFrequencyDBToStrings() throws Exception {
        Tuple2<String, String> output = new Tuple2<>(
                serializeObjectToString(covertMapStateToMap(exactlyMatchEventFrequencyMap)),
                serializeObjectToString(covertMapStateToMap(sourceRelationshipMatchEventFrequencyMap)));
        return output;
    }
    public void dumpEventFrequencyDBToFile() throws Exception {
        Tuple2<String, String> dbStrings = dumpEventFrequencyDBToStrings();

//        IOHandler eMapHandler;
//        IOHandler srMapHandler;
//
//        if (isLocalTask){
//            eMapHandler = new FsHandler(fsPath + eMapFileName, false);
//            srMapHandler = new FsHandler(fsPath + srMapFileName, false);
//        }
//        else {
//            eMapHandler = new HadoopHdfsHandler(hdfsServerUri, hdfsPath + eMapFileName, false, false);
//            srMapHandler = new HadoopHdfsHandler(hdfsServerUri, hdfsPath + srMapFileName, false, false);
//        }
//        eMapHandler.writeString(dbStrings.f0);
//        srMapHandler.writeString(dbStrings.f1);
    }

    // ? uuid
    public static PDM.HostUUID getHostType(PDM.HostUUID uuid) {
        return PDM.HostUUID.newBuilder().build();
    }

}
