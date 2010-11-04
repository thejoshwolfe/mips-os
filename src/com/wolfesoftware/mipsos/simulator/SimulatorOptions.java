package com.wolfesoftware.mipsos.simulator;

import java.util.*;

import com.wolfesoftware.mipsos.common.Options;

public class SimulatorOptions extends Options
{
    public int pageSizeExponent = 6;
    public boolean fancyIoSupport = false;
    public Boolean debugInfo = null;

    @Override
    public void parse(LinkedList<String> args)
    {
        Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.startsWith("--page=")) {
                pageSizeExponent = Integer.parseInt(arg.substring("--page=".length()));
                iterator.remove();
            } else if (arg.equals("--fancy")) {
                fancyIoSupport = true;
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
        if (debugInfo == null)
            debugInfo = false;
    }
}
