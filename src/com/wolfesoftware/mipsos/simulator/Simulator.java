package com.wolfesoftware.mipsos.simulator;

import java.io.IOException;
import java.util.LinkedList;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.common.*;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        // parse the args
        LinkedList<String> argList = Util.arrayToLinkedList(args);
        SimulatorOptions simulatorOptions = new SimulatorOptions();
        simulatorOptions.parse(argList, false);
        simulatorOptions.normalize();

        if (argList.size() != 1)
            throw new RuntimeException();
        String inputPath = argList.getFirst();

        // assemble source
        ExecutableBinary binary = Assembler.assembleToBinary(inputPath, simulatorOptions.assemblerOptions);

        // init the simulator.
        SimulatorCore simulatorCore = new SimulatorCore(simulatorOptions, ISimulatorListener.STD_ADAPTER);
        simulatorCore.loadBinary(binary);

        // run and don't look back
        simulatorCore.run();
    }
}
