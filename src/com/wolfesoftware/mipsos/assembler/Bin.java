package com.wolfesoftware.mipsos.assembler;

import java.util.HashMap;


/**
 * Container for BinBase and its subclasses
 */
public class Bin
{
    /**
     * The parent of all elements of a binary output. This structure stores the starting
     * and ending tokens of its source if applicable for tracing.
     */
    public static abstract class BinBase
    {
        public int tokenStart; // the first token that contributes to this binary element
        public int tokenEnd; // the token after the last element that contributes to this binary element

        /**
         *  constructor. takes parameters for the above two fields
         */
        public BinBase(int tokenStart, int tokenEnd)
        {
            this.tokenStart = tokenStart;
            this.tokenEnd = tokenEnd;
        }

        /**
         *  The purpose of this structure is to output a byte array to fill the output of
         * the assembler. Since the actual values of some elements can depend on label
         * mapping, the binary data need not be difined until a complete dictionary of 
         * labels has been constructed.
         */
        public abstract byte[] getBinary(HashMap<String, Long> labels, long currentAddress);

        /**
         * Returns the length of the byte array that getBinary() will return. This length 
         * must be definite as soon as a member of this structure is instantiated because it is
         * required for address incrementing.
         */
        public abstract int getBinLen();

        /**
         * Returns an array containing the names of all labels that must be defined in the 
         * HashMap passed to getBinary(). Child classes should override this method if
         * they depend on any labels
         */
        public String[] getLabelDependencies()
        {
            return new String[0];
        }
    }

    /**
     * Stores literal data (intended in the .data section)
     */
    public static class Data extends BinBase
    {
        public byte[] bytes; // the actual byte data

        // constructor needs to know the bytes. use convertXXX() below to generate
        // the bytes parameter here.
        public Data(byte[] bytes, int tokenStart, int tokenEnd)
        {
            super(tokenStart, tokenEnd);
            if ((bytes.length & 3) == 0)
                this.bytes = bytes;
            else {
                // pad up to a word
                this.bytes = new byte[(bytes.length + 3) & -4];
                System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
            }
        }

        @Override
        public int getBinLen()
        {
            return bytes.length;
        }

        @Override
        public byte[] getBinary(HashMap<String, Long> labels, long currentAddress)
        {
            return bytes;
        }

        // converts integers of any size to appropriate byte array.
        public static byte[] convertIntegral(Token.TagData.DataTagEnum dataType, long[] nums)
        {
            switch (dataType) {
                case BYTE:
                    return ByteUtils.convertByte(nums);
                case HALF:
                    return ByteUtils.convertShort(nums);
                case WORD:
                    return ByteUtils.convertToInts(nums);
                case DWORD:
                    return ByteUtils.convertLong(nums);
                default:
                    return null;
            }
        }

        // converts either floats or doubles to appropriate byte array
        public static byte[] convertFloating(Token.TagData.DataTagEnum dataType, double[] nums)
        {
            switch (dataType) {
                case FLOAT:
                    return ByteUtils.convertFloat(nums);
                case DOUBLE:
                    return ByteUtils.convertDouble(nums);
                default:
                    return null;
            }
        }
    }

    /**
     * The header section of a Parser.Binarization. Stores 8 ints. See below.
     */
    public static class Header extends BinBase
    {
        public int dataOffset; // the start of the .data section
        public int dataAddr; // the intended base address for the .data section
        public int dataLen; // the length of the .data section
        public int textOffset; // the start of the .text section
        public int textAddr; // the intended base address for the .text section
        public int textLen; // the length of the .text section

        public Header(int dataAddr, int dataLen, int textAddr, int textLen)
        {
            super(0, 0);
            this.dataOffset = getBinLen();
            this.dataAddr = dataAddr;
            this.dataLen = dataLen;
            this.textOffset = dataOffset + dataLen; // starts after .data
            this.textAddr = textAddr;
            this.textLen = textLen;
        }

        @Override
        public int getBinLen()
        {
            return getBinary(null, 0).length;
        }

