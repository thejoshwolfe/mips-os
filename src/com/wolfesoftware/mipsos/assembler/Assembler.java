package com.wolfesoftware.mipsos.assembler;

import java.io.*;
import java.util.*;

//http://www.d.umn.edu/~gshute/spimsal/talref.html
//http://6004.csail.mit.edu/6.371/handouts/mips6371.pdf

public class Assembler
{
    public static final int DefaultDataAddress = 0x10000000;
    public static final int DefaultTextAddress = 0x00400000;

    private static final String blankBinAddr = "          ";
    private static final String blankBinWord = "                  ";

    /**
     * calls assemble() with options gathered from the command-line arguments.
     * See printUsage().
     */
    public static void main(String[] args)
    {
        // get options from args
        OutputStream outStream = System.out;
        boolean readable = false;
        Map<String, Integer> baseAddresses = makeDefaultOptions().getSegmentBaseAddresses();
        int dataAddress = baseAddresses.get(".data");
        int textAddress = baseAddresses.get(".text");

        // read filename for input
        if (!(args.length >= 1)) {
            printUsage();
            return;
        }
        FileInputStream inStream;
        try {
            inStream = new FileInputStream(args[0]);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + args[0]);
            return;
        }

        // go through remaining args looking for options
        int i = 1; // skip the input file name
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-d")) {
                // data address
                if (i + 1 >= args.length) {
                    printUsage();
                    return;
                }
                try {
                    dataAddress = parseAddress(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.out.println(e.toString());
                    printUsage();
                    return;
                }
                i += 2;
            } else if (arg.equals("-t")) {
                // text address
                if (i + 1 >= args.length) {
                    printUsage();
                    return;
                }
                try {
                    textAddress = parseAddress(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.out.println(e.toString());
                    printUsage();
                    return;
                }
                i += 2;
            } else if (arg.equals("-r")) {
                // readable
                readable = true;
                i += 1;
            } else if (arg.equals("-o")) {
                // output file
                if (i + 1 >= args.length) {
                    printUsage();
                    return;
                }
                // FileOutputStream outStream;
                try {
                    outStream = new FileOutputStream(args[i + 1]);
                } catch (FileNotFoundException e) {
                    System.out.println("Illegal output file name: " + args[i + 1]);
                    return;
                }
                // options.outStream = outStream;
                i += 2;
            } else {
                // invalid argument
                printUsage();
                return;
            }
        }

        // if no output file, force -r readable option (disallow writing
        // byte-data to System.out)
        if (outStream == System.out)
            readable = true;

