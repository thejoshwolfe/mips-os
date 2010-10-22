package com.wolfesoftware.mipsos.assembler;


public final class ByteUtils
{
    // converts an array of numbers to an array of bytes stored as an array of
    // bytes
    public static byte[] convertByte(long[] bytes)
    {
        byte[] _bytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            _bytes[i] = (byte)bytes[i];
        return _bytes;
    }

    // converts an array of numbers to an array of shorts stored as an array of
    // bytes
    public static byte[] convertShort(long[] shorts)
    {
        byte[] bytes = new byte[shorts.length * 2];
        for (int i = 0; i < shorts.length; i++)
        {
            // big endian
            bytes[i * 2 + 0] = (byte)((shorts[i] & 0xFF00) >> 8);
            bytes[i * 2 + 1] = (byte)((shorts[i] & 0x00FF) >> 0);
        }
        return bytes;
    }

    // converts an array of numbers to an array of ints stored as an array of bytes
    public static byte[] convertToInts(long[] ints)
    {
        byte[] bytes = new byte[ints.length * 4];
        for (int i = 0; i < ints.length; i++) {
            // big endian
            bytes[i * 4 + 0] = (byte)((ints[i] & 0xFF000000) >> 24);
            bytes[i * 4 + 1] = (byte)((ints[i] & 0x00FF0000) >> 16);
            bytes[i * 4 + 2] = (byte)((ints[i] & 0x0000FF00) >> 8);
            bytes[i * 4 + 3] = (byte)((ints[i] & 0x000000FF) >> 0);
        }
        return bytes;
    }

    // converts an array of numbers to an array of longs stored as an array of
    // bytes
    public static byte[] convertLong(long[] longs)
    {
        byte[] bytes = new byte[longs.length * 8];
        for (int i = 0; i < longs.length; i++)
        {
            // big endian
            bytes[i * 8 + 0] = (byte)((longs[i] & 0xFF00000000000000L) >> 56);
            bytes[i * 8 + 1] = (byte)((longs[i] & 0x00FF000000000000L) >> 48);
            bytes[i * 8 + 2] = (byte)((longs[i] & 0x0000FF0000000000L) >> 40);
            bytes[i * 8 + 3] = (byte)((longs[i] & 0x000000FF00000000L) >> 32);
            bytes[i * 8 + 4] = (byte)((longs[i] & 0x00000000FF000000L) >> 24);
            bytes[i * 8 + 5] = (byte)((longs[i] & 0x0000000000FF0000L) >> 16);
            bytes[i * 8 + 6] = (byte)((longs[i] & 0x000000000000FF00L) >> 8);
            bytes[i * 8 + 7] = (byte)((longs[i] & 0x00000000000000FFL) >> 0);
        }
        return bytes;
    }

    // converts an array of numbers to an array of floats stored as an array of
    // bytes
    public static byte[] convertFloat(double[] floats)
    {
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++)
        {
            int intVal = Float.floatToRawIntBits((float)floats[i]);
            // big endian
            bytes[i * 4 + 0] = (byte)((intVal & 0xFF000000) >> 24);
            bytes[i * 4 + 1] = (byte)((intVal & 0x00FF0000) >> 16);
            bytes[i * 4 + 2] = (byte)((intVal & 0x0000FF00) >> 8);
            bytes[i * 4 + 3] = (byte)((intVal & 0x000000FF) >> 0);
        }
        return bytes;
    }

    // converts an array of numbers to an array of doubles stored as an array of
    // bytes
    public static byte[] convertDouble(double[] doubles)
    {
        byte[] bytes = new byte[doubles.length * 8];
        for (int i = 0; i < doubles.length; i++)
        {
            long longVal = Double.doubleToRawLongBits(doubles[i]);
            // big endian
            bytes[i * 8 + 0] = (byte)((longVal & 0xFF00000000000000L) >> 56);
            bytes[i * 8 + 1] = (byte)((longVal & 0x00FF000000000000L) >> 48);
            bytes[i * 8 + 2] = (byte)((longVal & 0x0000FF0000000000L) >> 40);
            bytes[i * 8 + 3] = (byte)((longVal & 0x000000FF00000000L) >> 32);
            bytes[i * 8 + 4] = (byte)((longVal & 0x00000000FF000000L) >> 24);
            bytes[i * 8 + 5] = (byte)((longVal & 0x0000000000FF0000L) >> 16);
            bytes[i * 8 + 6] = (byte)((longVal & 0x000000000000FF00L) >> 8);
            bytes[i * 8 + 7] = (byte)((longVal & 0x00000000000000FFL) >> 0);
        }
        return bytes;
    }

    // converts a String to an of bytes (not null-terminated)
    public static byte[] convertAscii(String text)
    {
        // pad upto the closest word
        byte[] bytes = new byte[(text.length() + 3) & -4];
        System.arraycopy(text.getBytes(), 0, bytes, 0, text.length());
        return bytes;
    }

    // converts a String to an of bytes (null-terminated)
    public static byte[] convertAsciiz(String text)
    {
        // pad upto the closest word from .length() + 1
        byte[] bytes = new byte[(text.length() + 4) & -4];
        System.arraycopy(text.getBytes(), 0, bytes, 0, text.length());
        // bytes[text.length()] = 0;
        return bytes;
    }

    public static int readInt(byte[] bytes, int offset)
    {
        return ((bytes[offset + 0] & 0xFF) << 24) | //
                ((bytes[offset + 1] & 0xFF) << 16) | //
                ((bytes[offset + 2] & 0xFF) << 8) | //
                ((bytes[offset + 3] & 0xFF) << 0);
    }
}
