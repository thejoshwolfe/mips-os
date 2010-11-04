package com.wolfesoftware.mipsos.simulator;

import java.io.*;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.simulator.SimulatorCore.SimulatorOptions;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        // parse the args
        SimulatorOptions options = new SimulatorOptions();
        args = options.parse(args);
        if (args.length != 1)
            throw new RuntimeException();
        String source = args[0];

        // assemble source
        byte[] binaryBytes = Assembler.assemble(new File(source), false, Assembler.DefaultDataAddress, Assembler.DefaultTextAddress);
        InputStream binaryInputStream = new ByteArrayInputStream(binaryBytes);
        ExecutableBinary binary = ExecutableBinary.decode(binaryInputStream);

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
        for (Segment segment : binary.segments) {
            byte[] addressBytes = segment.attributes.get(Segment.ATTRIBUTE_ADDRESS);
            if (addressBytes == null)
                continue;
            int address = ByteUtils.readInt(addressBytes, 0);
            simulatorCore.storeBytes(segment.bytes, segment.offset, segment.length, address);
        }
        simulatorCore.setPc(binary.executableEntryPoint);


        simulatorCore.run();
    }
}
