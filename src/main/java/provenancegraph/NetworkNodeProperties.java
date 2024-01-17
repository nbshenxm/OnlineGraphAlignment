package provenancegraph;

import java.util.Objects;


public class NetworkNodeProperties extends NodeProperties {
    private String remoteIp;
    private String remotePort;

    private int direction; // 1 - in, 2 - out, 3 - none

    public NetworkNodeProperties(String remoteIp, String remotePort, int direction) {
        this.type = NodeType.Network;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.direction = direction;
    }

    public String getRemoteIp() {
        return this.remoteIp;
    }
    public String getRemotePort() {return this.remotePort;}
    @Override
    public String toString() {
        if (direction == 1)
            return String.format("[Network Connect: %s:%s->Localhost]", this.remoteIp, this.remotePort);
        else if (direction == 2)
            return String.format("[Network Connect: Localhost->%s:%s]", this.remoteIp, this.remotePort);
        else
            return String.format("[Network Connect: Localhost--%s:%s]", this.remoteIp, this.remotePort);
    }

    @Override
    public boolean haveSameProperties(NodeProperties np) {
        NetworkNodeProperties that = (NetworkNodeProperties) np;
        return remoteIp.equals(that.remoteIp)
                && remotePort.equals(that.remotePort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), remoteIp, remotePort);
    }

    public NetworkNodeProperties generalize(Boolean income) {
        return this;
    }

    @Override
    public NetworkNodeProperties copyGeneralize() {
        return new NetworkNodeProperties(
                this.remoteIp,
                this.remotePort,
                this.direction
        );
    }
}