package com.wolfesoftware.mipsos.assembler;

public class TokenizingException extends AssemblingException
{
    public int srcLocation;

    public TokenizingException(int srcLocation, String message)
    {
        super(message);
        this.srcLocation = srcLocation;
    }
}
