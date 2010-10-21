package com.wolfesoftware.mipsos.common;

/**
 * this interface is to be implemented by the host application
 * (like a JApplet)
 */ 
public interface ISimulatorListener
{
    /** notifies the listener that the provided text is being outputted by the core */
    void output(String outText);
}