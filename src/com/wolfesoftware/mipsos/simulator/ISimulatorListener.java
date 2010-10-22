package com.wolfesoftware.mipsos.simulator;

/**
 * it'd be cool to not need this callback. how would stdout work?
 */
public interface ISimulatorListener
{
    void printCharacter(char c);
}