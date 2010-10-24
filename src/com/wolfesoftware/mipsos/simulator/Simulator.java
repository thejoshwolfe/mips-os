package com.wolfesoftware.mipsos.simulator;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.simulator.SimulatorCore.SimulatorOptions;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        InputStream inStream = new FileInputStream(args[0]);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Assembler.assemble(inStream, outStream, false, Assembler.DefaultDataAddress, Assembler.DefaultTextAddress);

        SimulatorOptions options = new SimulatorOptions();
        options.fancyIoSupport = true;
        SimulatorCore simulatorCore = new SimulatorCore(options, new ISimulatorListener() {
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
        });

        // load single executable
        ExecutableBinary binary = ExecutableBinary.decode(outStream.toByteArray());
        for (Segment segment : binary.segments())
            simulatorCore.storeSegment(segment);
        simulatorCore.setPc(binary.executableEntryPoint);

        simulatorCore.run();
    }
}
