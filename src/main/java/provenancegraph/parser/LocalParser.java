package provenancegraph.parser;

import com.google.gson.JsonElement;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.math3.ml.neuralnet.Network;
import provenancegraph.*;

import java.math.BigInteger;
import java.util.UUID;


// First 32 digits of uuid for host uuid
// Argument is in wrong order for each log

public class LocalParser {
    private static String getJsonField(String field, JsonElement jsonElement){
        return jsonElement.getAsJsonObject().get(field)
                .getAsJsonPrimitive().getAsString();
    }

    private static String getJsonArgField(String field, JsonElement jsonElement){
        return jsonElement.getAsJsonObject().get("arguments")
                .getAsJsonObject().get(field)
                .getAsJsonPrimitive().getAsString();
    }


    private static int getLastDashOrNum(String s){
        boolean isNum = true;
        int loc = 0;
        while(isNum){
            try{
                String curr = s.substring(loc, loc + 1);
                if(! curr.equals("-")){
                    Integer.parseInt(curr);

                }
                loc += 1;
            }
            catch(NumberFormatException nfe){
                isNum = false;
            }
        }
        return loc;
    }
    public static BasicNode initBasicSourceNode(JsonElement jsonElement){
        String processId;
        if(getJsonField("log_category", jsonElement).equals("Process") && ! getJsonField("event_type", jsonElement).equals("38")){
            processId = getJsonArgField("parent_process_uuid", jsonElement);
        }
        else{
            processId = getJsonArgField("process_uuid", jsonElement);
        }

        int lastNum = getLastDashOrNum(processId);

//        System.out.println(processId.substring(0, lastNum));
//        System.out.println(processId);
        String idTemp = processId.substring(0, lastNum).replace("-", "");
        UUID id = new UUID(
                new BigInteger(idTemp.substring(0, 16), 16).longValue(),
                new BigInteger(idTemp.substring(16), 16).longValue());

        return new BasicNode(id, "Process", getJsonField("log_name", jsonElement));

    }
    //initialize a sink node, given that it's a specific event type
    public static BasicNode initBasicSinkNode(JsonElement jsonElement){
        String nodeType = getJsonField("log_category", jsonElement);
        String processId;
        switch(getJsonField("log_category", jsonElement)){
            case "Process":
                if(! getJsonField("event_type", jsonElement).equals("38")) processId = getJsonArgField("process_uuid", jsonElement);
                else processId = "";
                int lastNum = getLastDashOrNum(processId);
                processId = processId.substring(0, lastNum);
                break;
            case "File":
                processId = getJsonArgField("file_uuid", jsonElement);
                break;
            case "Network":
                //unsure so far
                processId = getJsonArgField("process_timestamp", jsonElement) + getJsonArgField("destination_ip", jsonElement);
                break;
            default:
                processId = "";
        }

//        System.out.println(processId);
        String idTemp = processId.replace("-", "");
        idTemp = idTemp.replace(".", "");
        if(idTemp.length() < 32){
            String zero = "0";
            idTemp += new String(new char[32 - idTemp.length()]).replace("\0", "0");;
        }
//        System.out.println(idTemp);
        UUID id = new UUID(
                new BigInteger(idTemp.substring(0, 16), 16).longValue(),
                new BigInteger(idTemp.substring(16), 16).longValue());

        return new BasicNode(id, nodeType, getJsonField("log_name", jsonElement));

    }

