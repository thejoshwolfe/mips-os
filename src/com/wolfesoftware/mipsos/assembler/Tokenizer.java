package com.wolfesoftware.mipsos.assembler;

import java.util.ArrayList;

/**
 * provides the tokenize() function that takes a String and returns an array of Tokens
 */
public class Tokenizer
{
    public static final int MIN_HEX_CHAR_VAL = Character.getNumericValue('A');
    public static final int MAX_HEX_CHAR_VAL = Character.getNumericValue('F');

    private static final String PRMT_UnknownToken = "Unknown token.";
    private static final String PRMT_UnsupportedTag = "Unsupported tag.";
    private static final String PRMT_UnexpectedEndOfFile = "Unexpected end of file.";
    private static final String PRMT_RegisterNumberOutOfRange = "Register number out of range.";
    private static final String PRMT_UnrecognizedRegisterName = "Unrecognized register name.";
    private static final String PRMT_UnrecognizedEscapeSequence = "Unrecognized escape sequence.";
    private static final String PRMT_ExpectedHexDigit = "Expected hex digit.";


    public static Token.TokenBase[] tokenize(final String src) throws TokenizingException
    {
        ArrayList<Token.TokenBase> tokens = new ArrayList<Token.TokenBase>();
        int srcLength = src.length(); // cache line.length();

        int i = 0;
        while (i < srcLength) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) // whitespace (skip)
                i++;
            else if (c == '#') // comment (skip whole line)
            {
                do
                    i++;
                while (i < src.length() && src.charAt(i) != '\n'); // skip all chars until the newline
                i++; // skip the '\n' as well
            } else {
                Token.TokenBase token;
                // tag
                token = tokenizeTag(src, i);
                if (token != null) {
                    tokens.add(token);
                    i = token.srcEnd;
                    continue;
                }
                // register
                token = tokenizeRegister(src, i);
                if (token != null) {
                    tokens.add(token);
                    i = token.srcEnd;
                    continue;
                }
                // literal number
                token = tokenizeNumber(src, i);
                if (token != null) {
                    tokens.add(token);
                    i = token.srcEnd;
                    continue;
                }
                // literal string
                token = tokenizeString(src, i);
                if (token != null) {
                    tokens.add(token);
                    i = token.srcEnd;
                    continue;
                }
                // punctuation
                token = tokenizePunct(src, i);
                if (token != null) {
                    tokens.add(token);
                    i = token.srcEnd;
                    continue;
                }
                // instruction/label
                token = tokenizeInstrOrLabel(src, i);
                if (token != null) {
                    tokens.add(token);
                    i = token.srcEnd;
                    continue;
                }
                // unknown token
                throw new TokenizingException(i, PRMT_UnknownToken);
            }
        }

        Token.TokenBase[] tokenArray = new Token.TokenBase[tokens.size()];
        for (int j = 0; j < tokenArray.length; j++)
            tokenArray[j] = tokens.get(j);

        return tokenArray;
    }

    private static Token.TokenBase tokenizeTag(final String src, final int i) throws TokenizingException
    {
        char c = src.charAt(i);
        if (c != '.')
            return null;
        // can contain only numbers, letters, and underscores
        int j;
        for (j = i + 1; j < src.length(); j++) {
            char c2 = src.charAt(j);
            if (!(Character.isLetterOrDigit(c2) || c2 == '_'))
                break;
        }
        String tagName = src.substring(i, j).toUpperCase();
        // lookup tag name
        if (tagName.equals(".DATA"))
            return new Token.TagSection(Token.TagSection.SectionTagEnum.DATA, i, j);
        else if (tagName.equals(".TEXT"))
            return new Token.TagSection(Token.TagSection.SectionTagEnum.TEXT, i, j);
        else if (tagName.equals(".BYTE"))
            return new Token.TagData(Token.TagData.DataTagEnum.BYTE, i, j);
        else if (tagName.equals(".HALF"))
            return new Token.TagData(Token.TagData.DataTagEnum.HALF, i, j);
        else if (tagName.equals(".WORD"))
            return new Token.TagData(Token.TagData.DataTagEnum.WORD, i, j);
        else if (tagName.equals(".DWORD"))
            return new Token.TagData(Token.TagData.DataTagEnum.DWORD, i, j);
        else if (tagName.equals(".FLOAT"))
            return new Token.TagData(Token.TagData.DataTagEnum.FLOAT, i, j);
        else if (tagName.equals(".DOUBLE"))
            return new Token.TagData(Token.TagData.DataTagEnum.DOUBLE, i, j);
        else if (tagName.equals(".ASCII"))
            return new Token.TagData(Token.TagData.DataTagEnum.ASCII, i, j);
        else if (tagName.equals(".ASCIIZ"))
            return new Token.TagData(Token.TagData.DataTagEnum.ASCIIZ, i, j);
        else if (tagName.equals(".GLOBL"))
            return new Token.TagGlobl(i, j);
        else if (tagName.equals(".EXTERN"))
            return new Token.TagExtern(i, j);
        else
            // unknown tag
            throw new TokenizingException(i, PRMT_UnsupportedTag);
    }

    private static Token.TokenBase tokenizeRegister(final String src, final int i) throws TokenizingException
    {
        char c = src.charAt(i);
        if (c != '$')
            return null;
        int lineLength = src.length();
        int j = i + 1; // j tracks the end of the token (for substring(i, j))
        if (j >= lineLength)
            throw new TokenizingException(lineLength - 1, PRMT_UnexpectedEndOfFile);
        char c2 = src.charAt(j);
        byte regNum = -1;
        if (Character.isDigit(c2)) {
            // numbered register
            j = i + 2; // first digit
            if (j < lineLength && Character.isDigit(src.charAt(j)))
                j = i + 3; // two-digit register number
            regNum = Byte.parseByte(src.substring(i + 1, j));
            System.out.println(src.substring(i + 1, j) + "\n" + regNum);
            if (!(0 <= regNum && regNum <= 31))
                throw new TokenizingException(i + 1, PRMT_RegisterNumberOutOfRange);
        } else {
            // named register
            j = i + 3; // name must be 2+ chars long
            if (j > lineLength)
                throw new TokenizingException(lineLength - 1, PRMT_UnexpectedEndOfFile);
            String regName = src.substring(i + 1, j).toUpperCase();
            if (regName.equals("ZE")) {
                // $zero
                j = i + 5;
                if (j > lineLength)
                    throw new TokenizingException(lineLength - 1, PRMT_UnexpectedEndOfFile);
                regName = src.substring(i + 1, j).toUpperCase();
                if (!regName.equals("ZERO"))
                    throw new TokenizingException(i + 1, PRMT_UnrecognizedRegisterName);
                regNum = 0;
            } else if (regName.equals("AT"))
                regNum = 1;
            else if (regName.equals("V0"))
                regNum = 2;
            else if (regName.equals("V1"))
                regNum = 3;
            else if (regName.equals("A0"))
                regNum = 4;
            else if (regName.equals("A1"))
                regNum = 5;
            else if (regName.equals("A2"))
                regNum = 6;
            else if (regName.equals("A3"))
                regNum = 7;
            else if (regName.equals("T0"))
                regNum = 8;
            else if (regName.equals("T1"))
                regNum = 9;
            else if (regName.equals("T2"))
                regNum = 10;
            else if (regName.equals("T3"))
                regNum = 11;
            else if (regName.equals("T4"))
                regNum = 12;
            else if (regName.equals("T5"))
                regNum = 13;
            else if (regName.equals("T6"))
                regNum = 14;
            else if (regName.equals("T7"))
                regNum = 15;
            else if (regName.equals("S0"))
                regNum = 16;
            else if (regName.equals("S1"))
                regNum = 17;
            else if (regName.equals("S2"))
                regNum = 18;
            else if (regName.equals("S3"))
                regNum = 19;
            else if (regName.equals("S4"))
                regNum = 20;
            else if (regName.equals("S5"))
                regNum = 21;
            else if (regName.equals("S6"))
                regNum = 22;
            else if (regName.equals("S7"))
                regNum = 23;
            else if (regName.equals("T8"))
                regNum = 24;
            else if (regName.equals("T9"))
                regNum = 25;
            else if (regName.equals("K0"))
                regNum = 26;
            else if (regName.equals("K1"))
                regNum = 27;
            else if (regName.equals("GP"))
                regNum = 28;
            else if (regName.equals("SP"))
                regNum = 29;
            else if (regName.equals("FP"))
                regNum = 30;
            else if (regName.equals("RA"))
                regNum = 31;
            else
                throw new TokenizingException(i + 1, PRMT_UnrecognizedRegisterName);
        }
        return new Token.Register(regNum, i, j);
    }

    private static Token.TokenBase tokenizeNumber(final String src, final int i) throws TokenizingException
    {
        int numStart = i;
//		int lineLength = src.length();
        char c = src.charAt(numStart);
        if (!(c == '-' || Character.isDigit(c)))
            return null;

        // literal number
        // check for negative
        boolean negative = false;
        if (c == '-') {
            negative = true;
            // skip the "-"
            numStart++;
            if (numStart >= src.length())
                throw new TokenizingException(src.length() - 1, PRMT_UnexpectedEndOfFile);
            c = src.charAt(numStart);
            if (!Character.isDigit(c))
                return null;
        }
        // check for hex
        if (numStart + 1 < src.length() && (c == '0' && src.charAt(numStart + 1) == 'x')) {
            // hex
            numStart += 2; // skip the "0x"
            if (numStart >= src.length())
                throw new TokenizingException(src.length() - 1, PRMT_UnexpectedEndOfFile);
            // find the end of the number
            int j;
            for (j = numStart; j < src.length(); j++) {
                char digit = src.charAt(j);
                if (Character.isDigit(digit))
                    continue; // digits are still part of the number
                int dVal = Character.getNumericValue(Character.toUpperCase(digit));
                if (MIN_HEX_CHAR_VAL <= dVal && dVal <= MAX_HEX_CHAR_VAL)
                    continue; // hex is still part of the number
                break;
            }
            if (j == numStart)
                throw new TokenizingException(j - 1, PRMT_ExpectedHexDigit);
            return new Token.LiteralLong((negative ? -1 : 1) * Long.parseLong(src.substring(numStart, j), 16), i, j);
        } else {
            // dec
            boolean floating = false;
            // find the end of the number
            int j;
            for (j = numStart; j < src.length(); j++) {
                char digit = src.charAt(j);
                if (Character.isDigit(digit))
                    continue; // digits are still part of the number
                if ((!floating) && digit == '.') {
                    floating = true;
                    continue;
                }
                break; // character is not part of the number
            }
            if (floating) {
                // double
                return new Token.LiteralDouble((negative ? -1.0 : 1.0) * Double.parseDouble(src.substring(numStart, j)), i, j);
            } else {
                // long
                return new Token.LiteralLong((negative ? -1 : 1) * Long.parseLong(src.substring(numStart, j)), i, j);
            }
        }
    }

    private static Token.TokenBase tokenizeString(final String src, final int i) throws TokenizingException
    {
        if (src.charAt(i) != '\"')
            return null;
        int j = i + 1; // skip the first "
        String string = "";
        boolean escaping = false;
        while (j < src.length()) {
            char cj = src.charAt(j);
            if (escaping) {
                // the second character of an escape sequence
                switch (cj) // cj is the character after the \
                {
                    case '\'':
                        string += '\'';
                        break;
                    case '\"':
                        string += '\"';
                        break;
                    case '\\':
                        string += '\\';
                        break;
                    case '0':
                        string += '\0';
                        break;
                    case 'b':
                        string += '\b';
                        break;
                    case 'f':
                        string += '\f';
                        break;
                    case 'n':
                        string += '\n';
                        break;
                    case 'r':
                        string += '\r';
                        break;
                    case 't':
                        string += '\t';
                        break;
                    default:
                        throw new TokenizingException(j + 1, PRMT_UnrecognizedEscapeSequence);
                }
                escaping = false;
            } else if (cj == '\\')
                // beginning an escape sequence
                escaping = true;
            else if (cj == '\"')
                // finished the literal
                return new Token.StringClass(string, i, j + 1);
            else
                // normal character
                string += cj;

            j++;
        }
        throw new TokenizingException(src.length() - 1, PRMT_UnexpectedEndOfFile);
    }

    private static Token.TokenBase tokenizePunct(final String src, final int i)
    {
        Token.PunctClass.PunctEnum punct;
        switch (src.charAt(i)) {
            case ',':
                punct = Token.PunctClass.PunctEnum.COMMA;
                break;
            case ':':
                punct = Token.PunctClass.PunctEnum.COLON;
                break;
            case '(':
                punct = Token.PunctClass.PunctEnum.OPEN_PARENTH;
                break;
            case ')':
                punct = Token.PunctClass.PunctEnum.CLOSE_PARENTH;
                break;
            default:
                return null;
        }
        return new Token.PunctClass(punct, i, i + 1);
    }

    private static Token.TokenBase tokenizeInstrOrLabel(final String src, final int i)
    {
        if (!Character.isLetterOrDigit(src.charAt(i)))
            return null;
        // instruction or label
        int j = i + 1;
        while (true) {
            if (j >= src.length())
                break; // found the end of the token
            char cj = src.charAt(j);
            if (!(Character.isLetterOrDigit(cj) || cj == '_'))
                break; // found the end of the token
            j++;
        }
        String token = src.substring(i, j);
        Token.InstrName.InstrEnum instr;

        String tokenUpper = token.toUpperCase(); // instructions are case insensitive
        if (tokenUpper.equals("ADD"))
            instr = Token.InstrName.InstrEnum.ADD;
        else if (tokenUpper.equals("ADDI"))
            instr = Token.InstrName.InstrEnum.ADDI;
        else if (tokenUpper.equals("AND"))
            instr = Token.InstrName.InstrEnum.AND;
        else if (tokenUpper.equals("ANDI"))
            instr = Token.InstrName.InstrEnum.ANDI;
        else if (tokenUpper.equals("BEQ"))
            instr = Token.InstrName.InstrEnum.BEQ;
        else if (tokenUpper.equals("BNE"))
            instr = Token.InstrName.InstrEnum.BNE;
        else if (tokenUpper.equals("BREAK"))
            instr = Token.InstrName.InstrEnum.BREAK;
        else if (tokenUpper.equals("DIV"))
            instr = Token.InstrName.InstrEnum.DIV;
        else if (tokenUpper.equals("J"))
            instr = Token.InstrName.InstrEnum.J;
        else if (tokenUpper.equals("JAL"))
            instr = Token.InstrName.InstrEnum.JAL;
        else if (tokenUpper.equals("JALR"))
            instr = Token.InstrName.InstrEnum.JALR;
        else if (tokenUpper.equals("JR"))
            instr = Token.InstrName.InstrEnum.JR;
        else if (tokenUpper.equals("LB"))
            instr = Token.InstrName.InstrEnum.LB;
        else if (tokenUpper.equals("LH"))
            instr = Token.InstrName.InstrEnum.LH;
        else if (tokenUpper.equals("LUI"))
            instr = Token.InstrName.InstrEnum.LUI;
        else if (tokenUpper.equals("LW"))
            instr = Token.InstrName.InstrEnum.LW;
        else if (tokenUpper.equals("MFHI"))
            instr = Token.InstrName.InstrEnum.MFHI;
        else if (tokenUpper.equals("MFLO"))
            instr = Token.InstrName.InstrEnum.MFLO;
        else if (tokenUpper.equals("MTHI"))
            instr = Token.InstrName.InstrEnum.MTHI;
        else if (tokenUpper.equals("MTLO"))
            instr = Token.InstrName.InstrEnum.MTLO;
        else if (tokenUpper.equals("MULT"))
            instr = Token.InstrName.InstrEnum.MULT;
        else if (tokenUpper.equals("NOP"))
            instr = Token.InstrName.InstrEnum.NOP;
        else if (tokenUpper.equals("NOR"))
            instr = Token.InstrName.InstrEnum.NOR;
        else if (tokenUpper.equals("OR"))
            instr = Token.InstrName.InstrEnum.OR;
        else if (tokenUpper.equals("ORI"))
            instr = Token.InstrName.InstrEnum.ORI;
        else if (tokenUpper.equals("SB"))
            instr = Token.InstrName.InstrEnum.SB;
        else if (tokenUpper.equals("SH"))
            instr = Token.InstrName.InstrEnum.SH;
        else if (tokenUpper.equals("SLL"))
            instr = Token.InstrName.InstrEnum.SLL;
        else if (tokenUpper.equals("SLLV"))
            instr = Token.InstrName.InstrEnum.SLLV;
        else if (tokenUpper.equals("SLT"))
            instr = Token.InstrName.InstrEnum.SLT;
        else if (tokenUpper.equals("SLTI"))
            instr = Token.InstrName.InstrEnum.SLTI;
        else if (tokenUpper.equals("SRA"))
            instr = Token.InstrName.InstrEnum.SRA;
        else if (tokenUpper.equals("SRAV"))
            instr = Token.InstrName.InstrEnum.SRAV;
        else if (tokenUpper.equals("SRL"))
            instr = Token.InstrName.InstrEnum.SRL;
        else if (tokenUpper.equals("SRLV"))
            instr = Token.InstrName.InstrEnum.SRLV;
        else if (tokenUpper.equals("SUB"))
            instr = Token.InstrName.InstrEnum.SUB;
        else if (tokenUpper.equals("SW"))
            instr = Token.InstrName.InstrEnum.SW;
        else if (tokenUpper.equals("SYSCALL"))
            instr = Token.InstrName.InstrEnum.SYSCALL;
        else if (tokenUpper.equals("XOR"))
            instr = Token.InstrName.InstrEnum.XOR;
        else if (tokenUpper.equals("XORI"))
            instr = Token.InstrName.InstrEnum.XORI;
        else if (tokenUpper.equals("BGE"))
            instr = Token.InstrName.InstrEnum.BGE;
        else if (tokenUpper.equals("BGEZ"))
            instr = Token.InstrName.InstrEnum.BGEZ;
        else if (tokenUpper.equals("BGT"))
            instr = Token.InstrName.InstrEnum.BGT;
        else if (tokenUpper.equals("BGTZ"))
            instr = Token.InstrName.InstrEnum.BGTZ;
        else if (tokenUpper.equals("BLE"))
            instr = Token.InstrName.InstrEnum.BLE;
        else if (tokenUpper.equals("BLEZ"))
            instr = Token.InstrName.InstrEnum.BLEZ;
        else if (tokenUpper.equals("BLT"))
            instr = Token.InstrName.InstrEnum.BLT;
        else if (tokenUpper.equals("BLTZ"))
            instr = Token.InstrName.InstrEnum.BLTZ;
        else if (tokenUpper.equals("LA"))
            instr = Token.InstrName.InstrEnum.LA;
        else if (tokenUpper.equals("LI"))
            instr = Token.InstrName.InstrEnum.LI;
        else if (tokenUpper.equals("MOVE"))
            instr = Token.InstrName.InstrEnum.MOVE;
        else {
            // label
            return new Token.Label(token, i, j); // labels are case sensitive
        }
        // instruction
        return new Token.InstrName(instr, i, j);
    }

    public static class Tokenization
    {
        public final Token.TokenBase[] tokens;
        public final String fullSrc;

        public Tokenization(Token.TokenBase[] tokens, String fullSrc)
        {
            this.tokens = tokens;
            this.fullSrc = fullSrc;
        }
    }

}
