package com.wolfesoftware.mipsos.common;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public final class Util
{
    private Util()
    {
    }

    public static LinkedList<String> arrayToLinkedList(String[] args)
    {
        LinkedList<String> argList = new LinkedList<String>();
        for (String arg : args)
            argList.add(arg);
        return argList;
    }

    public static <T> T take(LinkedBlockingQueue<T> queue)
    {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T> void put(LinkedBlockingQueue<T> queue, T item)
    {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
