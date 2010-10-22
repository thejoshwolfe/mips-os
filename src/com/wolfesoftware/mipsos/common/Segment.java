package com.wolfesoftware.mipsos.common;

public class Segment
{
    public final byte[] bytes;
    public final int offset;
    public final int address;
    public final int length;
    public Segment(byte[] bytes, int offset, int address, int length)
    {
        this.bytes = bytes;
        this.offset = offset;
        this.address = address;
        this.length = length;
    }
}
