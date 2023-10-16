package libtagpropagation.alert;

import org.apache.flink.api.java.tuple.Tuple3;
import provenancegraph.AssociatedEvent;

import java.util.ArrayList;

public class MergedAlertGraph {
    public ArrayList<Tuple3<AssociatedEvent, Double, Long>> anomalyPath;

    public void insertPath(ArrayList<Tuple3<AssociatedEvent, Double, Long>> anomalyPath)
    {
        this.anomalyPath = anomalyPath;
    }
}
