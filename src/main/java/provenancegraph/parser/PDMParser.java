package provenancegraph.parser;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicNode;
import provenancegraph.datamodel.PDM;
import provenancegraph.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import utils.Utils;


import static provenancegraph.datamodel.PDM.LogContent.*;
import static provenancegraph.datamodel.PDM.NetEvent.Direction.IN;
import static provenancegraph.datamodel.PDM.NetEvent.Direction.OUT;

public class PDMParser implements FlatMapFunction<PDM.LogPack, PDM.Log> {
    public static UUID processUuidToUuid(PDM.ProcessUUID processUuid) {
        return UUID.nameUUIDFromBytes(Bytes.concat(
                Longs.toByteArray(processUuid.getTs()),
                Ints.toByteArray(processUuid.getPid())));
    }

    public static UUID fileUuidToUuid(PDM.FileUUID fileUuid) {
        return UUID.nameUUIDFromBytes(Bytes.concat(
                Longs.toByteArray(fileUuid.getFilePathHash())));
    }

    public static UUID hostUuidToUuid(PDM.HostUUID hostUuid) {
        return UUID.nameUUIDFromBytes(Bytes.concat(
                Longs.toByteArray(hostUuid.getHostUUID())));
    }

    public static UUID filePathToUuid(String filePath) {
        return UUID.nameUUIDFromBytes(filePath.getBytes());
    }

    public static UUID networkToUuid(PDM.IPAddress remoteIp, int remotePort) {
        return UUID.nameUUIDFromBytes(Bytes.concat(
                Ints.toByteArray(remoteIp.getAddress()),
                Ints.toByteArray(remoteIp.getAddress1()),
                Ints.toByteArray(remoteIp.getAddress2()),
                Ints.toByteArray(remoteIp.getAddress3()),
                Ints.toByteArray(remotePort)));
    }

    public static BasicNode initBasicNode(PDM.Log log) {
        if (log.getUHeader().getType() == PDM.LogType.ENTITY && !log.getEventData().hasNetEvent()) {
            throw new RuntimeException("Cannot initBasicNode from non-entity logInfo!");
        }
        else {
            UUID nodeId;
            String nodeType;
            String nodeName;

//            if (log.hasProcess()) {
//                nodeId = processUuidToUuid(log.getProcess().getProcUUID());
//                nodeType = "Process";
//                nodeName = log.getProcess().getProcessName();
//            }
//            else if (log.hasFile()) {
//                nodeId = fileUuidToUuid(log.getFile().getFileUUID());
//                nodeType = "File";
//                nodeName = log.getFile().getFilePath();
//            }
            if (log.hasEventData() && log.getEventData().hasProcessEvent()){
                PDM.ProcessEvent processEvent = log.getEventData().getProcessEvent();
                nodeId = processUuidToUuid(processEvent.getChildProc().getProcUUID());
                nodeType = "Process";
                nodeName = processEvent.getChildProc().getProcessName();
            }
            else if (log.hasEventData() && log.getEventData().hasFileEvent()){
                PDM.FileEvent fileEvent = log.getEventData().getFileEvent();
                nodeId = fileUuidToUuid(fileEvent.getFile().getFileUUID());
                nodeType = "File";
                nodeName = fileEvent.getFile().getFilePath();
            }
            else if (log.hasEventData() && log.getEventData().hasNetEvent()){
                PDM.NetEvent netInfo = log.getEventData().getNetEvent();
                if (netInfo.getDirect() == IN)
                    nodeId = networkToUuid(netInfo.getSip(), netInfo.getSport());
                else {
                    nodeId = networkToUuid(netInfo.getDip(), netInfo.getDport());
                }
                nodeType = "Network";
                nodeName = Utils.convertIntToIpString(netInfo.getSip().getAddress());
            }
            else {
                return null;
            }
            return new BasicNode(nodeId, nodeType, nodeName);
        }
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

        //set sourceNode and sinkNode
        BasicNode source = initBasicSourceNode(log);
        BasicNode sink = initBasicSinkNode(log);

        source.setProperties(initSourceNodeProperties(log));
        sink.setProperties(initSinkNodeProperties(log));

        event.setSourceNode(source);
        event.setSinkNode(sink);

//        System.out.println(event.toJsonString());

        return event;
    }

    //subject
    public static BasicNode initBasicSourceNode(PDM.Log log){
        String ts = Long.toString(log.getEventData().getEHeader().getTs());
        String pid = Integer.toString(log.getEventData().getEHeader().getProc().getProcUUID().getPid());
        UUID uuid = new UUID(new BigInteger(pid, 16).longValue(),
                new BigInteger(ts, 16).longValue());

        String content = log.getUHeader().getContent().toString();
        String log_category;
        switch (content){
            case "PROCESS_FORK":
            case "PROCESS_EXEC":
            case "PROCESS_LOAD":
                log_category = "Process";break;
            case "FILE_OPEN":
            case "FILE_READ":
            case "FILE_WRITE":
                log_category = "File";break;
            case "NET_CONNECT":
                log_category = "Network";break;
            default:
                return null;

        }
        String nodeName;
        switch (log_category) {
            case "Process":
                nodeName = "Process";
                break;
            case "File":
                nodeName = "FileMonitor";
                break;
            case "Network":
                nodeName = "Network";
                break;
            default:
                nodeName = "";

        }
        return new BasicNode(uuid, "Process", nodeName);
    }

