package stest.tron.wallet.onlinestress;

import org.zeromq.ZMQ;
import zmq.ZMQ.Event;

public class NativeClient {
  public static void main (String args[]) {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);
    req.subscribe("blockTrigger");
    req.subscribe("transactionTrigger");
    req.subscribe("contractLogTrigger");
    req.subscribe("contractEventTrigger");
    req.subscribe("contractEventTrigger");
    req.subscribe("solidityLogTrigger");
    req.subscribe("solidityEventTrigger");
    req.monitor("inproc://reqmoniter", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);  //这段代码会创建一个pair类型的socket，专门来接收当前socket发生的事件
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);   //这里创建一个pair类型的socket，用于与上面建立的moniter建立连接
    moniter.connect("inproc://reqmoniter");  //连接当前socket的监听
    new Thread(new Runnable(){
      public void run() {
        // TODO Auto-generated method stub
        while (true) {
          Event event = Event.read(moniter.base());  //从当前moniter里面读取event
          System.out.println(event.event +  "  " + event.addr);
        }
      }
    }).start();
    req.connect("tcp://39.106.145.222:50070");
    req.setReceiveTimeOut(10000);
    while (true) {
      byte[] message = req.recv();
      if (message != null)
        System.out.println("receive : " + new String(message));
    }
  }
}
