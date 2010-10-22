package com.wolfesoftware.mipsos.common;

import com.wolfesoftware.mipsos.assembler.ByteUtils;

/**
 * binary format for executable processes. there's probably only going to be one process at a time.
 */
public class ExecutableBinary
{
    public final Segment dataSegment;
    public final Segment textSegment;
    public final int executableEntryPoint;
    private ExecutableBinary(Segment dataSegment, Segment textSegment, int executableEntryPoint)
    {
        this.dataSegment = dataSegment;
        this.textSegment = textSegment;
        this.executableEntryPoint = executableEntryPoint;
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
}