        // outputs the 8 ints one right after the other
        @Override
        public byte[] getBinary(HashMap<String, Long> labels, long currentAddress)
        {
            return ByteUtils.convertToInts(new long[] { //
                    dataOffset, //
                    dataAddr, //
                    dataLen, //
                    textOffset, //
                    textAddr, //
                    textLen, //
                    labels == null ? 0 : labels.get("main"),
            });
        }
    }

    // TODO: continue Javadoc
    // the parent class for non-pseudo instruction elements
    public static abstract class Instr extends BinBase
    {
        public byte opcode; // all formats have the same opcode field

        // constructor takes the instr value and converts it to the proper opcode.
        protected Instr(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd)
        {
            super(tokenStart, tokenEnd);
            opcode = instrToOpcode(instr);
        }

        // this class implements the getBinary() method but requires child classes to implement
        // the getBinWord method. The binary word returned here is used for getBinary().
        protected abstract long getBinWord(HashMap<String, Long> labels, long currentAddress);

        // all non-pseudo instructions are 1 word
        @Override
        public int getBinLen()
        {
            return 4;
        }

        // implemented for all instruction elements
        @Override
        public byte[] getBinary(HashMap<String, Long> labels, long currentAddress)
        {
            long binWord = getBinWord(labels, currentAddress);
            // big endian
            return new byte[] { //
                    (byte)((binWord & 0xFF000000) >> 24), //
                    (byte)((binWord & 0x00FF0000) >> 16), //
                    (byte)((binWord & 0x0000FF00) >> 8), //
                    (byte)((binWord & 0x000000FF) >> 0), //
            };
        }

        // list of opcodes
        public static byte instrToOpcode(Token.InstrName.InstrEnum instr)
        {
            switch (instr) {
                case NOP:     return 0x00;
                case SLL:     return 0x00;
                case SRL:     return 0x00;
                case SRA:     return 0x00;
                case SLLV:    return 0x00;
                case SRLV:    return 0x00;
                case SRAV:    return 0x00;
                case JR:      return 0x00;
                case JALR:    return 0x00;
                case SYSCALL: return 0x00;
                case BREAK:   return 0x00;
                case MFHI:    return 0x00;
                case MTHI:    return 0x00;
                case MFLO:    return 0x00;
                case MTLO:    return 0x00;
                case MULT:    return 0x00;
                case DIV:     return 0x00;
                case ADD:     return 0x00;
                case SUB:     return 0x00;
                case AND:     return 0x00;
                case OR:      return 0x00;
                case XOR:     return 0x00;
                case NOR:     return 0x00;
                case SLT:     return 0x00;
                case BEQ:     return 0x04;
                case BNE:     return 0x05;
                case ADDI:    return 0x08;
                case SLTI:    return 0x0A;
                case ANDI:    return 0x0C;
                case ORI:     return 0x0D;
                case XORI:    return 0x0E;
                case MFC0:    return 0x10;
                case MTC0:    return 0x10;
                case LUI:     return 0x0F;
                case LB:      return 0x20;
                case LH:      return 0x21;
                case LW:      return 0x23;
                case SB:      return 0x28;
                case SH:      return 0x29;
                case SW:      return 0x2B;
                case J:       return 0x02;
                case JAL:     return 0x03;
                default:      return -1;
            }
        }
    }

    // I-format instruction with no label resolution needed. 
    // Immediate field must be passed to the constructor
    public static class InstrI extends Instr
    {
        public byte rs; // rs field
        public byte rt; // rt field
        public short imm; // known at instantiation time

        // constructor must be provided the immediage field
        public InstrI(Token.InstrName.InstrEnum instr, int rs, int rt, int imm, int tokenStart, int tokenEnd)
        {
            super(instr, tokenStart, tokenEnd);
            this.rs = (byte)rs;
            this.rt = (byte)rt;
            this.imm = (short)imm;
        }

        // I-format
        @Override
        protected long getBinWord(HashMap<String, Long> labels, long currentAddress)
        {
            return (opcode << 26) | (rs << 21) | (rt << 16) | (imm & 0xFFFF);
        }
    }

    // parent class of I-format instructions that needs the 
    // address of a label to fill their immediate field.
    public static abstract class InstrILabel extends InstrI
    {
        public String labelName; // the name of the label which this instruction depends on