    public static NodeProperties initSourceNodeProperties(JsonElement jsonElement){
        String nodeType = getJsonField("log_category", jsonElement);
        NodeProperties n;
        switch(nodeType){
            case "File":
            case "Network":
//                System.out.println(jsonElement.toString());
                String cmd;
                try {
                    cmd = getJsonArgField("process_commandline", jsonElement);
                }
                catch(NullPointerException e){
                    cmd = getJsonArgField("process_path", jsonElement);
                }
                n = new ProcessNodeProperties(Integer.parseInt(getJsonArgField("process_id", jsonElement)), getJsonArgField("process_path", jsonElement), cmd);
                break;
            case "Process":
                n = new ProcessNodeProperties(Integer.parseInt(getJsonArgField("parent_process_id", jsonElement)), getJsonArgField("parent_process_path", jsonElement), getJsonArgField("parent_process_commandline", jsonElement));
                break;
            default:
                n = null;
        }
        return n;
    }
    public static NodeProperties initSinkNodeProperties(JsonElement jsonElement){
        String nodeType = getJsonField("log_category", jsonElement);
        NodeProperties n;
        switch(nodeType){

            case "File":
                n = new FileNodeProperties(getJsonArgField("filepath", jsonElement));
                break;
            case "Network":
                n = new NetworkNodeProperties(getJsonArgField("destination_ip", jsonElement), getJsonArgField("destination_port", jsonElement), Integer.parseInt(getJsonArgField("direction", jsonElement)));
                break;
            case "Process":
                n = new ProcessNodeProperties(Integer.parseInt(getJsonArgField("process_id", jsonElement)), getJsonArgField("process_path", jsonElement), getJsonArgField("process_commandline", jsonElement));
                break;
            default:
                n = null;
        }

        return n;
    }


    public static AssociatedEvent initAssociatedEvent(JsonElement jsonElement){
        String eventTypeNum;
        String eventTypeName;
        String id = getJsonField("uuid", jsonElement);
//        System.out.println("uuid: " + id);
//        System.out.println(jsonElement.toString());
        UUID hostUUID = new UUID(
                new BigInteger(id.substring(0, 16), 16).longValue(),
                new BigInteger(id.substring(16, 32), 16).longValue());
//        System.out.println(hostUUID.toString());
        AssociatedEvent event = new AssociatedEvent();
        event.setHostUUID(hostUUID);
        event.setTimeStamp(Long.parseLong(getJsonField("timestamp", jsonElement)));
        //hostUUID, eventTypeName, Long.parseLong(getJsonField("timestamp", jsonElement))
        try {
            getJsonField("event_type", jsonElement);
        }
        catch(Exception e){
            System.out.println("what did u do david");
            System.out.println(jsonElement.toString());
        }
        if(getJsonField("log_category", jsonElement).equals("Domain")){
            //temp for fill
            return null;
        }
        switch(getJsonField("event_type", jsonElement)){
            case "12":
            case "13":
                eventTypeNum = "3";
                eventTypeName = "PROCESS_FORK";
                break;
            case "14":
                eventTypeNum = "4";
                eventTypeName = "PROCESS_EXEC";
                break;
            case "35":
                eventTypeNum = "5";
                eventTypeName = "PROCESS_LOAD";
                break;
            case "1":
            case "2":
                eventTypeNum = "6";
                eventTypeName = "FILE_OPEN";
                break;
            case "4":
            case "5":
                eventTypeNum = "7";
                eventTypeName = "FILE_READ";
                break;
            case "8":
            case "9":
            case "10":
            case "11":
            case "22":
            case "23":
                eventTypeNum = "8";
                eventTypeName = "FILE_WRITE";
                break;
            case "16":
                eventTypeNum = "9";
                eventTypeName = "NET_CONNECT";
                break;
            default:
                eventTypeNum = "6";
                eventTypeName = "FILE_OPEN";
                break;
        }

        event.setRelationship(eventTypeName);
        BasicNode sink = initBasicSinkNode(jsonElement);
//        System.out.println("tf");
//        System.out.println(jsonElement.toString());
        BasicNode source = initBasicSourceNode(jsonElement);
        event.setSinkNode(sink);
        event.setSourceNode(source);
//        System.out.println(source.toString());
//        System.out.println(sink.toString());
//        System.out.println("Node UUID: " + sink.getNodeId());
//        System.out.println("Node Name: " + sink.getNodeName());
//        System.out.println("Node Type: " + sink.getNodeType());
//        System.out.println(jsonElement.toString());
//        NodeProperties sinkProperties = initNodeProperties(jsonElement, sink.getNodeType());
        sink.setProperties(initSinkNodeProperties(jsonElement));
        source.setProperties(initSourceNodeProperties(jsonElement));
        System.out.println(jsonElement.toString());
        System.out.println(event.toJsonString());
        return event;
    }
}
