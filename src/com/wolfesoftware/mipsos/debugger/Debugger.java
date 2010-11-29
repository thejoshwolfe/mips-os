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

        // init the simualtor core (provide the listener later)
        SimulatorCore simulatorCore = new SimulatorCore(debuggerOptions.simulatorOptions, null);
        simulatorCore.loadBinary(binary);

        // init the debugger (including setting the listener)
        Debugger debugger = new Debugger(simulatorCore);
        if (debuggerOptions.breakAt != -1)
            debugger.setBreakpoint(debuggerOptions.breakAt);

        // maybe start
        if (debuggerOptions.run)
            debugger.go();

        // pass control to the debugger's terminal interface
        debugger.cliMain();
    }

    private final SimulatorCore simulatorCore;
    private final BlockingEvent needUserActionEvent = new BlockingEvent(true);
    private final Thread simulatorThread;
    private final LinkedBlockingQueue<Runnable> simulatorActions = new LinkedBlockingQueue<Runnable>();
    private final LinkedBlockingQueue<Character> stdinQueue = new LinkedBlockingQueue<Character>();
    private final HashSet<Integer> breakpoints = new HashSet<Integer>();

    public Debugger(SimulatorCore simulatorCore)
    {
        this.simulatorCore = simulatorCore;
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

    public String[] wideList(int listRadius)
    {
        return new String[] { "TODO: wide list" };
        // TODO
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
        // TODO: translate line number to address
        breakpoints.add(lineNumber);
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

                switch (simulatorCore.getStatus())
                {
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
            for (String line : wideList(listRadius))
                System.out.println(line);
        }
    }
}
