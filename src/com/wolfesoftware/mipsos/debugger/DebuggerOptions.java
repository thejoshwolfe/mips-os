package com.wolfesoftware.mipsos.debugger;

import java.util.*;

import com.wolfesoftware.mipsos.common.Options;
import com.wolfesoftware.mipsos.simulator.SimulatorOptions;

public class DebuggerOptions extends Options
{
    public SimulatorOptions simulatorOptions;

    public Boolean run;
    public int breakAt = -1;
    public String stdinFile = null;

    public void parse(LinkedList<String> args)
    {
        simulatorOptions = new SimulatorOptions();
        simulatorOptions.parse(args, true);

        // none of our own args yet
        Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.equals("--run")) {
                run = true;
                iterator.remove();
            } else if (arg.equals("--no-run")) {
                run = false;
                iterator.remove();
            } else if (arg.startsWith("--break=")) {
                breakAt = Integer.parseInt(arg.substring("--break=".length()));
                iterator.remove();
            } else if (arg.startsWith("--stdin-file=")) {
                stdinFile = arg.substring("--stdin-file=".length());
                iterator.remove();
            }
        }
    }

    @Override
    public void normalize()
    {
        if (run == null)
            run = true;
    }
}
