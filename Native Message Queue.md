# Native Message Queue Deployment

Using built-in message queue to subscribe block, transaction and contract events informations.

For more information about the native message queue you can refer to TIP28:

https://github.com/tronprotocol/TIPs/blob/master/tip-28.md

## Config.conf

Before the subscription on client side, some modifications need to be applied.

```java
native = {
  useNativeQueue = true // if true, use native message queue, else use event plugin.
  bindport = 5555 // bind port
  sendqueuelength = 1000 //max length of send queue
}
```

Firstly find `native` in `event.subscribe`. Java-tron defines 2 ways to do subscription: event plugin and native queue (default is native queue). We will use the native queue in this page, so switch `useNativeQueue` to true.

Then move to `topics`.	

```java
topics = [
  {
    triggerName = "block" // block trigger, the value can't be modified
    enable = true
    topic = "block" // plugin topic, the value could be modified
  },
  {
    triggerName = "transaction"
    enable = false
    topic = "transaction"
  },
  {
    triggerName = "contractevent"
    enable = false
    topic = "contractevent"
  },
  {
    triggerName = "contractlog"
    enable = false
    topic = "contractlog"
  }
]
```

Totally there are 4 triggers : `block`, `transaction`, `contractevent` and `contractlog`. Here we take `block` as an example, then set field `enable` to `true`.

Now we've finished the configuration before running a full node.

# Start java-tron from `FullNode.java`

To successfully get subscription messages, we need an argument when starting the FullNode.

In IntelliJ IDEA, open `Run/Debug Configurations`, add `--es` to `program arguements`.

# Start client side subscription

You can choose your favorite language to implement the event subscription.

**JAVA**

```java
try (ZContext context = new ZContext()) {
        ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
        subscriber.subscribe("blockTrigger");
        subscriber.subscribe("transactionTrigger");
        subscriber.subscribe("contractLogTrigger");
        subscriber.subscribe("contractEventTrigger");
        while (!Thread.currentThread().isInterrupted()) {
        byte[] message = subscriber.recv();
        System.out.println("receive : " + new String(message));
        }
        }
```

**Python**

```python
import zmq
import sys

context = zmq.Context()
socket = context.socket(zmq.SUB)
socket.connect("tcp://localhost:5555")
socket.setsockopt_string(zmq.SUBSCRIBE,'blockTrigger') 
socket.setsockopt_string(zmq.SUBSCRIBE,'transactionTrigger') 
socket.setsockopt_string(zmq.SUBSCRIBE,'contractLogTrigger')  
socket.setsockopt_string(zmq.SUBSCRIBE,'contractEventTrigger') 

while True:
    response = str(socket.recv(), encoding = "utf8")
    print(response)
```

 

**Nodejs**

```nodeJs
var zmq = require('zeromq')
    , sock = zmq.socket('sub');

sock.connect('tcp://127.0.0.1:5555');
sock.subscribe('blockTrigger');
sock.subscribe('transactionTrigger');
sock.subscribe('contractLogTrigger');
sock.subscribe('contractEventTrigger');

sock.on('message', function(topic, message) {
  console.log(new Buffer(topic).toString('utf8'), new Buffer(message).toString('utf8'));
});
```

 

**Go**

```go
func main() {
   subscriber, _ := zmq.NewSocket(zmq.SUB)
   defer subscriber.Close()
   subscriber.Connect("tcp://127.0.0.1:5555")
   subscriber.SetSubscribe("blockTrigger")
   subscriber.SetSubscribe("transactionTrigger")
   subscriber.SetSubscribe("contractLogTrigger")
   subscriber.SetSubscribe("contractEventTrigger")

   for {
      msg, _ := subscriber.Recv(0)
      fmt.Printf( msg);
   }
}
```

There are 4 topics to subscribe, the names are case-sensitive:

1. blockTrigger
2. transactionTrigger
3. contractLogTrigger
4. contractEventTrigger

We use `blockTrigger` in this page as an example, so just comment out other triggers.

# Running result

After each block production,  client side will be able to receive the information of each block.

###blockTrigger

```json
blockTrigger
{"timeStamp":1531453596000,"triggerName":"blockTrigger","blockNumber":520501,"blockHash":"000000000007f135e1bd4e83ff87cddec563c808abfdfa16ccdb327cdd0038a7","transactionSize":1,"latestSolidifiedBlockNumber":0,"transactionList":["82e92ebf2d075008d9818aaa399923bcce04a4c48afee52b33de2225d9d8c87f"]}
blockTrigger
{"timeStamp":1531453599000,"triggerName":"blockTrigger","blockNumber":520502,"blockHash":"000000000007f1364e7db306221bca839e5ea3874a23c2201f1f2f3322b4f61e","transactionSize":1,"latestSolidifiedBlockNumber":0,"transactionList":["47f8ebf19f5bb04cf3183f6242912eb371961920944209b80ad3eadf6a0943ba"]}
```



