package ru.mt.utils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Processor {
    private final Action action;
    private final Thread processingThread;
    private volatile boolean stop = false;

    public interface Action {
        void run() throws InterruptedException;
    }

    public Processor(String name, Action action) {
        Assert.notEmpty(name, "Processor name is empty");
        Assert.notNull(action, "Action is null");

        this.action = action;
        processingThread = new Thread(this::process, name);
    }

    public void start() {
        log.debug("starting...");
        processingThread.start();
    }

    public void stop() {
        log.debug("stopping...");
        stop = true;
        try {
            // wait 1 second until the thread finishes its work
            processingThread.join(1000);
            processingThread.interrupt();
        } catch (SecurityException | InterruptedException ignore) {
        }
        log.debug("stopping...done");
    }

    private void process() {
        while (!Thread.currentThread().isInterrupted() && !stop) {
            log.debug("process...");

            try {
                action.run();
            } catch (InterruptedException e) {
                break;
            }
        }

        log.debug("processing had interrupted");
    }
}
