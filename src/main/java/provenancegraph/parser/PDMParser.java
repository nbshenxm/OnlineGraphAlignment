package provenancegraph.parser;

import org.apache.flink.util.Collector;
import provenancegraph.AssociatedEvent;
import provenancegraph.BasicEdge;
import provenancegraph.BasicNode;
import provenancegraph.NodeProperties;
import provenancegraph.datamodel.PDM;

import java.util.List;
import java.util.UUID;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import utils.Utils;

public class PDMParser {
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
        if (log.getUHeader().getType() != PDM.LogType.ENTITY && !log.getEventData().hasNetEvent()) {
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
                if (netInfo.getDirect() == PDM.NetEvent.Direction.IN)
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
        return null;
    }

    public static void Unpack(PDM.LogPack logPack, Collector<PDM.Log> logCollector) {
        List<PDM.Log> logList = logPack.getDataList();
        for (PDM.Log log : logList) {
            logCollector.collect(log);
        }
    }

//    ToDo: Change UDM item to PDM item.
//    public static NodeProperties initNodeProperties(PDM.Log log) {
//        if (log.getUHeader().getCategory() == PDM.LogType.ENTITY) {
//            switch(log.getUHeader().getType()) {
//                case CLIENT_ENTITY:
//                    String clientIpString = "";
//                    for (PDM.IPAddress ipAddress: log.getClient().getIpListList()) {
//                        clientIpString += Util.intToIpv4(ipAddress.getAddress()) + ", ";
//                    }
//                    ClientProperties clientProperties = new ClientProperties(clientIpString);
//                    return clientProperties;
//                case PROCESS_ENTITY:
//                    PDM.Process processInfo = log.getProcess();
//                    ProcessNodeProperties processNodeProperties =
//                            new ProcessNodeProperties(
//                                    processInfo.getVirtualPid(),
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
//        }
//        else if (log.getUHeader().getCategory() == PDM.logCategory.Event
//                && log.getUHeader().getType() == PDM.LogType.NET_CONNECT) {
//            PDM.NetEventInfo netEventInfo = log.getEventInfo().getNetInfo();
//            PDM.NetEventInfo.Direction direction = netEventInfo.getDirect();
//            if (direction == PDM.NetEventInfo.Direction.in) {
//                NetworkNodeProperties networkNodeProperties =
//                        new NetworkNodeProperties(Utils.convertIntToIpString(netEventInfo.getSip().getAddress()),
//                                Integer.toString(netEventInfo.getSport()),
//                                netEventInfo.getDirectValue());
//                return networkNodeProperties;
//            }
//            else if (direction == PDM.NetEventInfo.Direction.out) {
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
//        PDM.LogType eventType = log.getUHeader().getType();
//        PDM.EventInfo eventInfo = log.getEventInfo();
//        long timeStamp = eventInfo.getEHeader().getTs();
//
//        UUID sourceUuid;
//        UUID sinkUuid;
//        String type = eventType.toString();
//
//        switch (eventType) {
//            case PROCESS_FORK:
//            case PROCESS_EXEC:
//                sourceUuid = processUuidToUuid(eventInfo.getEHeader().getProcUUID());
//                sinkUuid = processUuidToUuid(eventInfo.getProcessInfo().getChildProcUUID());
//                break;
//            case FILE_CREATE:
//            case FILE_WRITE:
//                sourceUuid = processUuidToUuid(eventInfo.getEHeader().getProcUUID());
//                sinkUuid = fileUuidToUuid(eventInfo.getFileInfo().getFileUUID());
//                PDM.File.FILE_TYPE fileType = eventInfo.getFileInfo().getFileType();
//                if (fileType == PDM.File.FILE_TYPE.FILE_FIFO || fileType == PDM.File.FILE_TYPE.FILE_SOCK) {
//                    BasicEdge pipeIOEdge = UnNamedEntity.handleUnNamedEntity(sourceUuid, sinkUuid, UnNamedEntity.IODirection.WRITE, timeStamp, fileType);
//                    return pipeIOEdge;
//                }
//                break;
//            case FILE_READ:
//            case FILE_OPEN:
//                sourceUuid = fileUuidToUuid(eventInfo.getFileInfo().getFileUUID());
//                sinkUuid = processUuidToUuid(eventInfo.getEHeader().getProcUUID());
//                fileType = eventInfo.getFileInfo().getFileType();
//                if (fileType == PDM.File.FILE_TYPE.FILE_FIFO || fileType == PDM.File.FILE_TYPE.FILE_SOCK) {
//                    BasicEdge pipeIOEdge = UnNamedEntity.handleUnNamedEntity(sourceUuid, sinkUuid, UnNamedEntity.IODirection.READ, timeStamp, fileType);
//                    return pipeIOEdge;
//                }
//                break;
//            case NET_CONNECT:  // FixMe: NET_CONNECT has no direction
//                PDM.NetEventInfo.Direction direction = eventInfo.getNetInfo().getDirect();
//                if (direction == PDM.NetEventInfo.Direction.in) {
//                    sourceUuid = networkToUuid(eventInfo.getNetInfo().getSip(), eventInfo.getNetInfo().getSport());
//                    sinkUuid = processUuidToUuid(eventInfo.getEHeader().getProcUUID());
//                }
//                else if (direction == PDM.NetEventInfo.Direction.out) {
//                    sourceUuid = networkToUuid(eventInfo.getNetInfo().getDip(), eventInfo.getNetInfo().getDport());
//                    sinkUuid = processUuidToUuid(eventInfo.getEHeader().getProcUUID());
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