        // requires the instr value, the rs and rt register numbers, and the name of the label.
        protected InstrILabel(Token.InstrName.InstrEnum instr, int rs, int rt, String label, int tokenStart, int tokenEnd)
        {
            super(instr, rs, rt, 0, tokenStart, tokenEnd);
            this.rs = (byte)rs;
            this.rt = (byte)rt;
            this.labelName = label;
        }

        // child classes will use the label's address in some way to fill the immediate field.
        protected abstract short getImm(HashMap<String, Long> labels, long currentAddress);

        // I-format after filling the immediate field with getImm()
        @Override
        protected long getBinWord(HashMap<String, Long> labels, long currentAddress)
        {
            imm = getImm(labels, currentAddress);
            return (opcode << 26) | (rs << 21) | (rt << 16) | (imm & 0xFFFF);
        }

        @Override
        public String[] getLabelDependencies()
        {
            return new String[] { labelName };
        }
    }

    // I-format instruction that fills the immediate field with the 
    // difference between currentAddress and the label's address.
    // NOTE: currentAddress is expected to be PC+4 (NOT PC)
    public static class InstrIBranch extends InstrILabel
    {
        // See parent's constructor
        public InstrIBranch(Token.InstrName.InstrEnum instr, int rs, int rt, String label, int tokenStart, int tokenEnd)
        {
            super(instr, rs, rt, label, tokenStart, tokenEnd);
        }

        // fills the immediate field with the difference between currentAddress 
        // and the label's address.
        // NOTE: currentAddress is expected to be PC+4 (NOT PC)
        @Override
        protected short getImm(HashMap<String, Long> labels, long currentAddress)
        {
            long addr = labels.get(labelName);
            return (short)((addr - currentAddress - 1) >> 2 & 0xFFFF);
        }
    }

    // I-format instruction that uses the lower 16 bits 
    // of a label's address as the immediate field
    public static class InstrIHalf extends InstrILabel
    {
        // see parent's constructor for field explanation
        public InstrIHalf(Token.InstrName.InstrEnum instr, int rs, int rt, String label, int tokenStart, int tokenEnd)
        {
            super(instr, rs, rt, label, tokenStart, tokenEnd);
        }

        // uses the lower 16 bits of label's address
        @Override
        protected short getImm(HashMap<String, Long> labels, long currentAddress)
        {
            long addr = labels.get(labelName);
            return (short)((addr & 0x0000FFFF) >> 0);
        }
    }

    //I-format instruction that uses the upper 16 bits 
    //of a label's address as the immediate field
    public static class InstrIUpper extends InstrILabel
    {
        // see parent's constructor for field explanation
        public InstrIUpper(Token.InstrName.InstrEnum instr, int rs, int rt, String label, int tokenStart, int tokenEnd)
        {
            super(instr, rs, rt, label, tokenStart, tokenEnd);
        }

        // uses the upper 16 bits of label's address
        @Override
        protected short getImm(HashMap<String, Long> labels, long currentAddress)
        {
            long addr = labels.get(labelName);
            return (short)((addr & 0xFFFF0000) >> 16);
        }
    }

    // J-format instruction. Requires a label.
    public static class InstrJ extends Instr
    {
        public static long TARGET_MASK = 0x0FFFFFFC; // the important 26 bits of the target address

        public String labelName; // the name of the label to jump to

        // requires a label.
        public InstrJ(Token.InstrName.InstrEnum instr, String label, int tokenStart, int tokenEnd)
        {
            super(instr, tokenStart, tokenEnd);
            this.labelName = label;
        }

        // fills the 26-bit target field with the most important part of the address of the label.
        // if the target is unreachable, throws an exception
        @Override
        protected long getBinWord(HashMap<String, Long> labels, long currentAddress)
        {
            long targetAddress = labels.get(labelName);
            if ((targetAddress & ~TARGET_MASK) != (currentAddress & ~TARGET_MASK))
                throw new RuntimeException();
            long target = (targetAddress & TARGET_MASK) >> 2;
            return (opcode << 26) | (target << 0);
        }

