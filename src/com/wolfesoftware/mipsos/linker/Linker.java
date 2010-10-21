package com.wolfesoftware.mipsos.linker;

import com.wolfesoftware.mipsos.common.*;

public class Linker
{
    public static IExecutable link(ILinkable binaryObjects[], LinkerOptions options) throws LinkingException
    {
        // Hashtable<String, ByteSegment> dataSegments = new Hashtable<String,
        // ByteSegment>();
        // for (IBinaryObject binaryObject : binaryObjects)
        // {
        // dataSegments.put(binaryObject.getName() + ".data",
        // binaryObject.getDataSegment());
        // dataSegments.put(binaryObject.getName() + ".text",
        // binaryObject.getTextSegment());
        // }
        //
        // checkForCollisions(dataSegments); // throws SegmentCollisionException

        return null;
    }

    // private static void checkForCollisions(Hashtable<String, ByteSegment>
    // dataSegments) throws SegmentCollisionException
    // {
    // String[] collisions;
    //		
    // collisions = new String[0];
    //		
    // if (collisions.length != 0)
    // throw new SegmentCollisionException(collisions);
    // }

    public static class LinkerOptions
    {
        public String entryPointName = "kernel";
    }
}