        // call the assemble function
        try {
            assemble(inStream, outStream, readable, dataAddress, textAddress);
        } catch (AssemblingException e) {
            System.out.print(e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * the main point of this class. Assembles the MIPS source from inStream,
     * and prints the result to outStream.
     */
    public static void assemble(InputStream inStream, OutputStream outStream, boolean readable, int dataAddress, int textAddress) throws AssemblingException, IOException
    {
        // OutputStream outStream = options.outStream;

        // read input stream
        Scanner inScanner = new Scanner(inStream);
        String fullSrc = ""; // TODO: use StringBuilder
        ArrayList<Integer> lineIndecies = new ArrayList<Integer>();
        // convert all newlines to '\n'
        if (inScanner.hasNextLine()) { // do the first one specially with no "\n" at the beginning
            lineIndecies.add(fullSrc.length());
            fullSrc += inScanner.nextLine();
        }
        while (inScanner.hasNextLine()) {
            lineIndecies.add(fullSrc.length());
            fullSrc += "\n" + inScanner.nextLine();
        }

        // tokenize
        Token.TokenBase[] tokens;
        try {
            tokens = Tokenizer.tokenize(fullSrc);
        } catch (TokenizingException e) {
            int srcLocation = e.srcLocation;
            int line = findInList(lineIndecies, srcLocation);
            int col = srcLocation - lineIndecies.get(line);
            throw new CompilingException(srcLocation, line + 1, col + 1, 1, e.message);
        }

        // parse
        Parser.Binarization binarization;
        try {
            binarization = Parser.parse(tokens, dataAddress, textAddress);
        } catch (ParsingException e) {
            Token.TokenBase token = tokens[e.tokenIndex];
            int startLine = findInList(lineIndecies, token.srcStart);
            throw new CompilingException(token.srcStart, startLine + 1, token.srcStart - lineIndecies.get(startLine), token.srcEnd - token.srcStart, e.message);
        }

        // collect all the required labels
        BinTreeSet<String> requiredLabels = new BinTreeSet<String>();
        requiredLabels.add("main");
        for (Bin.BinBase binElem : binarization.dataElems)
            for (String s : binElem.getLabelDependencies())
                requiredLabels.add(s);
        for (Bin.BinBase binElem : binarization.textElems)
            for (String s : binElem.getLabelDependencies())
                requiredLabels.add(s);
        // collect all defined labels
        BinTreeSet<String> definedLabels = new BinTreeSet<String>();
        definedLabels.addAll(binarization.labels.keySet());
        // determine missing ones
        ArrayList<String> missingLabels = requiredLabels.complement(definedLabels);
        // check that all labels are defined
        if (!(missingLabels.isEmpty()))
            throw new UndefinedLabelsException(missingLabels); // report missing labels

        // output
        if (readable) {
            // .head section
            PrintStream printStream = new PrintStream(outStream);
            printStream.println(".head");
            {
                byte[] bytes = binarization.header.getBinary(binarization.labels, -1);
                int wordCounter = 0;
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; .data Offset");
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; .data Address");
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; .data Length");
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; .text Offset");
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; .text Address");
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; .text Length");
                printStream.println(blankBinAddr + bytesWordToString(bytes, 4 * wordCounter++) + " ; executable entry point");
            }

            // .data section
            verboseOutput(binarization.dataElems, printStream, ".data", dataAddress, binarization.labels, true, tokens, fullSrc);

            // .text section
            verboseOutput(binarization.textElems, printStream, ".text", textAddress, binarization.labels, true, tokens, fullSrc);
        } else {
            // .head section
            outStream.write(binarization.header.getBinary(binarization.labels, 0));

            // .data section
            nonverboseOutput(dataAddress, binarization.dataElems, outStream, binarization.labels);

            // .text section
            nonverboseOutput(textAddress, binarization.textElems, outStream, binarization.labels);
        }
    }

    private static void verboseOutput(Bin.BinBase[] elems, PrintStream printStream, String sectionTitle, long baseAddress, HashMap<String, Long> labels, boolean useAddress,
            Token.TokenBase[] tokens, String fullSrc)
    {
        if (elems.length > 0) {
            printStream.println();
            printStream.println(sectionTitle);
        }
        long addr = baseAddress;
        for (Bin.BinBase binElem : elems) {
            byte[] bytes = binElem.getBinary(labels, addr);
            String[] strBinWords = new String[bytes.length >> 2];
            for (int j = 0; j < bytes.length / 4; j++) {
                strBinWords[j] = (useAddress ? addrToString(addr) + ": " : blankBinAddr) + bytesWordToString(bytes, j * 4);
                addr += 4;
            }
            String comment = fullSrc.substring(tokens[binElem.tokenStart].srcStart, tokens[binElem.tokenEnd - 1].srcEnd);
            String[] commentLines = comment.split("\n");
            int maxLen = Math.max(strBinWords.length, commentLines.length);
            for (int j = 0; j < maxLen; j++) {
                printStream.println((j < strBinWords.length ? strBinWords[j] : blankBinWord) + (j < commentLines.length ? " ; " + commentLines[j] : ""));
            }
        }
    }

    private static void nonverboseOutput(long baseAddress, Bin.BinBase[] binElems, OutputStream outStream, HashMap<String, Long> labels) throws IOException
    {
        long addr = baseAddress;
        for (Bin.BinBase binElem : binElems) {
            byte[] data = binElem.getBinary(labels, addr);
            outStream.write(data);
            addr += data.length;
        }
    }

    private static int findInList(ArrayList<Integer> list, int target)
    {
        // iterative binary search
        int left = 0, right = list.size() - 1, mid;
        while (left < right) {
            mid = (left + right + 1) >> 1; // midpoint rounded up
            if (target < list.get(mid))
                right = mid - 1;
            else
                left = mid;
        }
        return left;
    }

    // validates the address into the 32-bit unsigned range and returns a String
    // of the hex value in the form "HHHHHHHH"
    private static String addrToString(long addr)
    {
        // validate range
        if (!(0 <= addr && addr <= 0xFFFFFFFFL))
            throw new RuntimeException(); // todo
        String rtnStr = "";
        for (int i = 0; i < 8; i++) // eight nibbles in a word
        {
            rtnStr = Long.toHexString(addr & 0xF).toUpperCase() + rtnStr;
            addr >>= 4;
        }
        return rtnStr;
    }

    // returns a String of the hex value of one word starting at i in the byte
    // array in the form "HHHHHHHH"
    private static String bytesWordToString(byte[] bytes, int i)
    {
        String rtnStr = "";
        for (int j = i; j < i + 4; j++) // four bytes in a word
        {
            // big endian
            rtnStr += Integer.toHexString((bytes[j] & 0xF0) >> 4).toUpperCase();
            rtnStr += Integer.toHexString((bytes[j] & 0x0F) >> 0).toUpperCase();
        }
        return rtnStr;
    }

    // prints the intended command line usage of the main function
    private static void printUsage()
    {
        System.out.print("\n" + //
                "Usage:\n" + //
                "    java Assembler (inputfile) [-o (outputfile)] [-t (textbaseaddress)] [-d (databaseaddress)] [-r]\n" + //
                "\n" + //
                "Example:\n" + //
                "    java Assembler MyMipsProgram.asm -o a.out.txt -t 0x00400000 -v\n" + //
                "\n" + //
                "Args:\n" + //
                "    [inputfile]: name of input file\n" + //
                "    [outputfile]: name of output file\n" + //
                "    [textbaseaddress]: base address of the instructions (int>=0. dec or hex)\n" + //
                "    [databaseaddress]: base address of the data (int>=0. dec or hex)\n" + //
                "\n" + //
                "Options:\n" + //
                "    [-r]: readable. human-readable output\n" + //
                "\n");
    }

    // parses an integer in either hex or decimal and and validates the value
    // into the 32-bit unsigned range
    private static int parseAddress(String text)
    {
        long temp;
        if (text.length() >= 2 && text.substring(0, 2).equals("0x")) {
            // hex base address
            temp = Long.parseLong(text.substring(2), 16);
        } else {
            // dec base address
            temp = Long.parseLong(text, 10);
        }
        if (temp > 0x7FFFFFFF) // java has no unsigned types
            temp -= 0x100000000L;
        if (!(0 <= temp && temp <= 0x7FFFFFFFL))
            throw new NumberFormatException();
        else
            return (int)(temp & -4); // truncate to word
    }

    public static IAssemblerOptions makeDefaultOptions()
    {
        return new IAssemblerOptions() {
            @Override
            public Map<String, Integer> getSegmentBaseAddresses()
            {
                Map<String, Integer> baseAddresses = new HashMap<String, Integer>(2);
                baseAddresses.put(".data", DefaultDataAddress);
                baseAddresses.put(".text", DefaultTextAddress);
                return baseAddresses;
            }
        };
    }
}
