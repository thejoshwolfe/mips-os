package com.wolfesoftware.mipsos.mips.simulator;

import java.util.Hashtable;

public class Memory
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

    public byte loadByte(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void storeByte(int address, byte value)
    {
        // TODO Auto-generated method stub
    }

    public short loadHalf(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void storeHalf(int address, short value)
    {
        // TODO Auto-generated method stub
    }

    public int loadWord(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void storeWord(int address, int value)
    {
        // TODO Auto-generated method stub
    }

    public long loadDword(int address)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void storeDword(int address, long value)
    {
        // TODO Auto-generated method stub
    }

    public void clearAllSegments()
    {
        // TODO Auto-generated method stub
    }
}
