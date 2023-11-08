package libtagpropagation.anomalypath;

import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.AssociatedEvent;
import provenancegraph.NodeProperties;

import java.util.ArrayList;
import java.util.Properties;

public class AnomalyScoreTagCache  extends GenericTagCache{

    private static double ATTENUATION_THRESHOLD = 0.4;
    private static final double ATTENUATION_THRESHOLD_REGULAR_SCORE = 0.2;

    private static Long DECAY_TIME_THRESHOLD = 10 * 60 * 1000000L; // 1000 Second

    private static double ALPHA = 3;
    private static double ALERT_THRESHOLD = 0.999;
    private static final double ALERT_DISTANCE_THRESHOLD = 4;

    public static int currentTagCount = 0;
    public static int totalTagCount = 0;
    public static int initialTagCount = 0;
    public static int propagationCount = 0;
    public static int attenuatedTagCount = 0;
    public static int decayedTagCount = 0;
    public static int totalAlertCount = 0;

    private double regularScore;
    private Double anomalyScore;
    private double alpha;

    private int propagateDistance;
    private Long tagInitializedTime;

    public ArrayList<Tuple3<AssociatedEvent, Double, Long>> anomalyPath;
    public NodeProperties LastSourceNode; // to avoid loop, sink node should not equal to last source node.

    public AnomalyScoreTagCache(AssociatedEvent event, double regularScore) {
        this(event, regularScore, ALPHA);
    }

    public AnomalyScoreTagCache(AssociatedEvent event, double regularScore, double alpha) {
        setRegularScore(regularScore);
        setAlpha(alpha);
        this.propagateDistance = 1;
        this.anomalyPath = new ArrayList<>();
        this.anomalyPath.add(new Tuple3<>(event, regularScore, 0L));
        this.LastSourceNode = event.sourceNodeProperties;

        this.tagInitializedTime = event.timeStamp;

        totalTagCount += 1;
        initialTagCount += 1;
    }

    public AnomalyScoreTagCache(AnomalyScoreTagCache original) {
        this.regularScore = original.regularScore;
        this.anomalyScore = original.anomalyScore;
        this.alpha = original.alpha;
        this.propagateDistance = original.propagateDistance + 1;
        this.anomalyPath = new ArrayList<>(original.anomalyPath);

        this.tagInitializedTime = original.tagInitializedTime;

        totalTagCount += 1;
        propagationCount += 1;
    }

    public AnomalyScoreTagCache propagate(AssociatedEvent event, double edgeRegularScore) {
        AnomalyScoreTagCache newTag = new AnomalyScoreTagCache(this);
        newTag.setRegularScore(this.regularScore * edgeRegularScore * alpha);
        newTag.tagInitializedTime = event.timeStamp;
        newTag.anomalyPath.add(new Tuple3<>(event, edgeRegularScore, newTag.tagInitializedTime - this.tagInitializedTime));
        newTag.LastSourceNode = event.sourceNodeProperties;
        return newTag;
    }

    public boolean shouldReplaceTag(AnomalyScoreTagCache newTag) {
        if (newTag.getAnomalyScore() >= this.getAnomalyScore())
            return true;
        else return false;
    }

    public static void setProperties(Properties properties) {
        AnomalyScoreTagCache.ATTENUATION_THRESHOLD = Double.parseDouble(properties.getProperty("AnomalyScoreTag.ATTENUATION_THRESHOLD", "0.4"));
        AnomalyScoreTagCache.DECAY_TIME_THRESHOLD = Long.parseLong(properties.getProperty("AnomalyScoreTag.DECAY_TIME_THRESHOLD", "1800000000"));
        AnomalyScoreTagCache.ALPHA = Double.parseDouble(properties.getProperty("AnomalyScoreTag.ALPHA", "0.4"));
        AnomalyScoreTagCache.ALERT_THRESHOLD = Double.parseDouble(properties.getProperty("AnomalyScoreTag.ALERT_THRESHOLD", "0.99"));
    }

//    public AnomalyScoreTagCache mergeTag(AnomalyScoreTagCache newTag) {
//        if(this.anomalyScore >= newTag.anomalyScore)
//            return new AnomalyScoreTagCache(this);
//        else
//            return newTag;
//    }

    public boolean shouldDecayed(Long currentTime) {
        Long timeWindow = currentTime - tagInitializedTime;
        return timeWindow > DECAY_TIME_THRESHOLD;
    }

    public boolean shouldAttenuated() {
        return anomalyScore <= ATTENUATION_THRESHOLD;
    }

    public boolean shouldTriggerAlert() {
        return this.getAnomalyScore() >= ALERT_THRESHOLD;
    }

    public AnomalyScoreTagCache triggerAlert() {
        totalAlertCount += 1;
        return this;
    }

    public double getAnomalyScore() {
        this.anomalyScore = 1 - this.regularScore;
        return this.anomalyScore;
    }

    public void setRegularScore(double regularScore) {
        this.regularScore = regularScore;
    }

    public double getRegularScore() {
        return regularScore;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public Long getTagInitializedTime() {
        return tagInitializedTime;
    }

    @Override
    public String toString() {
        String output = "Path Anomaly Score: " + this.getAnomalyScore() + "\n-------\n";
        for (Tuple3<AssociatedEvent, Double, Long> step: anomalyPath)
            output = output.concat(String.format("%s: %s: %s\n", step.f0.toString(), step.f1.toString(), step.f2.toString()));
        return output;
    }
}

