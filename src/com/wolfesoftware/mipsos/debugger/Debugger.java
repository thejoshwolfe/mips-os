package com.wolfesoftware.mipsos.debugger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.common.*;
import com.wolfesoftware.mipsos.simulator.*;

public class Debugger
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        // parse the args
        LinkedList<String> argList = Util.arrayToLinkedList(args);
        DebuggerOptions debuggerOptions = new DebuggerOptions();
        debuggerOptions.parse(argList);
        debuggerOptions.normalize();

        if (argList.size() != 1)
            throw new RuntimeException();
        String inputPath = argList.getFirst();

        // assemble source
        ExecutableBinary binary = Assembler.assembleToBinary(inputPath, debuggerOptions.simulatorOptions.assemblerOptions);
        // grab the debug info
        DebugInfo debugInfo = null;
        for (Segment segment : binary.segments) {
            if (Arrays.equals(segment.attributes.get(Segment.ATTRIBUTE_TYPE), Segment.TYPE_DEBUGINFO)) {
                debugInfo = DebugInfo.fromSegment(segment);
                break;
            }
        }

        // init the simualtor core (provide the listener later)
        SimulatorCore simulatorCore = new SimulatorCore(debuggerOptions.simulatorOptions, null);
        simulatorCore.loadBinary(binary);

        // init the debugger (including setting the listener)
        Debugger debugger = new Debugger(simulatorCore, debugInfo);
        if (debuggerOptions.breakAt != -1)
            debugger.setBreakpoint(debuggerOptions.breakAt);

        // maybe start
        if (debuggerOptions.run)
            debugger.go();

        // pass control to the debugger's terminal interface
        debugger.cliMain();
    }

    private final SimulatorCore simulatorCore;
    private final DebugInfo debugInfo;
    private final String[] sourceLines;
    private final BlockingEvent needUserActionEvent = new BlockingEvent(true);
    private final Thread simulatorThread;
    private final LinkedBlockingQueue<Runnable> simulatorActions = new LinkedBlockingQueue<Runnable>();
    private final LinkedBlockingQueue<Character> stdinQueue = new LinkedBlockingQueue<Character>();
    private final HashSet<Integer> breakpoints = new HashSet<Integer>();

    public Debugger(SimulatorCore simulatorCore, DebugInfo debugInfo)
    {
        this.simulatorCore = simulatorCore;
        this.debugInfo = debugInfo;
        this.sourceLines = Util.readLines(debugInfo.inputPath);
        simulatorCore.listener = new ISimulatorListener() {
            @Override
            public char readCharacter()
            {
                return internalReadCharacter();
            }
            @Override
            public void printCharacter(char c)
            {
                internalPrintCharacter(c);
            }
        };
        simulatorThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                simulatorThreadMain();
            }
        });
        simulatorThread.start();
    }

    private void cliMain()
    {
        new Cli().main();
    }
    private void simulatorThreadMain()
    {
        while (true) {
            Runnable action = Util.take(simulatorActions);
            if (action == null)
                break;
            action.run();
        }
    }

    public String[] list(int listRadius, String defaultPrefix, String currentPrefix)
    {
        int address = simulatorCore.getPc();
        int lineNumber;
        try {
            lineNumber = debugInfo.addressToLine(address);
        } catch (IllegalArgumentException e) {
            return new String[] { "[no source. " + addressToString(address) + "]" };
        }
        ArrayList<String> lines = new ArrayList<String>(listRadius);
        for (int i = lineNumber - listRadius; i <= lineNumber + listRadius; i++) {
            if (!(0 <= i && i < sourceLines.length))
                continue; // out of bounds
            String prefix = i == lineNumber ? currentPrefix : defaultPrefix;
            lines.add(prefix + sourceLines[i]);
        }
        return lines.toArray(new String[lines.size()]);
    }

    private static String addressToString(int address)
    {
        return "0x" + Util.zfill(Integer.toHexString(address), 8);
    }

    public void step()
    {
        needUserActionEvent.clear();

        Util.put(simulatorActions, new Runnable() {
            @Override
            public void run()
            {
                simulatorCore.step();

                needUserActionEvent.set();
            }
        });
    }

    public void go()
    {
        needUserActionEvent.clear();

        Util.put(simulatorActions, new Runnable() {
            @Override
            public void run()
            {
                while (true) {
                    SimulatorStatus simulatorStatus = simulatorCore.step();
                    if (simulatorStatus != SimulatorStatus.Ready)
                        break;
                    if (breakpoints.contains(simulatorCore.getPc()))
                        break;
                }

                needUserActionEvent.set();
            }
        });
    }

    public void input(String string)
    {
        for (char c : string.toCharArray()) {
            Util.put(stdinQueue, c);
            needUserActionEvent.clear();
        }
    }

    public void setBreakpoint(int lineNumber)
    {
        breakpoints.add(debugInfo.lineToAddress(lineNumber));
    }

    private char internalReadCharacter()
    {
        if (stdinQueue.isEmpty())
            needUserActionEvent.set();
        return Util.take(stdinQueue);
    }
    private void internalPrintCharacter(char c)
    {
        System.out.print(c);
    }
    private class Cli
    {
        private CliSettings settings = new CliSettings();
        public void main()
        {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                needUserActionEvent.waitForIt();

                switch (simulatorCore.getStatus()) {
                    case Stdin:
                        System.out.println("* Blocking on stdin");
                        break;
                    case Done:
                        System.out.println("* Done");
                        break;
                }

                System.out.print(">>> ");

                if (!scanner.hasNextLine())
                    break;
                String line = scanner.nextLine();

                for (String statement : line.split(";")) {
                    String[] parts = statement.trim().split(" ", 2);
                    String commandString = parts[0];
                    DebuggerCommand command = DebuggerCommand.fromName(commandString);
                    if (command == null) {
                        System.err.println("Bad Command: " + commandString);
                        continue;
                    }

                    switch (command) {
                        case GO:
                            go();
                            break;
                        case INPUT:
                            input(parts[1] + "\n");
                            break;
                        case LIST:
                            wideListToStdout(settings.listRadius);
                            break;
                        case STEP:
                            step();
                            break;
                        default:
                            throw null;
                    }
                }
            }

            Util.put(simulatorActions, null);
        }
        private void wideListToStdout(int listRadius)
        {
            for (String line : list(listRadius, "  ", "->"))
                System.out.println(line);
        }
    }
}
