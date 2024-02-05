package org.tron.core.tracing;

public class TracerManager {

    private static Tracer tracer = new EmptyTracer();


    public static Tracer getTracer() {
        return tracer;
    }

    public static void init(String implementationClass) throws Exception {
        tracer = (Tracer) Class.forName(implementationClass).newInstance();
    }

}

