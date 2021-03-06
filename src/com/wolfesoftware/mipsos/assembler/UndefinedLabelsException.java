package com.wolfesoftware.mipsos.assembler;

import java.util.ArrayList;

public class UndefinedLabelsException extends AssemblingException
{
    public static final String PRMT_UndefinedLabels = "Undefined labels:";

    public ArrayList<String> missingLabels;

    public UndefinedLabelsException(ArrayList<String> missingLabels)
    {
        super(PRMT_UndefinedLabels);
        this.missingLabels = missingLabels;
    }
    @Override
    public String getMessage()
    {
        String outMsg = message + "\n";
        for (String label : missingLabels)
            outMsg += "\t" + label + "\n";
        return outMsg;
    }
}
