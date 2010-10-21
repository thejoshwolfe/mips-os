package com.wolfesoftware.mipsos.common;

import com.wolfesoftware.mipsos.linker.LinkerOptions;

public interface ILinker
{
	public IExecutable link(ILinkable[] binaryObjects, LinkerOptions options) throws LinkingException;
}
