package com.wolfesoftware.mipsos.debugger;

public class BlockingEvent
{
    private boolean eventIsSet;
    private final Object lock = new Object();
    public BlockingEvent()
    {
        this(false);
    }
    public BlockingEvent(boolean initialState)
    {
        eventIsSet = initialState;
    }

    public void waitForIt()
    {
        synchronized (lock) {
            if (eventIsSet)
                return;
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void set()
    {
        synchronized (lock) {
            eventIsSet = true;
            lock.notifyAll();
        }
    }
    public synchronized void clear()
    {
        synchronized (lock) {
            eventIsSet = false;
        }
    }
}

