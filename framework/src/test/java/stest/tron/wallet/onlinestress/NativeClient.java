package stest.tron.wallet.onlinestress;

import org.zeromq.ZMQ;
import zmq.ZMQ.Event;

public class NativeClient {

  /**
   * constructor.
   */
  public static void main(String[] args) {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);
    req.subscribe("blockTrigger");
    req.subscribe("transactionTrigger");
    req.subscribe("contractLogTrigger");
    req.subscribe("contractEventTrigger");
    req.subscribe("contractEventTrigger");
    req.subscribe("solidityLogTrigger");
    req.subscribe("solidityEventTrigger");
    req.monitor("inproc://reqmoniter", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(new Runnable() {
      public void run() {
        // TODO Auto-generated method stub
        while (true) {
          Event event = Event.read(moniter.base());
          System.out.println(event.event +  "  " + event.addr);
        }
      }
    }).start();
    req.connect("tcp://39.106.145.222:50070");
    req.setReceiveTimeOut(10000);
    while (true) {
      byte[] message = req.recv();
      if (message != null) {
        System.out.println("receive : " + new String(message));
      }
    }
  }
}
