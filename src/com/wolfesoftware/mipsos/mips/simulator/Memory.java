package com.wolfesoftware.mipsos.mips.simulator;

import java.util.Hashtable;

import com.wolfesoftware.mipsos.common.*;

public class Memory implements IMultisizeMemory
{
	private Hashtable<Integer, Integer[]> pages = new Hashtable<Integer, Integer[]>();
	private final int pageSizeExponent;

	public Memory(int pageSizeExponent)
	{
		this.pageSizeExponent = pageSizeExponent;
	}

	public int load(int address)
	{
		return pages.get(address >> pageSizeExponent)[address & (-1 << pageSizeExponent)];
	}

    @Override
    public byte loadByte(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void storeByte(int address, byte value)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public short loadHalf(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void storeHalf(int address, short value)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int loadWord(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void storeWord(int address, int value)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long loadDword(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void storeDword(int address, long value)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initSegment(IDataSegment segment)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clearAllSegments()
    {
        // TODO Auto-generated method stub
        
    }
}