    //object
    public static BasicNode initBasicSinkNode(PDM.Log log) {
        UUID uuid;
        String nodeType;
        String nodeName;
        String log_category;
        String content = log.getUHeader().getContent().toString();
        switch (content){
            case "PROCESS_FORK":
            case "PROCESS_EXEC":
            case "PROCESS_LOAD":
                log_category = "Process";break;
            case "FILE_OPEN":
            case "FILE_READ":
            case "FILE_WRITE":
                log_category = "File";break;
            case "NET_CONNECT":
                log_category = "Network";break;
            default:
                return null;

        }
        switch (log_category) {
            case "Process":
                String ts = Long.toString(log.getEventData().getProcessEvent().getChildProc().getProcUUID().getTs());
                String pid = Integer.toString(log.getEventData().getProcessEvent().getChildProc().getProcUUID().getPid());
                uuid = new UUID(new BigInteger(ts, 16).longValue(),
                        new BigInteger(pid, 16).longValue());
                nodeType = "Process";
                nodeName = "Process";
                break;
            case "File":
                String filePathHash = Long.toString(log.getEventData().getFileEvent().getFile().getFileUUID().getFilePathHash());
                uuid = new UUID(new BigInteger(filePathHash, 16).longValue(),
                        new BigInteger(filePathHash, 16).longValue());
                nodeType = "File";
                nodeName = "FileMonitor";
                break;
            case "Network":
                String sip = Long.toString(log.getEventData().getNetEvent().getSip().getAddress());
                String dip = Integer.toString(log.getEventData().getNetEvent().getDip().getAddress());
                uuid = new UUID(new BigInteger(sip, 16).longValue(),
                        new BigInteger(dip, 16).longValue());
                nodeType = "Network";
                nodeName = "Network";
                break;
            default:
                uuid = new UUID(0,0);
                nodeType = "";
                nodeName = "";

        }
        return new BasicNode(uuid, nodeType, nodeName);
    }

    //add properties of subject (pid, path, cmd)
    public static NodeProperties initSourceNodeProperties(PDM.Log log){
        NodeProperties nodeProperties = new ProcessNodeProperties(log.getEventData().getEHeader().getProc().getProcUUID().getPid(),
                        log.getEventData().getEHeader().getProc().getExePath(),
                        log.getEventData().getEHeader().getProc().getCmdline(), log.getEventData().getEHeader().getProc().getProcessName());
       return nodeProperties;
    }

