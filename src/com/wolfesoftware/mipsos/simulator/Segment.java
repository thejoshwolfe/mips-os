package com.wolfesoftware.mipsos.simulator;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.wolfesoftware.mipsos.assembler.ByteUtils;

public class Segment
{
    public static final String ATTRIBUTE_ADDRESS = "address";
    public final HashMap<String, byte[]> attributes;
    public final byte[] bytes;
    public final int offset;
    public final int length;
    public Segment(HashMap<String, byte[]> attributes, byte[] bytes)
    {
        this(attributes, bytes, 0, bytes.length);
    }
    public Segment(HashMap<String, byte[]> attributes, byte[] bytes, int offset, int length)
    {
        this.attributes = attributes;
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }
    public void encode(OutputStream outStream) throws IOException
    {
        // attributes
        ByteUtils.writeInt(outStream, attributes.size());
        for (Entry<String, byte[]> entry : attributes.entrySet()) {
            ByteUtils.writeString(outStream, entry.getKey());
            ByteUtils.writeByteArray(outStream, entry.getValue());
        }

        // bytes
        ByteUtils.writeByteArray(outStream, bytes, offset, length);
    }
    public static Segment decode(InputStream inStream) throws IOException
    {
        // attributes
        int attributesCount = ByteUtils.readInt(inStream);
        HashMap<String, byte[]> attributes = new HashMap<String, byte[]>();
        for (int i = 0; i < attributesCount; i++) {
            String key = ByteUtils.readString(inStream);
            byte[] value = ByteUtils.readByteArray(inStream);
            attributes.put(key, value);
        }

        // bytes
        byte[] bytes = ByteUtils.readByteArray(inStream);

        return new Segment(attributes, bytes);
    }
}
