package com.wolfesoftware.mipsos.debugger;

public class Extras
{
    public final int hi;
    public final int lo;
    public final int interruptHandler;
    public final int nextTimerInterrupt;
    public final int epc;

    public Extras(int hi, int lo, int interruptHandler, int nextTimerInterrupt, int epc)
    {
        this.hi = hi;
        this.lo = lo;
        this.interruptHandler = interruptHandler;
        this.nextTimerInterrupt = nextTimerInterrupt;
        this.epc = epc;
    }
}
