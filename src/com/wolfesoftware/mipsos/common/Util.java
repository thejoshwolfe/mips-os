package com.wolfesoftware.mipsos.common;

import java.io.*;
import java.util.*;
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

    public static int findInList(ArrayList<Integer> list, int target)
    {
        // iterative binary search
        int left = 0, right = list.size() - 1, mid;
        while (left < right) {
            mid = (left + right + 1) >> 1; // midpoint rounded up
            if (target < list.get(mid))
                right = mid - 1;
            else
                left = mid;
        }
        return left;
    }

    public static String readFile(String filename)
    {
        try {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[0x1000];
            FileReader reader = new FileReader(filename);
            while (true) {
                int read = reader.read(buffer);
                if (read == -1)
                    break;
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String[] readLines(String inputPath)
    {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner scanner;
        try {
            scanner = new Scanner(new File(inputPath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            while (scanner.hasNextLine())
                lines.add(scanner.nextLine());
            return lines.toArray(new String[lines.size()]);
        } finally {
            scanner.close();
        }
    }

    public static String zfill(String string, int length)
    {
        return rjust(string, length, '0');
    }
    public static String rjust(String string, int length)
    {
        return rjust(string, length, ' ');
    }
    public static String rjust(String string, int length, char pad)
    {
        if (string.length() >= length)
            return string;
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length - string.length(); i++)
            stringBuilder.append(pad);
        stringBuilder.append(string);
        return stringBuilder.toString();
    }

    public static <T> T[] varargs(T... array)
    {
        return array;
    }

    public static String addressToString(int address)
    {
        return "0x" + zfill(Integer.toHexString(address), 8);
    }

    public static String toString(Iterable<Character> chars)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars)
            stringBuilder.append(c);
        return stringBuilder.toString();
    }

    public static int parseInt(String string)
    {
        if (string.startsWith("0x"))
            return Integer.parseInt(string.substring("0x".length()), 16);
        return Integer.parseInt(string);
    }
    public static int linearSearch(String[] array, String item)
    {
        for (int i = 0; i < array.length; i++)
            if (item.equals(array[i]))
                return i;
        return -1;
    }
}
