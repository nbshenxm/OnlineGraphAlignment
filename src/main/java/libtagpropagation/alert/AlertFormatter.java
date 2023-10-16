package libtagpropagation.alert;

import com.google.gson.Gson;

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
        Gson fullAlertJson = new Gson();
        return fullAlertJson.toJson(this);
    }
}
