package com.wolfesoftware.mipsos.simulator;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.simulator.SimulatorCore.SimulatorOptions;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        // parse the args
        String source = null;
        SimulatorOptions options = new SimulatorOptions();

        for (String arg : args) {
            if (arg.equals("--fancy"))
                options.fancyIoSupport = true;
            else {
                if (source != null)
                    throw new RuntimeException();
                source = arg;
            }
        }
        if (source == null)
            throw new RuntimeException();

        // assemble source
        InputStream inStream = new FileInputStream(source);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Assembler.assemble(inStream, outStream, false, Assembler.DefaultDataAddress, Assembler.DefaultTextAddress);
        ExecutableBinary binary = ExecutableBinary.decode(outStream.toByteArray());

        // init the simulator
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
        for (Segment segment : binary.segments())
            simulatorCore.storeSegment(segment);
        simulatorCore.setPc(binary.executableEntryPoint);


        simulatorCore.run();
    }
}
