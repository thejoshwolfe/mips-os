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

        // init the simualtor (provide the listener later)
        Simulator simulator = new Simulator(debuggerOptions.simulatorOptions, null);
        simulator.loadBinary(binary);

        // init the debugger (including setting the listener)
        Debugger debugger = new Debugger(simulator, debugInfo);
        for (String breakAt : debuggerOptions.breakAt) {
            if (breakAt.startsWith("0x")) {
                // address
                int address = Integer.parseInt(breakAt.substring("0x".length()), 16);
                debugger.setBreakpointAtAddress(address);
            } else {
                // line number
                int lineNumber = Integer.parseInt(breakAt);
                debugger.setBreakpointAtLine(lineNumber);
            }
        }
        if (debuggerOptions.stdinFile != null)
            debugger.setStdinFile(debuggerOptions.stdinFile);

        // pass control to the debugger's terminal interface
        debugger.cliMain(debuggerOptions.run);
    }

    private final Simulator simulator;
    private final DebugInfo debugInfo;
    private final String[] sourceLines;
    private final BlockingEvent needUserActionEvent = new BlockingEvent(true);
    private boolean longOperationInProgress = true;
    private final Thread simulatorThread;
    private final LinkedBlockingQueue<Runnable> simulatorActions = new LinkedBlockingQueue<Runnable>();
    private static final Runnable STOP = new Runnable() {
        @Override
        public void run()
        {
        }
    };
    private final LinkedBlockingQueue<Character> stdinQueue = new LinkedBlockingQueue<Character>();
    private final HashSet<Integer> breakpoints = new HashSet<Integer>();
    private boolean pausing;

    public Debugger(Simulator simulator, DebugInfo debugInfo)
    {
        this.simulator = simulator;
        this.debugInfo = debugInfo;
        this.sourceLines = Util.readLines(debugInfo.inputPath);
        simulator.listener = new ISimulatorListener() {
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

    private void cliMain(boolean run)
    {
        new Cli().main(run);
    }
    private void simulatorThreadMain()
    {
        while (true) {
            Runnable action = Util.take(simulatorActions);
            if (action == STOP)
                break;
            action.run();
        }
    }

    public Listing list(int listRadius)
    {
        int address = simulator.getPc();
        int lineNumber;
        try {
            lineNumber = debugInfo.addressToLine(address);
        } catch (IllegalArgumentException e) {
            return new Listing(new String[0], -1, -1, address, simulator.getClock());
        }
        ArrayList<String> lines = new ArrayList<String>(listRadius);
        int start = Math.max(lineNumber - listRadius, 0);
        int end = Math.min(lineNumber + listRadius + 1, sourceLines.length);
        for (int i = start; i < end; i++)
            lines.add(sourceLines[i]);
        return new Listing(lines.toArray(new String[lines.size()]), start, lineNumber, address, simulator.getClock());
    }
    public Registers getRegisters()
    {
        return new Registers(simulator.getRegisters());
    }
    private Extras getExtras()
    {
        return simulator.getExtras();
    }
    private byte[] getMemory(int address, int length)
    {
        return simulator.getMemory(address, length);
    }

    public void step()
    {
        step(1);
    }
    public void step(final int count)
    {
        beginLongOperation();

        Util.put(simulatorActions, new Runnable() {
            @Override
            public void run()
            {
                for (int i = 0; i < count && !pausing; i++)
                    simulator.step();

                finishLongOperation();
            }
        });
    }

    public void go()
    {
        go(-1);
    }
    public void go(final int until)
    {
        beginLongOperation();

        Util.put(simulatorActions, new Runnable() {
            @Override
            public void run()
            {
                for (int i = simulator.getClock(); until == -1 || i < until; i++) {
                    SimulatorStatus simulatorStatus = simulator.step();
                    if (simulatorStatus != SimulatorStatus.Ready)
                        break;
                    if (breakpoints.contains(simulator.getPc()))
                        break;
                    if (pausing)
                        break;
                }

                finishLongOperation();
            }
        });
    }
    public void pause()
    {
        pausing = true;
    }

    private void beginLongOperation()
    {
        needUserActionEvent.clear();
        longOperationInProgress = true;
    }
    private void finishLongOperation()
    {
        pausing = false;
        needUserActionEvent.set();
    }

    private void setStdinFile(String stdinFile)
    {
        input(Util.readFile(stdinFile));
    }
    public void input(String string)
    {
        if (string.isEmpty())
            return;
        SimulatorStatus status = simulator.getStatus();
        for (char c : string.toCharArray())
            Util.put(stdinQueue, c);
        if (status == SimulatorStatus.Stdin) {
            // this has woken the simulator back up from being blocked
            beginLongOperation();
        }
    }
    public void setBreakpointAtLine(int lineNumber)
    {
        setBreakpointAtAddress(debugInfo.lineToAddress(lineNumber));
    }
    public void setBreakpointAtAddress(int address)
    {
        breakpoints.add(address);
    }
    private boolean isBreakpointAtLine(int lineNumber)
    {
        for (int address : breakpoints)
            if (debugInfo.addressToLine(address) == lineNumber)
                return true;
        return false;
    }


    private char internalReadCharacter()
    {
        if (stdinQueue.isEmpty()) {
            // now blocking on stdin
            finishLongOperation();
        }
        return Util.take(stdinQueue);
    }
    private void internalPrintCharacter(char c)
    {
        System.out.print(c);
    }
    private class Cli
    {
        private CliSettings settings = CliSettings.load();
        public void main(boolean run)
        {
            Scanner scanner = new Scanner(System.in);
            String lastLine = null;
            if (run)
                go();
            while (true) {
                if (longOperationInProgress) {
                    needUserActionEvent.waitForIt();
                    longOperationInProgress = false;

                    if (simulator.getStatus() == SimulatorStatus.Done)
                        break;

                    parseAndRun(settings.autoCommand);

                    if (simulator.getStatus() == SimulatorStatus.Stdin)
                        System.out.println("* Blocking on stdin");
                }

                System.out.print(">>> ");
                if (!scanner.hasNextLine())
                    break;
                String line = scanner.nextLine();
                if (lastLine != null && line.equals(""))
                    line = lastLine;
                else
                    lastLine = line;

                parseAndRun(line);
            }

            pause();
            Util.put(simulatorActions, STOP);
            System.out.println();
            Util.put(stdinQueue, '\0');
        }
        private void parseAndRun(final String line)
        {
            class Parser
            {
                private final ArrayList<String> commandBuffer = new ArrayList<String>();
                private final StringBuilder tokenBuffer = new StringBuilder();
                public void parseAndRun()
                {
                    int index = 0;
                    while (index < line.length()) {
                        char c = line.charAt(index++);
                        switch (c) {
                            case ' ':
                                flushToken();
                                break;
                            case ';':
                                flushCommand();
                                break;
                            case '"':
                                // string literal
                                while (index < line.length()) {
                                    c = line.charAt(index++);
                                    if (c == '\\' && index < line.length()) {
                                        // escape
                                        c = line.charAt(index++);
                                        switch (c) {
                                            case '\\':
                                            case '"':
                                                tokenBuffer.append(c);
                                                break;
                                            case 'n':
                                                tokenBuffer.append('\n');
                                                break;
                                            default:
                                                System.err.println("Invalid escape sequence: \\" + c);
                                                return;
                                        }
                                    } else if (c == '"') {
                                        if (tokenBuffer.length() == 0) {
                                            // special empty token
                                            commandBuffer.add("");
                                        }
                                        break;
                                    } else {
                                        tokenBuffer.append(c);
                                    }
                                }
                                break;
                            default:
                                tokenBuffer.append(c);
                                break;
                        }
                    }
                    flushCommand();
                }

                private void flushCommand()
                {
                    flushToken();
                    if (commandBuffer.isEmpty())
                        return;
                    try {
                        String commandName = commandBuffer.get(0);
                        Command command = commands.get(commandName);
                        if (command == null) {
                            System.err.println("* Bad Command: " + commandName);
                            return;
                        }
                        commandBuffer.remove(0);
                        int argLength = commandBuffer.size();
                        if (!(command.minArgs <= argLength)) {
                            System.err.println("* command " + commandName + " takes at least " + command.minArgs + " argument" + (command.minArgs != 1 ? "s" : ""));
                            return;
                        }
                        if (command.maxArgs != -1 && !(argLength <= command.maxArgs)) {
                            System.err.println("* command " + commandName + " takes at most " + command.maxArgs + " argument" + (command.maxArgs != 1 ? "s" : ""));
                            return;
                        }
                        command.run(commandBuffer.toArray(new String[commandBuffer.size()]));
                    } finally {
                        commandBuffer.clear();
                    }
                }

                private void flushToken()
                {
                    if (tokenBuffer.length() == 0)
                        return;
                    commandBuffer.add(tokenBuffer.toString());
                    tokenBuffer.delete(0, tokenBuffer.length());
                }
            }
            new Parser().parseAndRun();
        }
        private abstract class Command
        {
            public final int minArgs, maxArgs;
            protected Command()
            {
                this(0, 0);
            }
            protected Command(int minArgs, int maxArgs)
            {
                this.minArgs = minArgs;
                this.maxArgs = maxArgs;
            }
            public abstract void run(String[] args);
        }
        private HashMap<String, Command> commands = new HashMap<String, Command>();
        private void registerCommand(String[] names, Command command)
        {
            for (String name : names)
                commands.put(name, command);
        }
        {
            registerCommand(Util.varargs("", "newline"), new Command() {
                @Override
                public void run(String[] args)
                {
                    System.out.println();
                }
            });
            registerCommand(Util.varargs("auto"), new Command(0, 1) {
                @Override
                public void run(String[] args)
                {
                    if (args.length == 0) {
                        System.out.println(settings.autoCommand);
                    } else {
                        settings.autoCommand = args[0];
                    }
                }
            });
            registerCommand(Util.varargs("b", "break", "breaks", "breakpoint", "breakpoints"), new Command(0, -1) {
                @Override
                public void run(String[] args)
                {
                    if (args.length == 0) {
                        // list
                        for (int address : breakpoints) {
                            String printThis;
                            try {
                                int lineNumber = debugInfo.addressToLine(address);
                                printThis = lineNumber + " [" + Util.addressToString(address) + "]";
                            } catch (IllegalArgumentException e) {
                                printThis = "[" + Util.addressToString(address) + "]";
                            }
                            System.out.println(printThis);
                        }
                    } else if (args.length == 1 && args[0].equals("-")) {
                        // remove all
                        int count = breakpoints.size();
                        breakpoints.clear();
                        System.out.println("* all " + count + " breakpoint" + (count != 1 ? "s" : "") + " deleted");
                    } else {
                        // toggle
                        for (String arg : args) {
                            if (arg.startsWith("0x")) {
                                // address
                                int address;
                                try {
                                    address = Integer.parseInt(arg.substring("0x".length()), 16);
                                } catch (NumberFormatException e) {
                                    System.err.println(e);
                                    continue;
                                }
                                if ((address & 3) != 0) {
                                    System.err.println("* WARNING: word-aligning address: " + Util.addressToString(address));
                                    address &= ~3;
                                }
                                if (breakpoints.remove(address)) {
                                    // removed
                                    try {
                                        int lineNumber = debugInfo.addressToLine(address);
                                        System.out.println("* breakpoint deleted: " + lineNumber + " [" + Util.addressToString(address) + "]");
                                    } catch (IllegalArgumentException e) {
                                        System.out.println("* breakpoint deleted: [" + Util.addressToString(address) + "]");
                                    }
                                } else {
                                    // add
                                    breakpoints.add(address);
                                    try {
                                        int lineNumber = debugInfo.addressToLine(address);
                                        System.out.println("* breakpoint created: " + lineNumber + " [" + Util.addressToString(address) + "]");
                                    } catch (IllegalArgumentException e) {
                                        System.err.println("* WARNING: no line number for address " + Util.addressToString(address));
                                        System.out.println("* breakpoint created: [" + Util.addressToString(address) + "]");
                                    }
                                }
                            } else {
                                // line number
                                int lineNumber;
                                try {
                                    lineNumber = Integer.parseInt(arg);
                                } catch (NumberFormatException e) {
                                    System.err.println(e);
                                    continue;
                                }
                                try {
                                    int address = debugInfo.lineToAddress(lineNumber);
                                    lineNumber = debugInfo.addressToLine(address);
                                    if (breakpoints.remove(address)) {
                                        // removed
                                        System.out.println("* breakpoint deleted: " + lineNumber + " [" + Util.addressToString(address) + "]");
                                    } else {
                                        // add
                                        breakpoints.add(address);
                                        System.out.println("* breakpoint created: " + lineNumber + " [" + Util.addressToString(address) + "]");
                                    }
                                } catch (IllegalArgumentException e) {
                                    System.out.println("* ERROR: no address for line number " + lineNumber);
                                }
                            }
                        }
                    }
                }
            });
            registerCommand(Util.varargs("g", "go", "continue", "resume"), new Command(0, 1) {
                @Override
                public void run(String[] args)
                {
                    int until = -1;
                    if (args.length != 0) {
                        try {
                            until = evalInt(args[0]);
                        } catch (NumberFormatException e) {
                            System.err.println(e);
                        }
                    }
                    go(until);
                }
            });
            registerCommand(Util.varargs("h", "help"), new Command(0, -1) {
                @Override
                public void run(String[] args)
                {
                    System.out.println("* help: see the source code :/ ");
                    System.out.println("* check out Debugger.java. search for \"registerCommand\".");
                }
            });
            registerCommand(Util.varargs("i", "in", "stdin", "input"), new Command(0, 1) {
                @Override
                public void run(String[] args)
                {
                    if (args.length == 0) {
                        // print uneaten buffer
                        System.out.print(Util.toString(stdinQueue));
                    } else {
                        // add to buffer
                        input(args[0] + "\n");
                    }
                }
            });
            registerCommand(Util.varargs("l", "ls", "list"), new Command(0, 1) {
                @Override
                public void run(String[] args)
                {
                    if (args.length != 0) {
                        try {
                            settings.listRadius = Integer.parseInt(args[0]);
                        } catch (NumberFormatException e) {
                            System.err.println(e);
                        }
                    }
                    Listing listing = list(settings.listRadius);
                    for (int i = 0; i < listing.lines.length; i++) {
                        int lineNumber = i + listing.startLine;
                        boolean current = lineNumber == listing.currentLine;
                        boolean breakpoint = isBreakpointAtLine(lineNumber);
                        String prefix = current ? (breakpoint ? "@>" : "->") : (breakpoint ? "@ " : "  ");
                        System.out.println(prefix + listing.lines[i]);
                    }
                    System.out.println(listing.currentLine + " [" + Util.addressToString(listing.currentAddress) + "]  clock: " + Util.addressToString(listing.clock));
                }
            });
            registerCommand(Util.varargs("m", "memory"), new Command(1, -1) {
                private final HashMap<Integer, Integer> previousMemory = new HashMap<Integer, Integer>();
                @Override
                public void run(String[] args)
                {
                    for (String arg : args) {
                        try {
                            int length = -1;
                            int offset = 0;
                            if (arg.contains(":")) {
                                int index = arg.lastIndexOf(':');
                                String lengthString = arg.substring(index + 1);
                                arg = arg.substring(0, index);
                                length = evalInt(lengthString);
                            }
                            if (arg.contains("+")) {
                                int index = arg.lastIndexOf('+');
                                String offsetString = arg.substring(index + 1);
                                arg = arg.substring(0, index);
                                offset = evalInt(offsetString);
                            } else if (arg.contains("-")) {
                                int index = arg.lastIndexOf('-');
                                String offsetString = arg.substring(index + 1);
                                arg = arg.substring(0, index);
                                offset = -evalInt(offsetString);
                            }
                            int address;
                            try {
                                address = evalInt(arg);
                            } catch (NumberFormatException e) {
                                Long addressObject = debugInfo.labels.get(arg);
                                if (addressObject == null) {
                                    System.err.println("* ERROR: unrecognized memory address: " + arg);
                                    continue;
                                }
                                address = addressObject.intValue();
                            }
                            int startAddress = address + offset;
                            int endAddress = startAddress + length;
                            startAddress &= ~3; // round down to a word
                            endAddress = (endAddress + 3) & ~3; // round up to a word
                            length = Math.max(endAddress - startAddress, 4); // at least 1 word
                            if (length == 4) {
                                // display single word
                                byte[] memory = getMemory(startAddress, length);
                                int value = ByteUtils.readInt(memory, 0);
                                Integer previousValue = previousMemory.get(startAddress);
                                if (previousValue == null) {
                                    previousMemory.put(startAddress, value);
                                    previousValue = value;
                                }
                                System.out.println(Util.addressToString(startAddress) + " " + (value == previousValue ? " " : "*") + "[" + Util.addressToString(value) + "]");
                                continue;
                            }
                            // make a table
                            int width = 0x10;
                            startAddress &= ~(width - 1); // round down to a row
                            endAddress = (endAddress + width - 1) & ~(width - 1); // round up to a row
                            length = endAddress - startAddress;
                            byte[] memory = getMemory(startAddress, length);
                            for (int i = 0; i < length; i += 4) {
                                address = startAddress + i;
                                if ((i & (width - 1)) == 0)
                                    System.out.print(Util.addressToString(address));
                                int value = ByteUtils.readInt(memory, i);
                                Integer previousValue = previousMemory.get(address);
                                if (previousValue == null) {
                                    previousMemory.put(address, value);
                                    previousValue = value;
                                }
                                System.out.print(" " + (value == previousValue ? " " : "*") + "[" + Util.addressToString(value) + "]");
                                if ((i & width - 1) == width - 4)
                                    System.out.println();
                                previousMemory.put(address, value);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println(e);
                        }
                    }
                }
            });
            registerCommand(Util.varargs("pause"), new Command() {
                @Override
                public void run(String[] args)
                {
                    pause();
                }
            });
            registerCommand(Util.varargs("r", "reg", "registers"), new Command(0, 1) {
                private Registers previousRegisters = null;
                @Override
                public void run(String[] args)
                {
                    Registers registers = getRegisters();
                    if (previousRegisters == null)
                        previousRegisters = registers; // avoid npe's
                    if (args.length == 1) {
                        if (args[0].equals("4"))
                            settings.reigsterDisplayWidth = 4;
                        else if (args[0].equals("8"))
                            settings.reigsterDisplayWidth = 8;
                        else
                            System.err.println("* ERROR: only reigster display widths of 4 and 8 are supported");
                    }
                    int width = settings.reigsterDisplayWidth;
                    int height = 32 / width;
                    for (int i = 0; i < registers.values.length; i++) {
                        int r = i / width + i % width * height;
                        System.out.print(Util.rjust(Registers.NAMES[r], 5));
                        System.out.print((registers.values[r] == previousRegisters.values[r] ? " " : "*") + "[" + Util.addressToString(registers.values[r]) + "] ");
                        if ((i + 1) % width == 0)
                            System.out.println();
                    }
                    previousRegisters = registers;
                }
            });
            registerCommand(Util.varargs("s", "step"), new Command(0, 1) {
                @Override
                public void run(String[] args)
                {
                    int count = 1;
                    if (args.length != 0) {
                        try {
                            count = evalInt(args[0]);
                        } catch (NumberFormatException e) {
                            System.err.println(e);
                        }
                    }
                    step(count);
                }
            });
            registerCommand(Util.varargs("x", "extra", "extras"), new Command(0, 1) {
                private Extras previousExtras = null;
                @Override
                public void run(String[] args)
                {
                    if (args.length != 0) {
                        if (args[0].equals("4"))
                            settings.reigsterDisplayWidth = 4;
                        else if (args[0].equals("8"))
                            settings.reigsterDisplayWidth = 8;
                        else
                            System.err.println("* ERROR: only display with 4 and 8 are allowed");
                    }
                    Extras extras = getExtras();
                    if (previousExtras == null)
                        previousExtras = extras;
                    System.out.print("  $hi" + (extras.hi == previousExtras.hi ? " " : "*") + "[" + Util.addressToString(extras.hi) + "] ");
                    System.out.print("  $lo" + (extras.lo == previousExtras.lo ? " " : "*") + "[" + Util.addressToString(extras.lo) + "] ");
                    System.out.print("$irqh" + (extras.interruptHandler == previousExtras.interruptHandler ? " " : "*") + "[" + Util.addressToString(extras.interruptHandler) + "] ");
                    System.out.print(" $irq" + (extras.nextTimerInterrupt == previousExtras.nextTimerInterrupt ? " " : "*") + "[" + Util.addressToString(extras.nextTimerInterrupt) + "] ");
                    if (settings.reigsterDisplayWidth == 8) {
                        System.out.print(" $epc" + (extras.epc == previousExtras.epc ? " " : "*") + "[" + Util.addressToString(extras.epc) + "] ");
                    }

                    System.out.println();
                    previousExtras = extras;
                }
            });
        }
        private Integer evalInt(String string)
        {
            if (string.equals(""))
                return Integer.parseInt(""); // crash
            if (string.charAt(0) == '$') {
                // register
                int registerNumber = Util.linearSearch(Registers.NAMES, string);
                if (registerNumber != -1)
                    return getRegisters().values[registerNumber];
                try {
                    return getRegisters().values[Integer.parseInt(string.substring(1))];
                } catch (NumberFormatException e) {
                    System.err.println("* ERROR: unrecognized register name: " + string);
                    throw e;
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println(e);
                    throw new NumberFormatException();
                }
            }
            return Util.parseInt(string);
        }
    }
}
