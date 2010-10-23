package com.wolfesoftware.mipsos.simulator;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.common.*;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        InputStream inStream = new FileInputStream(args[0]);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Assembler.assemble(inStream, outStream, false, Assembler.DefaultDataAddress, Assembler.DefaultTextAddress);

        SimulatorCore simulatorCore = new SimulatorCore();
        simulatorCore.setSimulatorListener(new ISimulatorListener() {
            @Override
            public void printCharacter(char c)
            {
                System.out.print(c);
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
