package com.wolfesoftware.mipsos.simulator;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.ByteUtils;

/**
 * binary format for executable processes. there's probably only going to be one process at a time.
 */
public class ExecutableBinary
{
    private static final int MAGIC_NUMBER = 0x981dc595;
    public final Segment dataSegment;
    public final Segment textSegment;
    public final int executableEntryPoint;
    public ExecutableBinary(Segment dataSegment, Segment textSegment, int executableEntryPoint)
    {
        this.dataSegment = dataSegment;
        this.textSegment = textSegment;
        this.executableEntryPoint = executableEntryPoint;
    }

    public void encode(OutputStream outStream) throws IOException
    {
        // header
        ByteUtils.writeInt(outStream, 7 * 4);
        ByteUtils.writeInt(outStream, dataSegment.address);
        ByteUtils.writeInt(outStream, dataSegment.length);
        ByteUtils.writeInt(outStream, 7 * 4 + dataSegment.length);
        ByteUtils.writeInt(outStream, textSegment.address);
        ByteUtils.writeInt(outStream, textSegment.length);
        ByteUtils.writeInt(outStream, executableEntryPoint);

        outStream.write(dataSegment.bytes, dataSegment.offset, dataSegment.length);

        outStream.write(textSegment.bytes, textSegment.offset, textSegment.length);
    }

    public static ExecutableBinary decode(byte[] bytes)
    {
        int wordCounter = 0;

        int dataBytesOffset = ByteUtils.readInt(bytes, 4 * wordCounter++);
        int dataAddress = ByteUtils.readInt(bytes, 4 * wordCounter++);
        int dataLength = ByteUtils.readInt(bytes, 4 * wordCounter++);
        Segment dataSegment = new Segment(bytes, dataBytesOffset, dataAddress, dataLength);

        int textBytesOffset = ByteUtils.readInt(bytes, 4 * wordCounter++);
        int textAddress = ByteUtils.readInt(bytes, 4 * wordCounter++);
        int textLength = ByteUtils.readInt(bytes, 4 * wordCounter++);
        Segment textSegment = new Segment(bytes, textBytesOffset, textAddress, textLength);

        int executableEntryPoint = ByteUtils.readInt(bytes, 4 * wordCounter++);
        return new ExecutableBinary(dataSegment, textSegment, executableEntryPoint);
    }

    public Segment[] segments()
    {
        return new Segment[] { dataSegment, textSegment };
    }
}
