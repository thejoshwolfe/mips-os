package com.wolfesoftware.mipsos.simulator;

import java.io.IOException;

/**
 * it'd be cool to not need this callback. how would stdout work?
 */
public interface ISimulatorListener
{
    void printCharacter(char c);
    char readCharacter();

    /**
     * uses host stdin and stdout
     */
    ISimulatorListener STD_ADAPTER = new ISimulatorListener() {
        @Override
        public void printCharacter(char c)
        {
            System.out.print(c);
        }
        @Override
        public char readCharacter()
        {
            try {
                return (char)System.in.read();
            } catch (IOException e) {
                // yeah right.
                throw new RuntimeException(e);
            }
        }
    };
}