        @Override
        public String[] getLabelDependencies()
        {
            return new String[] { labelName };
        }
    }

    // R-format instruction. Requires all fields but funct at instantiation time. 
    public static class InstrR extends Instr
    {
        public byte rs;
        public byte rt;
        public byte rd;
        public byte shamt;
        public byte funct;

        // Takes the instr value and fills the funct field accordingly. 
        // Requires remaining 4 fields. 
        public InstrR(Token.InstrName.InstrEnum instr, int rs, int rt, int rd, int shamt, int tokenStart, int tokenEnd)
        {
            super(instr, tokenStart, tokenEnd);
            this.rs = (byte)rs;
            this.rt = (byte)rt;
            this.rd = (byte)rd;
            this.shamt = (byte)shamt;
            funct = instrToFunct(instr);
        }

        // R-format
        @Override
        protected long getBinWord(HashMap<String, Long> labels, long currentAddress)
        {
            return (opcode << 26) | (rs << 21) | (rt << 16) | (rd << 11) | (shamt << 6) | (funct << 0);
        }

        // list of funct values
        public static byte instrToFunct(Token.InstrName.InstrEnum instr)
        {
            switch (instr) {
                case NOP:     return 0x00;
                case SLL:     return 0x00;
                case SRL:     return 0x02;
                case SRA:     return 0x03;
                case SLLV:    return 0x04;
                case SRLV:    return 0x06;
                case SRAV:    return 0x07;
                case JR:      return 0x08;
                case JALR:    return 0x09;
                case SYSCALL: return 0x0C;
                case BREAK:   return 0x0D;
                case MFHI:    return 0x10;
                case MTHI:    return 0x11;
                case MFLO:    return 0x12;
                case MTLO:    return 0x13;
                case MULT:    return 0x18;
                case DIV:     return 0x1A;
                case ADD:     return 0x20;
                case SUB:     return 0x22;
                case AND:     return 0x24;
                case OR:      return 0x25;
                case XOR:     return 0x26;
                case NOR:     return 0x27;
                case SLT:     return 0x2A;
                case MFC0:    return 0x00;
                case MTC0:    return 0x00;
                default:      throw null;
            }
        }
    }

    // A label declaration. This binary element takes up no space in the output,
    // but is still required as an element for mapping and tracing purposes.
    public static class Label extends BinBase
    {
        public String labelName;

        public Label(String labelName, int tokenStart, int tokenEnd)
        {
            super(tokenStart, tokenEnd);
            this.labelName = labelName;
        }

        // takes up no space
        @Override
        public int getBinLen()
        {
            return 0;
        }

        // takes up no space
        @Override
        public byte[] getBinary(HashMap<String, Long> labels, long currentAddress)
        {
            return new byte[0];
        }
    }

    // a pseudo-instruction element. This instruction is composed of multiple 
    // other instructions, yet has a single tokenStart and tokenEnd for tracing.
    // The constructor requires all of the sub-instructions. 
    public static class Pseudo extends BinBase
    {
        public Instr[] instrs;

        // requires all sub-instructions
        protected Pseudo(Instr[] instrs, int tokenStart, int tokenEnd)
        {
            super(tokenStart, tokenEnd);
            this.instrs = instrs;
        }

        // sum of components
        @Override
        public int getBinLen()
        {
            int rtnVal = 0;
            for (Instr instr : instrs)
                rtnVal += instr.getBinLen();
            return rtnVal;
        }

        // sum of components
        @Override
        public byte[] getBinary(HashMap<String, Long> labels, long currentAddress)
        {
            byte[] bytes = new byte[getBinLen()];
            int i = 0;
            for (Instr binInstr : instrs) {
                for (byte b : binInstr.getBinary(labels, currentAddress))
                    bytes[i++] = b;
            }
            return bytes;
        }

        @Override
        public String[] getLabelDependencies()
        {
            java.util.ArrayList<String> labels = new java.util.ArrayList<String>();
            for (Instr instr : instrs)
                for (String string : instr.getLabelDependencies())
                    labels.add(string);
            return labels.toArray(new String[0]);
        }
    }
}
