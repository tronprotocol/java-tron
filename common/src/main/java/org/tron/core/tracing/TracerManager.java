package org.tron.core.tracing;

import org.tron.common.parameter.CommonParameter;

public class TracerManager {

    private static Tracer tracer = new EmptyTracer();


    public static Tracer getTracer() {
        return tracer;
    }

    public static void init() throws Exception {
        String tracerImpl = CommonParameter.getInstance().getTracerImplementation();
        if(tracerImpl.isEmpty()){
            return;
        }
        tracer = (Tracer) Class.forName(tracerImpl).newInstance();
        String tracerConfig = CommonParameter.getInstance().getTracerConfigFile();
        tracer.init(tracerConfig);
    }

}

