package server.remote;

import java.util.Set;

public abstract class Worker implements IWorker {
    protected final String name;
    protected final String host;

    public Worker(String name, String host) {
        this.name = name;
        this.host = host;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHost() {
        return host;
    }
}
