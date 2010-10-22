package com.wolfesoftware.mipsos.simulator;

import java.util.HashMap;

public class Memory
{
    private final HashMap<Integer, byte[]> pages = new HashMap<Integer, byte[]>();
    private final int pageSizeExponent;
    private final int pageSize;

    public Memory(int pageSizeExponent)
    {
        this.pageSizeExponent = pageSizeExponent;
        pageSize = 1 << pageSizeExponent;
    }

    public void storeSegment(int address, byte[] data)
    {
        int writtenCount = 0;
        while (writtenCount < data.length) {
            byte[] page = getPage(address);
            int pageOffset = getPageOffset(address);
            int length = Math.min(page.length - pageOffset, data.length - writtenCount);
            System.arraycopy(data, writtenCount, page, pageOffset, length);
            writtenCount += length;
        }
    }

    private int getPageOffset(int address)
    {
        return address & (pageSize - 1);
    }

    private byte[] getPage(int address)
    {
        int pageIndex = address >>> pageSizeExponent;
        byte[] page = pages.get(pageIndex);
        if (page == null) {
            page = new byte[pageSize];
            pages.put(pageIndex, page);
        }
        return page;
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
}
