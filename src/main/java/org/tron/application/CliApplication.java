package org.tron.application;

import com.google.inject.Injector;

import org.tron.peer.Peer;

public class CliApplication extends Application {

  private Peer peer;

  public CliApplication(Injector injector) {
    super(injector);
  }

  public Peer getPeer() {
    return peer;
  }

  public void setPeer(Peer peer) {
    this.peer = peer;
  }
}
