package com.wolfesoftware.mipsos.simulator;

import java.util.*;

import com.wolfesoftware.mipsos.assembler.AssemblerOptions;
import com.wolfesoftware.mipsos.common.Options;

public class SimulatorOptions extends Options
{
    public AssemblerOptions assemblerOptions;
    public int pageSizeExponent = 6;
    public boolean fancyIoSupport = false;

    public void parse(LinkedList<String> args, boolean forceDebug)
    {
        assemblerOptions = new AssemblerOptions();
        assemblerOptions.parse(args);
        if (assemblerOptions.outStream == System.out)
            throw new RuntimeException();
        if (Boolean.TRUE.equals(assemblerOptions.readable))
            throw new RuntimeException();
        assemblerOptions.readable = false;
        if (forceDebug) {
            if (Boolean.FALSE.equals(assemblerOptions.debugInfo))
                throw new RuntimeException();
            assemblerOptions.debugInfo = true;
        }
        assemblerOptions.normalize();

        Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.startsWith("--page=")) {
                pageSizeExponent = Integer.parseInt(arg.substring("--page=".length()));
                iterator.remove();
            } else if (arg.equals("--fancy")) {
                fancyIoSupport = true;
                iterator.remove();
            }
        }
    }
}
