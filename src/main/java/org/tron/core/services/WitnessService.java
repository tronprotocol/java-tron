package org.tron.core.services;

import org.tron.common.application.Service;

public class WitnessService implements Service {

    @Override
    public void init() {

    }

    @Override
    public void start() {
        GereateBlockLoop loop = new GereateBlockLoop();
        Thread Gereatethread = new Thread(loop);
        Gereatethread.start();
    }

    @Override
    public void stop() {

    }
    private void generateBlockLoop() {

    }
    private class GereateBlockLoop implements Runnable {

        @Override
        public void run() {

        }
    }

}
