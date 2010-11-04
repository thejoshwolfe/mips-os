package com.wolfesoftware.mipsos.simulator;

import java.io.*;
import java.util.LinkedList;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.common.*;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        // parse the args
        AssemblerOptions assemblerOptions = new AssemblerOptions();
        LinkedList<String> argList = assemblerOptions.parse(args);
        if (assemblerOptions.outStream == System.out)
            throw new RuntimeException();
        if (Boolean.TRUE.equals(assemblerOptions.readable))
            throw new RuntimeException();
        assemblerOptions.readable = false;

        SimulatorOptions simulatorOptions = new SimulatorOptions();
        simulatorOptions.parse(argList);
        simulatorOptions.normalize();

        if (argList.size() != 1)
            throw new RuntimeException();
        String inputPath = argList.getFirst();

        // assemble source
        byte[] binaryBytes = Assembler.assembleToBytes(inputPath, assemblerOptions);
        InputStream binaryInputStream = new ByteArrayInputStream(binaryBytes);
        ExecutableBinary binary = ExecutableBinary.decode(binaryInputStream);

        // init the simulator
        SimulatorCore simulatorCore = new SimulatorCore(simulatorOptions, new ISimulatorListener() {
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
