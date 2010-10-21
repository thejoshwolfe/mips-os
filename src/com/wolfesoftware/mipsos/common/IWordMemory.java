package com.wolfesoftware.mipsos.common;

public interface IWordMemory extends ISegmentMemory
{
	int loadWord(int address);
	void storeWord(int address, int value);
}
