package com.wolfesoftware.mipsos.common;

public interface IHalfMemory extends ISegmentMemory
{
	short loadHalf(int address);
	void storeHalf(int address, short value);
}
