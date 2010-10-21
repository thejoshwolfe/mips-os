package com.wolfesoftware.mipsos.common;

public interface IDwordMemory extends ISegmentMemory
{
	long loadDword(int address);
	void storeDword(int address, long value);
}
