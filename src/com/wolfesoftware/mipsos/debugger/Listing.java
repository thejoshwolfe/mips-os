package com.wolfesoftware.mipsos.debugger;

public class Listing
{
    public final String[] lines;
    public final int startLine;
    public final int currentLine;
    public final int currentAddress;
    public final long clock;
    public Listing(String[] lines, int startLine, int currentLine, int currentAddress, long clock)
    {
        this.lines = lines;
        this.startLine = startLine;
        this.currentLine = currentLine;
        this.currentAddress = currentAddress;
        this.clock = clock;
    }
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String prefix = i + startLine == currentLine ? "->" : "  ";
            builder.append(prefix + lines[i]).append('\n');
        }
        return builder.toString();
    }
}
