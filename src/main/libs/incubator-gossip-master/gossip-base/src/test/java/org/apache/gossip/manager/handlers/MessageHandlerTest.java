/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.manager.handlers;

import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.model.ActiveGossipMessage;
import org.apache.gossip.model.Base;
import org.apache.gossip.udp.UdpSharedDataMessage;
import org.junit.Assert;
import org.junit.Test;

public class MessageHandlerTest {
  private class FakeMessage extends Base {
    public FakeMessage() {
    }
  }

  private class FakeMessageData extends Base {
    public int data;

    public FakeMessageData(int data) {
      this.data = data;
    }
  }

  private class FakeMessageDataHandler implements MessageHandler {
    public int data;

    public FakeMessageDataHandler() {
      data = 0;
    }

    public boolean invoke(GossipCore gossipCore, GossipManager gossipManager, Base base) {
      data = ((FakeMessageData) base).data;
      return true;
    }
  }

  private class FakeMessageHandler implements MessageHandler {
    public int counter;

    public FakeMessageHandler() {
      counter = 0;
    }

    public boolean invoke(GossipCore gossipCore, GossipManager gossipManager, Base base) {
      counter++;
      return true;
    }
  }
      
  @Test
  public void testSimpleHandler() {
    MessageHandler mi = new TypedMessageHandler(FakeMessage.class, new FakeMessageHandler());
    Assert.assertTrue(mi.invoke(null, null, new FakeMessage()));
    Assert.assertFalse(mi.invoke(null, null, new ActiveGossipMessage()));
  }

  @Test(expected = NullPointerException.class)
  public void testSimpleHandlerNullClassConstructor() {
    new TypedMessageHandler(null, new FakeMessageHandler());
  }

  @Test(expected = NullPointerException.class)
  public void testSimpleHandlerNullHandlerConstructor() {
    new TypedMessageHandler(FakeMessage.class, null);
  }

  @Test
  public void testCallCountSimpleHandler() {
    FakeMessageHandler h = new FakeMessageHandler();
    MessageHandler mi = new TypedMessageHandler(FakeMessage.class, h);
    mi.invoke(null, null, new FakeMessage());
    Assert.assertEquals(1, h.counter);
    mi.invoke(null, null, new ActiveGossipMessage());
    Assert.assertEquals(1, h.counter);
    mi.invoke(null, null, new FakeMessage());
    Assert.assertEquals(2, h.counter);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("all")
  public void cantAddNullHandler() {
    MessageHandler handler = MessageHandlerFactory.concurrentHandler(null);
  }
  
  @Test(expected = NullPointerException.class)
  public void cantAddNullHandler2() {
    MessageHandlerFactory.concurrentHandler(
        new TypedMessageHandler(FakeMessage.class, new FakeMessageHandler()),
        null,
        new TypedMessageHandler(FakeMessage.class, new FakeMessageHandler())
    );
  }

  @Test
  public void testMessageHandlerCombiner() {
    //Empty combiner - false result
    MessageHandler mi = MessageHandlerFactory.concurrentHandler();
    Assert.assertFalse(mi.invoke(null, null, new Base()));

    FakeMessageHandler h = new FakeMessageHandler();
    mi = MessageHandlerFactory.concurrentHandler(
      new TypedMessageHandler(FakeMessage.class, h),
      new TypedMessageHandler(FakeMessage.class, h)
    );

    Assert.assertTrue(mi.invoke(null, null, new FakeMessage()));
    Assert.assertFalse(mi.invoke(null, null, new ActiveGossipMessage()));
    Assert.assertEquals(2, h.counter);
    
    //Increase size in runtime. Should be 3 calls: 2+3 = 5
    mi = MessageHandlerFactory.concurrentHandler(mi, new TypedMessageHandler(FakeMessage.class, h));
    Assert.assertTrue(mi.invoke(null, null, new FakeMessage()));
    Assert.assertEquals(5, h.counter);
  }

  @Test
  public void testMessageHandlerCombiner2levels() {
    FakeMessageHandler h = new FakeMessageHandler();

    MessageHandler mi1 = MessageHandlerFactory.concurrentHandler(
      new TypedMessageHandler(FakeMessage.class, h),
      new TypedMessageHandler(FakeMessage.class, h)
    );

    MessageHandler mi2 = MessageHandlerFactory.concurrentHandler(
      new TypedMessageHandler(FakeMessage.class, h),
      new TypedMessageHandler(FakeMessage.class, h)
    );

    MessageHandler mi = MessageHandlerFactory.concurrentHandler(mi1, mi2);
    
    Assert.assertTrue(mi.invoke(null, null, new FakeMessage()));
    Assert.assertEquals(4, h.counter);
  }

  @Test
  public void testMessageHandlerCombinerDataShipping() {
    MessageHandler mi = MessageHandlerFactory.concurrentHandler();
    FakeMessageDataHandler h = new FakeMessageDataHandler();
    mi = MessageHandlerFactory.concurrentHandler(mi, new TypedMessageHandler(FakeMessageData.class, h));

    Assert.assertTrue(mi.invoke(null, null, new FakeMessageData(101)));
    Assert.assertEquals(101, h.data);
  }

  @Test
  public void testCombiningDefaultHandler() {
    MessageHandler mi = MessageHandlerFactory.concurrentHandler(
      MessageHandlerFactory.defaultHandler(),
      new TypedMessageHandler(FakeMessage.class, new FakeMessageHandler())
    );
    //UdpSharedGossipDataMessage with null gossipCore -> exception
    boolean thrown = false;
    try {
      mi.invoke(null, null, new UdpSharedDataMessage());
    } catch (NullPointerException e) {
      thrown = true;
    }
    Assert.assertTrue(thrown);
    //skips FakeMessage and FakeHandler works ok
    Assert.assertTrue(mi.invoke(null, null, new FakeMessage()));
  }

}
