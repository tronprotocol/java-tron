package org.tron.core.net2.message;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "core.net2")
public class TMessageHandle {

    private ArrayList<Method> methods = Lists.newArrayList();

    private ExecutorService  executors;

    private static TMessageHandle msgHandle;

    public void init() {
        int n = Runtime.getRuntime().availableProcessors();
        executors = new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000));
    }

    public void handleMsg(TMessage msg){
        executors.submit(() -> {
            for(Method method : methods) {
                try {
                    logger.info("handle {},{}",method.getDeclaringClass().getName(), (msg.getData() == null ? "" : new String(msg.getData())));
                    method.invoke(method.getDeclaringClass(), msg);
                } catch (Exception e) {
                    logger.error("handle msg failed, msgType=" + msg.getMsgType(), e);
                }
            }
        });
    }

    public void regMsgHandle(Method method){
        methods.add(method);
    }

    public static TMessageHandle getInstance(){
        if (msgHandle == null){
            msgHandle = new TMessageHandle();
        }
        return msgHandle;
    }
}
