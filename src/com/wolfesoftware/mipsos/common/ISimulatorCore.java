package com.wolfesoftware.mipsos.common;

/**
 * This interface is to be implemented by the simulator core (core could be MIPS
 * or BrainF or whatever)
 */
public interface ISimulatorCore
{
    /** loads the executable to run. Returns NotInitialized on fail. */
    EStatus loadExecutable(IExecutable executable);

    /** resets all state information to the state just after loading */
    EStatus reload();

    /** executes one unit of code and returns the status */
    EStatus step();

    /** supplies input to the core's input buffer */
    void input(String inText);

    /** sets the listener for this core */
    void setSimulatorListener(ISimulatorListener listener);

    /** returns the status */
    EStatus getStatus();
}
