package org.tron.common.logsfilter.nativequeue;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.Objects;

public class NativeMessageQueue {
    ZMQ.Context context = null;
    private ZMQ.Socket publisher = null;
    private static NativeMessageQueue instance;
    public static NativeMessageQueue getInstance() {
        if (Objects.isNull(instance)) {
            synchronized (NativeMessageQueue.class) {
                if (Objects.isNull(instance)) {
                    instance = new NativeMessageQueue();
                }
            }
        }
        return instance;
    }

    public void start(){
        context = ZMQ.context(1);
        publisher = context.socket(SocketType.PUB);
        publisher.bind("tcp://*:5555");
    }

    public void stop(){
        if (Objects.nonNull(publisher)){
            publisher.close();
        }

        if (Objects.nonNull(context)){
            context.term();
        }
    }

  public void publishTrigger(String data, String topic){
    if (Objects.isNull(publisher)) {
      return;
    }

    publisher.sendMore(topic);
    publisher.send(data);

    System.out.println("topic " + topic);
    System.out.println("trigger " + data);
  }
}
