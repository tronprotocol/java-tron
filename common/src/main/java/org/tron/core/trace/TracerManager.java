package org.tron.core.trace;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;

@Slf4j(topic = "tracer")
public class TracerManager {
    @Getter
    private static Tracer tracer = new EmptyTracer();

    public static void init() throws Exception {
        String tracerImpl = CommonParameter.getInstance().getTracerImplementationClass();
        String tracerConfig = CommonParameter.getInstance().getTracerConfig();

        logger.info("TracerManager init, TracerImpl: {}, tracerConfigPath: {}", tracerImpl, tracerConfig);

        if (tracerImpl.isEmpty()){
            return;
        }

        tracer = (Tracer) Class.forName(tracerImpl).newInstance();
        tracer.init(tracerConfig);
    }

}