    public static NodeProperties initSinkNodeProperties(PDM.Log log){
        NodeProperties nodeProperties;
        String log_category;
        String content = log.getUHeader().getContent().toString();
        switch (content){
            case "PROCESS_FORK":
            case "PROCESS_EXEC":
            case "PROCESS_LOAD":
                log_category = "Process";break;
            case "FILE_OPEN":
            case "FILE_READ":
            case "FILE_WRITE":
                log_category = "File";break;
            case "NET_CONNECT":
                log_category = "Network";break;
            default:
                return null;

        }
        switch (log_category) {
            case "Process":
                nodeProperties = new ProcessNodeProperties(log.getEventData().getProcessEvent().getChildProc().getProcUUID().getPid(),
                        log.getEventData().getProcessEvent().getChildProc().getExePath(),
                        log.getEventData().getProcessEvent().getChildProc().getCmdline(), log.getEventData().getProcessEvent().getChildProc().getProcessName());
                break;
            case "File":
                nodeProperties = new FileNodeProperties(log.getEventData().getFileEvent().getFile().getFilePath());
                break;
            case "Network":
                PDM.NetEvent.Direction direct = log.getEventData().getNetEvent().getDirect();
                int dir = 2;
                if (direct == IN) dir = 0;
                if (direct == OUT) dir = 1;
                nodeProperties = new NetworkNodeProperties(String.valueOf(log.getEventData().getNetEvent().getDip().getAddress()),
                        String.valueOf(log.getEventData().getNetEvent().getDport()),
                        dir);
                break;
            default:
            nodeProperties = new FileNodeProperties("");
        }
        return nodeProperties;
    }
    
    
    @Override
    public void flatMap(PDM.LogPack logPack, Collector<PDM.Log> logCollector) throws Exception{
        List<PDM.Log> logList = logPack.getDataList();
        for (PDM.Log log : logList) {
            logCollector.collect(log);
        }
    }



//    ToDo: Change UDM item to PDM item.
//    public static NodeProperties initNodeProperties(PDM.Log log) {
//        if (log.getUHeader().getType() == PDM.LogType.ENTITY) {
//            switch(log.getUHeader().getContent()) {
//                case CLIENT_ENTITY:
//                    String clientIpString = "";
//                    for (PDM.IPAddress ipAddress: log.getClient().getIpListList()) {
////                        clientIpString += Util.intToIpv4(ipAddress.getAddress()) + ", ";
//                    }
//                    ClientProperties clientProperties = new ClientProperties(clientIpString);
//                    return clientProperties;
//                case PROCESS_ENTITY:
//                    PDM.Process processInfo = log.getEventData().getProcessEvent().getChildProc();
//                    ProcessNodeProperties processNodeProperties =
//                            new ProcessNodeProperties(
////                                    processInfo.getVirtualPid(),
//                                    processInfo.getExePath(),
//                                    processInfo.getCmdline());
//                    return processNodeProperties;
//                case FILE_ENTITY:
//                    PDM.File fileInfo = log.getFile();
//                    FileNodeProperties fileNodeProperties = new FileNodeProperties(fileInfo.getFilePath());
//                    return fileNodeProperties;
//                default:
//                    return null;
//            }
//            return null;
//        }
//        else if (log.getUHeader().getType() == PDM.LogType.EVENT
//                && log.getUHeader().getContent() == PDM.LogContent.NET_CONNECT) {
//            PDM.NetEvent netEventInfo = log.getEventData().getNetEvent();
//            PDM.NetEvent.Direction direction = netEventInfo.getDirect();
//            if (direction == IN) {
//                NetworkNodeProperties networkNodeProperties =
//                        new NetworkNodeProperties(Utils.convertIntToIpString(netEventInfo.getSip().getAddress()),
//                                Integer.toString(netEventInfo.getSport()),
//                                netEventInfo.getDirectValue());
//                return networkNodeProperties;
//            }
//            else if (direction == OUT) {
//                NetworkNodeProperties networkNodeProperties =
//                        new NetworkNodeProperties(Utils.convertIntToIpString(netEventInfo.getDip().getAddress()),
//                                Integer.toString(netEventInfo.getDport()),
//                                netEventInfo.getDirectValue());
//                return networkNodeProperties;
//            }
//            else return null;
//        }
//        else return null;
//    }

//    ToDo: Change UDM item to PDM item.
//    public static BasicEdge initBasicEdge(PDM.Log log) {
//        PDM.LogContent eventType = log.getUHeader().getContent();
//        PDM.EventData eventInfo = log.getEventData();
//        long timeStamp = eventInfo.getEHeader().getTs();
//
//        UUID sourceUuid;
//        UUID sinkUuid;
//        String type = eventType.toString();
//
//        switch (eventType) {
//            case PROCESS_FORK:
//            case PROCESS_EXEC:
//                sourceUuid = processUuidToUuid(eventInfo.getEHeader().getProc().getProcUUID());
//                sinkUuid = processUuidToUuid(eventInfo.getProcessEvent().getChildProc().getProcUUID());
//                break;
//            case FILE_WRITE:
//                sourceUuid = processUuidToUuid(eventInfo.getEHeader().getProc().getProcUUID());
//                sinkUuid = fileUuidToUuid(eventInfo.getFileEvent().getFile().getFileUUID());
//                PDM.File.FileType fileType = eventInfo.getFileEvent().getFile().getFileType();
////                if (fileType == PDM.File.FileType.FILE_FIFO || fileType == PDM.File.FileType.FILE_SOCK) {
////                    BasicEdge pipeIOEdge = UnNamedEntity.handleUnNamedEntity(sourceUuid, sinkUuid, UnNamedEntity.IODirection.WRITE, timeStamp, fileType);
////                    return pipeIOEdge;
////                }
//                break;
//            case FILE_READ:
//            case FILE_OPEN:
//                sourceUuid = processUuidToUuid(eventInfo.getEHeader().getProc().getProcUUID());
//                sinkUuid = processUuidToUuid(eventInfo.getEHeader().getProc().getProcUUID());
//                fileType = eventInfo.getFileEvent().getFile().getFileType();
////                if (fileType == PDM.File.FILE_TYPE.FILE_FIFO || fileType == PDM.File.FILE_TYPE.FILE_SOCK) {
////                    BasicEdge pipeIOEdge = UnNamedEntity.handleUnNamedEntity(sourceUuid, sinkUuid, UnNamedEntity.IODirection.READ, timeStamp, fileType);
////                    return pipeIOEdge;
////                }
//                break;
//            case NET_CONNECT:  // FixMe: NET_CONNECT has no direction
//                PDM.NetEvent.Direction direction = eventInfo.getNetEvent().getDirect();
//                if (direction == IN) {
//                    sourceUuid = networkToUuid(eventInfo.getNetEvent().getSip(), eventInfo.getNetEvent().getSport());
//                    sinkUuid = processUuidToUuid(eventInfo.getEHeader().getProc().getProcUUID());
//                }
//                else if (direction == OUT) {
//                    sourceUuid = networkToUuid(eventInfo.getNetEvent().getDip(), eventInfo.getNetEvent().getDport());
//                    sinkUuid = processUuidToUuid(eventInfo.getEHeader().getProc().getProcUUID());
//                }
//                else return null;
//                break;
//            default:
//                return null;
//        }
//
//        return new BasicEdge(type, timeStamp, sourceUuid, sinkUuid);
//    }
}
