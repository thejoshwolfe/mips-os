package com.wolfesoftware.mipsos.common;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.ByteUtils;

/**
 * binary format for executable processes. there's probably only going to be one process at a time.
 */
public class ExecutableBinary
{
    private static final int MAGIC_NUMBER = 0x981dc595;
    public final Segment[] segments;
    public ExecutableBinary(Segment[] segments)
    {
        this.segments = segments;
    }

    public void encode(OutputStream outStream) throws IOException
    {
        // magic number
        ByteUtils.writeInt(outStream, MAGIC_NUMBER);

        // segments
        ByteUtils.writeInt(outStream, segments.length);
        for (Segment segment : segments)
            segment.encode(outStream);
    }

    public static ExecutableBinary decode(InputStream inStream) throws IOException
    {
        // magic number
        int magicNumber = ByteUtils.readInt(inStream);
        if (magicNumber != MAGIC_NUMBER)
            throw new RuntimeException();

        // segments
        int segmentsCount = ByteUtils.readInt(inStream);
        Segment[] segments = new Segment[segmentsCount];
        for (int i = 0; i < segmentsCount; i++)
            segments[i] = Segment.decode(inStream);

        return new ExecutableBinary(segments);
    }
}
