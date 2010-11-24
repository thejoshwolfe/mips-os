package com.wolfesoftware.mipsos.debugger;

public class CliSettings
{
    public boolean autoList = true;
    public int listRadius = 5;

    public static CliSettings load()
    {
        // not actually going to read any files
        return new CliSettings();
    }
}
