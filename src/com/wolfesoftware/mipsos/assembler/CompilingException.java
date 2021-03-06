package com.wolfesoftware.mipsos.assembler;


public class CompilingException extends AssemblingException
{
    public int srcStart;
    public int startLine;
    public int startCol;
    public int len;

    public CompilingException(int srcStart, int startLine, int startCol, int len, String message)
    {
        super(message);
        this.srcStart = srcStart;
        this.startLine = startLine;
        this.startCol = startCol;
        this.len = len;
    }
    @Override
    public String getMessage()
    {
        return message + "\n" + "\tSource Location: (" + startLine + "," + startCol + "):" + len + "\n";
    }
}
