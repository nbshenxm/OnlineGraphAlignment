package alert;

import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.AssociatedEvent;

import java.util.UUID;

public class AlertFormatter {

    public long currentTime;
    public String str1;
    public String str2;
    public double AnomalyScore;
    public UUID uuid;
    public String ip;
    public MergedAlertGraph AlertPath;

    public AlertFormatter(long currentTime, String str1, String str2, double AnomalyScore, UUID uuid, String ip, MergedAlertGraph AlertPath)
    {
        this.currentTime = currentTime;
        this.str1 = str1;
        this.str2 = str2;
        this.AnomalyScore = AnomalyScore;
        this.uuid = uuid;
        this.ip = ip;
        this.AlertPath = AlertPath;

    }

    public String toJsonString() {
        StringBuilder fullAlertJson = new StringBuilder();
        fullAlertJson.append("###############Alert###############\ncurrentTime:" + currentTime + "\n");
        fullAlertJson.append("AnomalyScore: " + AnomalyScore + "\n");
        fullAlertJson.append("AlertPath:");
        for (Tuple3<AssociatedEvent, Double, Long> path : AlertPath.getAnomalyPath()){
            fullAlertJson.append(path.f0.toString()+": " +path.f1 + ": " +path.f2 + "\n");
        }
        return fullAlertJson.toString();
    }
}
