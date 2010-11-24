package com.wolfesoftware.mipsos.debugger;

import java.util.HashMap;

public enum DebuggerCommand {
    GO("g", "go", "continue", "resume"), //
    LIST("l", "ls", "list"),
    STEP("s", "step"), //
    ;

    private final String[] names;
    private DebuggerCommand(String... names)
    {
        this.names = names;
    }

    private static final HashMap<String, DebuggerCommand> fromNameMap = new HashMap<String, DebuggerCommand>();
    static {
        for (DebuggerCommand command : values())
            for (String name : command.names)
                fromNameMap.put(name, command);
    }

    public static DebuggerCommand fromName(String name)
    {
        return fromNameMap.get(name);
    }
}
