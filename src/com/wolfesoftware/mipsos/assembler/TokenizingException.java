package com.wolfesoftware.mipsos.assembler;


public class TokenizingException extends AssemblingException
{
	private static final long serialVersionUID = 655007843047191682L;

	public int srcLocation;

	public TokenizingException(int srcLocation, String message)
	{
		super(message);
		this.srcLocation = srcLocation;
	}
}
