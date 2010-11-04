package com.wolfesoftware.mipsos.simulator;

public class Segment
{
    public final byte[] bytes;
    public final int offset;
    public final int address;
    public final int length;
    public Segment(byte[] bytes, int address)
    {
        this(bytes, 0, address, bytes.length);
    }
    public Segment(byte[] bytes, int offset, int address, int length)
    {
        this.bytes = bytes;
        this.offset = offset;
        this.address = address;
        this.length = length;
    }

    @Override
    public String toString()
    {
        return "(0x" + zeroFill(Integer.toHexString(address), 8) + "+" + length + ")";
    }
    private static String zeroFill(String s, int length)
    {
        if (length <= s.length())
            return s;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length - s.length(); i++)
            builder.append('0');
        builder.append(s);
        return builder.toString();
    }
}
