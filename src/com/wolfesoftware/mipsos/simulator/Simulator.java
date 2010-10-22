package com.wolfesoftware.mipsos.simulator;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.*;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        InputStream inStream = new FileInputStream(args[0]);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Assembler.assemble(inStream, outStream, false, Assembler.DefaultDataAddress, Assembler.DefaultTextAddress);

        SimulatorCore core = new SimulatorCore();
        core.setSimulatorListener(new ISimulatorListener() {
            @Override
            public void printCharacter(char c)
            {
                System.out.print(c);
            }
        });

        // TODO
        outStream.toByteArray();
    }
}
