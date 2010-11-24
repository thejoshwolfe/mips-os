package com.wolfesoftware.mipsos.assembler;

import java.io.OutputStream;
import java.util.*;

import com.wolfesoftware.mipsos.common.Options;

public class AssemblerOptions extends Options
{
    public static final int DefaultDataAddress = 0x10000000;
    public static final int DefaultTextAddress = 0x00400000;

    public int dataAddress = DefaultDataAddress;
    public int textAddress = DefaultTextAddress;
    public Boolean readable = null;
    public OutputStream outStream = null;
    public Boolean debugInfo = null;

    public void parse(LinkedList<String> args)
    {
        Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.startsWith("--data=")) {
                dataAddress = parseHex(arg.substring("--data=".length()));
                iterator.remove();
            } else if (arg.startsWith("--text=")) {
                textAddress = parseHex(arg.substring("--text=".length()));
                iterator.remove();
            } else if (arg.equals("--readable")) {
                readable = true;
                iterator.remove();
            } else if (arg.equals("--binary")) {
                readable = false;
                iterator.remove();
            } else if (arg.startsWith("--output=")) {
                outStream = openOutputStream(arg.substring("--output=".length()));
                iterator.remove();
            } else if (arg.equals("--debug")) {
                debugInfo = true;
                iterator.remove();
            } else if (arg.equals("--no-debug")) {
                debugInfo = false;
                iterator.remove();
            }
        }
    }
    @Override
    public void normalize()
    {
        if (outStream == null)
            outStream = System.out;
        if (readable == null) {
            // probably don't want to print binary to stdout in the default case
            readable = outStream == System.out;
        }
        if (debugInfo == null)
            debugInfo = false;
    }
}
