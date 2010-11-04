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

    public void storeBytes(byte[] bytes, int offset, int length, int address)
    {
        int writtenCount = 0;
        while (writtenCount < length) {
            byte[] page = getPage(address + writtenCount);
            int pageOffset = getPageOffset(address + writtenCount);
            int chunkLength = Math.min(page.length - pageOffset, length - writtenCount);
            System.arraycopy(bytes, offset + writtenCount, page, pageOffset, chunkLength);
            writtenCount += chunkLength;
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
        return getPage(address)[getPageOffset(address)];
    }

    public void storeByte(int address, byte value)
    {
        getPage(address)[getPageOffset(address)] = value;
    }

    public short loadHalf(int address)
    {
        byte[] page = getPage(address);
        int offset = getPageOffset(address);
        return (short)( //
                (page[offset + 0] & 0xFF) << 8 | //
                (page[offset + 1] & 0xFF) << 0 //
        );
    }

    public void storeHalf(int address, short value)
    {
        byte[] page = getPage(address);
        int offset = getPageOffset(address);
        page[offset + 0] = (byte)((value & 0xFF00) >>> 8);
        page[offset + 1] = (byte)((value & 0x00FF) >>> 0);
    }

    public int loadWord(int address)
    {
        byte[] page = getPage(address);
        int offset = getPageOffset(address);
        return ( //
                (page[offset + 0] & 0xFF) << 24 | //
                (page[offset + 1] & 0xFF) << 16 | //
                (page[offset + 2] & 0xFF) << 8 | //
                (page[offset + 3] & 0xFF) << 0 //
        );
    }

    public void storeWord(int address, int value)
    {
        byte[] page = getPage(address);
        int offset = getPageOffset(address);
        page[offset + 0] = (byte)((value & 0xFF000000) >>> 24);
        page[offset + 1] = (byte)((value & 0x00FF0000) >>> 16);
        page[offset + 2] = (byte)((value & 0x0000FF00) >>> 8);
        page[offset + 3] = (byte)((value & 0x000000FF) >>> 0);
    }

    public long loadDword(int address)
    {
        byte[] page = getPage(address);
        int offset = getPageOffset(address);
        return ( //
                ((long)page[offset + 0] & 0xFF) << 56 | //
                ((long)page[offset + 1] & 0xFF) << 48 | //
                ((long)page[offset + 2] & 0xFF) << 40 | //
                ((long)page[offset + 3] & 0xFF) << 32 | //
                ((long)page[offset + 4] & 0xFF) << 24 | //
                ((long)page[offset + 5] & 0xFF) << 16 | //
                ((long)page[offset + 6] & 0xFF) << 8 | //
                ((long)page[offset + 7] & 0xFF) << 0 //
        );
    }

    public void storeDword(int address, long value)
    {
        byte[] page = getPage(address);
        int offset = getPageOffset(address);
        page[offset + 0] = (byte)((value & 0xFF00000000000000L) >>> 56);
        page[offset + 1] = (byte)((value & 0x00FF000000000000L) >>> 48);
        page[offset + 2] = (byte)((value & 0x0000FF0000000000L) >>> 40);
        page[offset + 3] = (byte)((value & 0x000000FF00000000L) >>> 32);
        page[offset + 4] = (byte)((value & 0x00000000FF000000L) >>> 24);
        page[offset + 5] = (byte)((value & 0x0000000000FF0000L) >>> 16);
        page[offset + 6] = (byte)((value & 0x000000000000FF00L) >>> 8);
        page[offset + 7] = (byte)((value & 0x00000000000000FFL) >>> 0);
    }
}
