package com.wolfesoftware.mipsos.assembler;

public class ParsingException extends AssemblingException
{
    public int tokenIndex;

    public ParsingException(int tokenIndex, String message)
    {
        super(message);
        this.tokenIndex = tokenIndex;
    }
}
