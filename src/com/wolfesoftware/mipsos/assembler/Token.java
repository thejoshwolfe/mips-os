package com.wolfesoftware.mipsos.assembler;

public class Token
{
    // the parent of all token objects. Includes the index of the first
    // character
    // of this token, and the index of the character after the last.
    public static abstract class TokenBase
    {
        public int srcStart; // the first character's index
        public int srcEnd; // the last character's index + 1

        // constructor
        protected TokenBase(int srcStart, int srcEnd)
        {
            this.srcStart = srcStart;
            this.srcEnd = srcEnd;
        }
    }

    // an instruction name token (such as "addi")
    public static class InstrName extends TokenBase
    {
        public enum InstrEnum {
            // basic instructions
            ADD, ADDI, AND, ANDI, BEQ, BNE, BREAK, DIV, J, JAL, JALR, JR, LB, LH, LUI, LW, MFHI, MFLO, MTHI, MTLO, MULT, NOP, NOR, OR, ORI, SB, SH, SLL, SLLV, SLT, SLTI, SRA, SRAV, SRL, SRLV, SUB, SW, SYSCALL, XOR, XORI,
            // pseudo instructions
            BGE, BGEZ, BGT, BGTZ, BLE, BLEZ, BLT, BLTZ, LA, LI, MOVE, MUL,
            // NOTE: no unsigned arithmetic instructions are supported yet (TODO)
        }

        public InstrEnum instr; // the instruction that this token stores

        // constructor
        public InstrName(InstrEnum instr, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.instr = instr;
        }
    }

    // a label token, i.e. a string that has valid label-name characters, but
    // is not a supported instruction.
    public static class Label extends TokenBase
    {
        public String labelName; // the name of the label

        // requires the name of the label
        public Label(String labelName, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.labelName = labelName;
        }
    }

    // parent for literal tokens. The purpose of this class is to provide
    // an Type filter for literal tokens.
    public static abstract class Literal extends TokenBase
    {
        protected Literal(int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
        }
    }

    // a literal floating point token
    public static class LiteralDouble extends Literal
    {
        public double value; // value is stored as a double

        // constructor. requires value be parsed prior to this call
        public LiteralDouble(double value, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.value = value;
        }
    }

    // literal integral token.
    public static class LiteralLong extends Literal
    {
        public long value; // the integral value stored as a long

        // constructor. requires the value be parsed prior to this call.
        public LiteralLong(long value, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.value = value;
        }
    }

    // punctuation token.
    public static class PunctClass extends TokenBase
    {
        // these are the only valid punctuation marks in MIPS
        public enum PunctEnum {
            COMMA, COLON, OPEN_PARENTH, CLOSE_PARENTH,
        }

        public PunctEnum punct; // the punctuation of this token

        // constructor.
        public PunctClass(PunctEnum punct, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.punct = punct;
        }
    }

    // a register token (usually "$xx")
    public static class Register extends TokenBase
    {
        public int regNum; // the register number (in the range 0 - 31)

        // constructor
        public Register(int regNum, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.regNum = regNum;
        }
    }

    // a literal string token. This token includes the entire quoted string.
    public static class StringClass extends TokenBase
    {
        public String string; // the entire quoted string

        // constructor. The string must be parsed for escape characters prior to
        // this call.
        public StringClass(String string, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.string = string;
        }
    }

    // parent class for a tag token (".xxxx")
    public static abstract class Tag extends TokenBase
    {
        protected Tag(int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
        }
    }

    // a tag token specifying a data type (to be used in the .data section)
    public static class TagData extends Tag
    {
        // supported data types
        public enum DataTagEnum {
            BYTE, HALF, WORD, DWORD, FLOAT, DOUBLE, ASCII, ASCIIZ,
        }

        public DataTagEnum tag; // the data type

        // constructor
        public TagData(DataTagEnum tag, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.tag = tag;
        }
    }

    // .globl tag
    public static class TagGlobl extends Tag
    {
        public TagGlobl(int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
        }
    }

    // .extern tag
    public static class TagExtern extends Tag
    {
        public TagExtern(int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
        }
    }

    // tag token specifying the beginning of a section (.data or .text)
    public static class TagSection extends Tag
    {
        // supported sections
        public enum SectionTagEnum {
            DATA, TEXT,
        }

        public SectionTagEnum tag; // section

        // constructor
        public TagSection(SectionTagEnum tag, int srcStart, int srcEnd)
        {
            super(srcStart, srcEnd);
            this.tag = tag;
        }
    }

}
