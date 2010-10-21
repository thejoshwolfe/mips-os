package com.wolfesoftware.mipsos.common;

public interface IAssembler
{
	public ILinkable assemble(java.io.InputStream input, IAssemblerOptions options) throws AssemblingException;
}
