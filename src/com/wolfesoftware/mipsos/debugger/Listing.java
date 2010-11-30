package com.wolfesoftware.mipsos.debugger;

public class Listing
{
    public String[] lines;
    public int startLine;
    public int currentLine;
    public int currentAddress;
    public Listing(String[] lines, int startLine, int currentLine, int currentAddress)
    {
        this.lines = lines;
        this.startLine = startLine;
        this.currentLine = currentLine;
        this.currentAddress = currentAddress;
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
