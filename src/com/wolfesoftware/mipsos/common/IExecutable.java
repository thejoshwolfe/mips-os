package com.wolfesoftware.mipsos.common;

public interface IExecutable
{
    IDataSegment[] getDataSegnemnts();
    
    int getEntryPoint();
}
