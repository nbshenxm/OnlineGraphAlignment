package provenancegraph.parser;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;
import provenancegraph.datamodel.PDM;
import provenancegraph.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

import static provenancegraph.datamodel.PDM.NetEvent.Direction.IN;
import static provenancegraph.datamodel.PDM.NetEvent.Direction.OUT;

public class PDMParser implements FlatMapFunction<PDM.LogPack, PDM.Log> {

    public static UUID hostUuidToUuid(PDM.HostUUID hostUuid) {
        return UUID.nameUUIDFromBytes(Bytes.concat(
                Longs.toByteArray(hostUuid.getHostUUID())));
    }

    public static AssociatedEvent initAssociatedEvent(PDM.Log log){

        AssociatedEvent event = new AssociatedEvent();
        // set hostUUID,timeStamp and relationship
        Long hostUUID = log.getUHeader().getClientID().getHostUUID();
        event.setHostUUID(new UUID(hostUUID,hostUUID));
        long ts = log.getEventData().getEHeader().getTs();
        event.setTimeStamp(ts);

        String content = log.getUHeader().getContent().toString();
        event.setRelationship(content);

        BasicNode source;
        BasicNode sink;
        switch (log.getUHeader().getContent()){
            case PROCESS_FORK:
            case PROCESS_EXEC:
                source = initBasicNode(log.getEventData().getEHeader().getProc());
                sink = initBasicNode(log.getEventData().getProcessEvent().getChildProc());
                break;
            case FILE_WRITE:
            case FILE_OPEN:
                source = initBasicNode(log.getEventData().getEHeader().getProc());
                sink = initBasicNode(log.getEventData().getFileEvent().getFile());
                break;
            case FILE_READ:
            case PROCESS_LOAD:
                sink = initBasicNode(log.getEventData().getEHeader().getProc());
                source = initBasicNode(log.getEventData().getFileEvent().getFile());
                break;
            case NET_CONNECT:
                if (log.getEventData().getNetEvent().getDirect() == IN){
                    sink = initBasicNode(log.getEventData().getEHeader().getProc());
                    source = initBasicNode(log.getEventData().getNetEvent());
                }else {
                    source = initBasicNode(log.getEventData().getEHeader().getProc());
                    sink = initBasicNode(log.getEventData().getNetEvent());
                }
                break;
            default:
                return null;
        }

        String eventUUID = source.getNodeUUID().toString() + sink.getNodeUUID().toString() + event.getRelationship();
        event.setEventUUID(UUID.nameUUIDFromBytes(eventUUID.getBytes(StandardCharsets.UTF_8)));

        event.setSourceNode(source);
        event.setSinkNode(sink);
//        System.out.println(event.toJsonString());
        return event;
    }

    public static BasicNode initBasicNode(PDM.Process process){
        Long ts = process.getProcUUID().getTs();
        int pid = process.getProcUUID().getPid();
        UUID uuid = new UUID(ts, pid);

        BasicNode basicNode = new BasicNode(uuid, "Process", "Process");

        NodeProperties nodeProperties = new ProcessNodeProperties(pid,
                process.getExePath(),
                process.getCmdline(), process.getProcessName());
        basicNode.setProperties(nodeProperties);
        return basicNode;
    }

    public static BasicNode initBasicNode(PDM.File file){
        UUID uuid = new UUID(file.getFileUUID().getFilePathHash(), file.getFileUUID().getFilePathHash());

        BasicNode basicNode = new BasicNode(uuid, "File", "FileMonitor");

        NodeProperties nodeProperties = new FileNodeProperties(file.getFilePath());
        basicNode.setProperties(nodeProperties);
        return basicNode;
    }

    public static BasicNode initBasicNode(PDM.NetEvent netEvent){

        String dport = String.valueOf(netEvent.getDport());
        String dip = parseIPAddress(netEvent.getDip());
        UUID uuid = UUID.nameUUIDFromBytes((dip+dport).getBytes(StandardCharsets.UTF_8));

        BasicNode basicNode = new BasicNode(uuid, "Network", "Network");
        PDM.NetEvent.Direction direct = netEvent.getDirect();
        int dir = 2;
        if (direct == IN) dir = 0;
        if (direct == OUT) dir = 1;
        NodeProperties nodeProperties = new NetworkNodeProperties(dip,
                String.valueOf(netEvent.getDport()),
                dir);
        basicNode.setProperties(nodeProperties);
        return basicNode;
    }

    public static String parseIPAddress(PDM.IPAddress ipAddress){
        StringBuilder ipv4 = new StringBuilder();
        ipv4.append(ipAddress.getAddress() + ".");
        ipv4.append(ipAddress.getAddress1() + ".");
        ipv4.append(ipAddress.getAddress2() + ".");
        ipv4.append(ipAddress.getAddress3());
        return ipv4.toString();
    }
    
    @Override
    public void flatMap(PDM.LogPack logPack, Collector<PDM.Log> logCollector) throws Exception{
        List<PDM.Log> logList = logPack.getDataList();
        for (PDM.Log log : logList) {
            logCollector.collect(log);
        }
    }

}
