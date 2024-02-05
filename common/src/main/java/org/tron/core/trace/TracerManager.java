package org.tron.core.trace;

import lombok.Getter;
import org.tron.common.parameter.CommonParameter;

public class TracerManager {
    @Getter
    private static Tracer tracer = new EmptyTracer();

    public static void init() throws Exception {
        String tracerImpl = CommonParameter.getInstance().getTracerImplementation();

        if (tracerImpl.isEmpty()){
            return;
        }

        tracer = (Tracer) Class.forName(tracerImpl).newInstance();
        String tracerConfig = CommonParameter.getInstance().getTracerConfigFile();

        tracer.init(tracerConfig);
    }

}

