package com.wolfesoftware.mipsos.mips.assembler;

import com.wolfesoftware.mipsos.common.AssemblingException;

public class CompilingException extends AssemblingException
{
	private static final long serialVersionUID = 8069960165341698636L;

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
	public String getMessage()
	{
		return message + "\n" + 
				"\tSource Location: (" + startLine + "," + startCol + "):" + len + "\n";
	}

}
