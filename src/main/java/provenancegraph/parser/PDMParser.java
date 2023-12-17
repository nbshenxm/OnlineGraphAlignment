package provenancegraph.parser;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;
import provenancegraph.datamodel.PDM;
import provenancegraph.*;

import java.math.BigInteger;
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
        String filePathHash = Long.toString(file.getFileUUID().getFilePathHash());
        UUID uuid = new UUID(new BigInteger(filePathHash, 16).longValue(),
                new BigInteger(filePathHash, 16).longValue());

        BasicNode basicNode = new BasicNode(uuid, "File", "FileMonitor");

        NodeProperties nodeProperties = new FileNodeProperties(file.getFilePath());
        basicNode.setProperties(nodeProperties);
        return basicNode;
    }

    public static BasicNode initBasicNode(PDM.NetEvent netEvent){

        String sip = Long.toString(netEvent.getSip().getAddress());
        String dip = Integer.toString(netEvent.getDip().getAddress());
        UUID uuid = new UUID(new BigInteger(sip, 16).longValue(),
                new BigInteger(dip, 16).longValue());

        BasicNode basicNode = new BasicNode(uuid, "Network", "Network");
        PDM.NetEvent.Direction direct = netEvent.getDirect();
        int dir = 2;
        if (direct == IN) dir = 0;
        if (direct == OUT) dir = 1;
        NodeProperties nodeProperties = new NetworkNodeProperties(sip,
                String.valueOf(netEvent.getDport()),
                dir);
        basicNode.setProperties(nodeProperties);
        return basicNode;
    }

//
//    //subject
//    public static BasicNode initBasicSourceNode(PDM.Log log){
//        Long ts = log.getEventData().getEHeader().getProc().getProcUUID().getTs();
//        int pid = log.getEventData().getEHeader().getProc().getProcUUID().getPid();
//        UUID uuid = new UUID(ts, pid);
//        String nodeName = "Process";
//        return new BasicNode(uuid, "Process", nodeName);
//    }
//
//    //object
//    public static BasicNode initBasicSinkNode(PDM.Log log) {
//        UUID uuid;
//        String nodeType;
//        String nodeName;
//        String log_category;
//        String content = log.getUHeader().getContent().toString();
//        switch (content){
//            case "PROCESS_FORK":
//            case "PROCESS_EXEC":
//                log_category = "Process";break;
//            case "FILE_WRITE":
//            case "FILE_OPEN":
//            case "FILE_READ":
//            case "PROCESS_LOAD":
//                log_category = "File";break;
//            case "NET_CONNECT":
//                log_category = "Network";break;
//            default:
//                return null;
//
//        }
//        switch (log_category) {
//            case "Process":
//                Long ts = log.getEventData().getProcessEvent().getChildProc().getProcUUID().getTs();
//                int pid = log.getEventData().getProcessEvent().getChildProc().getProcUUID().getPid();
//                uuid = new UUID(ts, pid);
//                nodeType = "Process";
//                nodeName = "Process";
//                break;
//            case "File":
//                String filePathHash = Long.toString(log.getEventData().getFileEvent().getFile().getFileUUID().getFilePathHash());
//                uuid = new UUID(new BigInteger(filePathHash, 16).longValue(),
//                        new BigInteger(filePathHash, 16).longValue());
//                nodeType = "File";
//                nodeName = "FileMonitor";
//                break;
//            case "Network":
//                String sip = Long.toString(log.getEventData().getNetEvent().getSip().getAddress());
//                String dip = Integer.toString(log.getEventData().getNetEvent().getDip().getAddress());
//                uuid = new UUID(new BigInteger(sip, 16).longValue(),
//                        new BigInteger(dip, 16).longValue());
//                nodeType = "Network";
//                nodeName = "Network";
//                break;
//            default:
//                uuid = new UUID(0,0);
//                nodeType = "";
//                nodeName = "";
//
//        }
//        return new BasicNode(uuid, nodeType, nodeName);
//    }
//
//    //add properties of subject (pid, path, cmd)
//    public static NodeProperties initSourceNodeProperties(PDM.Log log){
//        NodeProperties nodeProperties = new ProcessNodeProperties(log.getEventData().getEHeader().getProc().getProcUUID().getPid(),
//                        log.getEventData().getEHeader().getProc().getExePath(),
//                        log.getEventData().getEHeader().getProc().getCmdline(), log.getEventData().getEHeader().getProc().getProcessName());
//       return nodeProperties;
//    }
//
//    public static NodeProperties initSinkNodeProperties(PDM.Log log){
//        NodeProperties nodeProperties;
//        String log_category;
//        String content = log.getUHeader().getContent().toString();
//        switch (content){
//            case "PROCESS_FORK":
//            case "PROCESS_EXEC":
//                log_category = "Process";break;
//            case "PROCESS_LOAD":
//            case "FILE_OPEN":
//            case "FILE_READ":
//            case "FILE_WRITE":
//                log_category = "File";break;
//            case "NET_CONNECT":
//                log_category = "Network";break;
//            default:
//                return null;
//
//        }
//        switch (log_category) {
//            case "Process":
//                nodeProperties = new ProcessNodeProperties(log.getEventData().getProcessEvent().getChildProc().getProcUUID().getPid(),
//                        log.getEventData().getProcessEvent().getChildProc().getExePath(),
//                        log.getEventData().getProcessEvent().getChildProc().getCmdline(), log.getEventData().getProcessEvent().getChildProc().getProcessName());
//                break;
//            case "File":
//                nodeProperties = new FileNodeProperties(log.getEventData().getFileEvent().getFile().getFilePath());
//                break;
//            case "Network":
//                PDM.NetEvent.Direction direct = log.getEventData().getNetEvent().getDirect();
//                int dir = 2;
//                if (direct == IN) dir = 0;
//                if (direct == OUT) dir = 1;
//                nodeProperties = new NetworkNodeProperties(String.valueOf(log.getEventData().getNetEvent().getDip().getAddress()),
//                        String.valueOf(log.getEventData().getNetEvent().getDport()),
//                        dir);
//                break;
//            default:
//            nodeProperties = new FileNodeProperties("");
//        }
//        return nodeProperties;
//    }
//
    
    @Override
    public void flatMap(PDM.LogPack logPack, Collector<PDM.Log> logCollector) throws Exception{
        List<PDM.Log> logList = logPack.getDataList();
        for (PDM.Log log : logList) {
            logCollector.collect(log);
        }
    }

}
