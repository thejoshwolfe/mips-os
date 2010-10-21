package com.wolfesoftware.mipsos.common;

public interface IByteMemory extends ISegmentMemory
{
	byte loadByte(int address);
	void storeByte(int address, byte value);
}
