package com.wolfesoftware.mipsos.common;

import java.io.*;

public abstract class Options
{
    public void normalize()
    {
    }

    protected static OutputStream openOutputStream(String path)
    {
        if (path.equals("-"))
            return System.out;
        try {
            return new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    protected static int parseHex(String string)
    {
        if (!string.startsWith("0x"))
            throw new RuntimeException("hex value must begin with 0x");
        return Integer.parseInt(string.substring("0x".length()), 16);
    }
}
