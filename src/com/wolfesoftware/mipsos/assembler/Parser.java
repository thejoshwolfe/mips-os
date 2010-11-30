package com.wolfesoftware.mipsos.assembler;

import java.util.*;



// provides the parse() function for parsing an array of tokens
public class Parser
{
	// All different variations of argument syntax for instructions.
	// L = Label; R = Register; [number] = [number]-Bit Literal; C = Comma; P = Parenth
	// See comments on switch cases in parseInstr() for examples.
	private static enum SyntEnum {
		None, L, R, RC16, RC16PRP, RC32, RCL, RCR, RCRC16, RCRC5, RCRCL, RCRCR, 
	}

	private static final String PRMT_ExpectedCloseParenth = "Expected \")\".";
	private static final String PRMT_ExpectedColonAfterLabel = "Expected \":\" after label";
	private static final String PRMT_ExpectedComma = "Expected \",\".";
	private static final String PRMT_ExpectedLabel = "Expected label.";
	private static final String PRMT_ExpectedLiteralNumber = "Expected literal number.";
	private static final String PRMT_ExpectedLiteralString = "Expected literal string.";
	private static final String PRMT_ExpectedOpenParenth = "Expected \"(\".";
	private static final String PRMT_ExpectedRegister = "Expected register.";
	private static final String PRMT_IllegalStartOfStatement = "Illegal start of statement.";
	private static final String PRMT_InvalidPunctuation = "Invalid punctuation.";
	private static final String PRMT_NumberOfRepeatsMustBeIntegral = "Number of repeats must be integral.";
	private static final String PRMT_NumberOfRepeatsOutOfRange = "Number of repeats out of range.";
	private static final String PRMT_NumberOutOfRange = "Number out of range.";
	private static final String PRMT_TypeMismatch = "Type mismatch.";
	private static final String PRMT_UnexpectedEndOfFile = "Unexpected end of file.";
	private static final String PRMT_UnsupportedDataType = "Unsupported data type.";
	private static final String PRMT_UnsupportedSection = "Unsupported section.";
	private static final String PRMT_UnsupportedSyntaxFormat = "Unsupported syntax format.";
	// the primary parse method. Takes an array of tokens and assembler options and 
	// outputs a Binarization containing arrays of binary elements.
	public static Binarization parse(Token.TokenBase[] tokens, int dataStartAddress, int textStartAddress) throws ParsingException
    {
        ArrayList<Bin.BinBase> dataElems = new ArrayList<Bin.BinBase>(); // accumulates binary elements in the .data section
        ArrayList<Bin.BinBase> textElems = new ArrayList<Bin.BinBase>(); // accumulates binary elements in the .text section

        HashMap<String, Long> labels = new HashMap<String, Long>(); // stores the addresses of labels

        int i = 0; // iterates tokens[i]
        Token.TagSection.SectionTagEnum section = // current section
        Token.TagSection.SectionTagEnum.TEXT; // default is .text section

        int runningDataAddress = dataStartAddress;
        int runningTextAddress = textStartAddress;
        // iterate through every token in the array
        while (i < tokens.length) {
            switch (section) {
                case DATA: // .data specific statements
                    // check for .data Label declaration
                    Bin.Label binDataLabel = parseLabel(tokens, i);
                    if (binDataLabel != null) {
                        // found .data Label declaration
                        if (labels.containsKey(binDataLabel.labelName))
                            throw new ParsingException(i - 1, "Duplicate label: " + binDataLabel.labelName + ".");
                        labels.put(binDataLabel.labelName, (long)runningDataAddress);
                        dataElems.add(binDataLabel);
                        i = binDataLabel.tokenEnd;
                        continue;
                    }
                    // check for data tags (.byte, .dword, .double, .asciiz, etc.)
                    Bin.Data binData = parseData(tokens, i);
                    if (binData != null) {
                        // found a data tag
                        dataElems.add(binData);
                        runningDataAddress += binData.getBinLen();
                        i = binData.tokenEnd;
                        continue;
                    }
                    break;
                case TEXT: // .text specific statements
                    // check for instruction(s)
                    Bin.BinBase binInstrs = parseInstr(tokens, i);
                    if (binInstrs != null) {
                        // found an instruction
                        textElems.add(binInstrs);
                        runningTextAddress += binInstrs.getBinLen();
                        i = binInstrs.tokenEnd;
                        continue;
                    }
                    // check for .text Label declarations
                    Bin.Label binTextLabel = parseLabel(tokens, i);
                    if (binTextLabel != null) {
                        // found a .text Label declaration
                        if (labels.containsKey(binTextLabel.labelName))
                            throw new ParsingException(i - 1, "Duplicate label: " + binTextLabel.labelName + ".");
                        labels.put(binTextLabel.labelName, (long)runningTextAddress);
                        textElems.add(binTextLabel);
                        i = binTextLabel.tokenEnd;
                        continue;
                    }
                    break;
                default:
                    // unknown section
                    throw new ParsingException(i - 1, PRMT_UnsupportedSection);
            }
            // non-section-specific statements

            // check for section tags (.data, .text)
            if (tokens[i] instanceof Token.TagSection) {
                // found a section tag
                section = ((Token.TagSection)tokens[i]).tag; // change sections
                i += 1;
                continue;
            }

            throw new ParsingException(i, PRMT_IllegalStartOfStatement);
        }

        // construct Binarization and return
        Bin.BinBase[] dataArray = dataElems.toArray(new Bin.BinBase[dataElems.size()]);
        Bin.BinBase[] textArray = textElems.toArray(new Bin.BinBase[textElems.size()]);
        return new Binarization(dataArray, textArray, labels, dataStartAddress, textStartAddress);
    }

	// attempts to parse a label declaration. ("labelName" + ":")
	// Returns null on failure. 
	private static Bin.Label parseLabel(Token.TokenBase[] tokens, int i) throws ParsingException
	{
		if (!(tokens[i] instanceof Token.Label)) // check first token type
			return null;
		if (i + 1 >= tokens.length) // check length
			throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
		if (!((tokens[i + 1] instanceof Token.PunctClass) && 
				((Token.PunctClass)tokens[i + 1]).punct == Token.PunctClass.PunctEnum.COLON)) // check for a ":"
			throw new ParsingException(i + 1, PRMT_ExpectedColonAfterLabel);
		return new Bin.Label(((Token.Label)tokens[i]).labelName, i, i + 2);
	}

	// attempts to parse literal data. (".dataType" + "6" + "," + "2" + etc.)
	// Returns null on early failure. 
	private static Bin.Data parseData(Token.TokenBase[] tokens, int i) throws ParsingException
	{
		if (!(tokens[i] instanceof Token.TagData)) // check token type
			return null;
		Token.TagData.DataTagEnum dataType = ((Token.TagData)tokens[i]).tag; // cache the dataType
		boolean floating = true; // floating numerical literal (see switch structure below)
		switch (dataType)
		{
		case BYTE:
		case HALF:
		case WORD:
		case DWORD: floating = false; // mark floating as false, and execute the code for case DOUBLE:
		case FLOAT:
		case DOUBLE:
			// numeric literal
			if (i + 1 >= tokens.length) // must have at least 1 number
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Literal)) // check that there's a number after the '.dataType '
				throw new ParsingException(i + 1, PRMT_ExpectedLiteralNumber);
			Token.Literal firstLiteral = (Token.Literal)tokens[i + 1]; // cache the first number token
			if (!isValidLiteral(dataType, firstLiteral)) // check the type and range
				throw new ParsingException(i + 1, PRMT_TypeMismatch);
			if (i + 2 < tokens.length && 
					tokens[i + 2] instanceof Token.PunctClass) // check for multiple or single number
			{
				// multiple numbers
				switch (((Token.PunctClass)tokens[i + 2]).punct) // check for repeat format, or list format
				{
				case COLON:
					// repeat format (eg: "0xFF:32")
					if (i + 3 >= tokens.length) // must have at least 1 number
						throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
					if (!(tokens[i + 3] instanceof Token.LiteralLong)) // number of reps must be integral
						throw new ParsingException(i + 3, PRMT_NumberOfRepeatsMustBeIntegral);
					long reps = ((Token.LiteralLong)tokens[i + 3]).value;
					if (!(0 <= reps && reps <= Short.MAX_VALUE)) // arbitrary max. how about only 2^15 reps.
						throw new ParsingException(i + 3, PRMT_NumberOfRepeatsOutOfRange);
					if (!floating)
					{
						// repeating bytes, shorts, ints, or longs
						if (!(firstLiteral instanceof Token.LiteralLong)) // check type of first literal
							throw new ParsingException(i + 1, PRMT_TypeMismatch);
						return new Bin.Data(repeatBytes(Bin.Data.convertIntegral(dataType, new long[] {
								((Token.LiteralLong)firstLiteral).value}), (int)reps), i, i + 4);
					}
					else
					{
						// repeating floats or doubles
						if (!(tokens[i + 1] instanceof Token.LiteralDouble)) // check type of first literal
							throw new ParsingException(i + 1, PRMT_TypeMismatch);
						return new Bin.Data(repeatBytes(Bin.Data.convertFloating(dataType, new double[] {
								((Token.LiteralDouble)tokens[i + 1]).value}), (int)reps), i, i + 4);
					}
				case COMMA:
					// comma delimited list (eg: "1, 2, 3")
					ArrayList<Token.Literal> literalTokens = new ArrayList<Token.Literal>();
					literalTokens.add(firstLiteral);
					int j = i + 1;
					do
					{
						j += 2;
						if (!(tokens[j] instanceof Token.Literal)) // check type of number token
							throw new ParsingException(j, PRMT_ExpectedLiteralNumber);
						if (!isValidLiteral(dataType, (Token.Literal)tokens[j])) // validate number
							throw new ParsingException(j, PRMT_TypeMismatch);
						literalTokens.add((Token.Literal)tokens[j]); // accumulate
						if (j + 2 >= tokens.length)
							break;
					} while (tokens[j + 1] instanceof Token.PunctClass &&
							((Token.PunctClass)tokens[j + 1]).punct == Token.PunctClass.PunctEnum.COMMA); // check next token for punctuation
					if (floating)
					{
						// list of floating point numbers
						// extract array of doubles from ArrayList of tokens
						double[] doubles = new double[literalTokens.size()]; 
						for (int k = 0; k < doubles.length; k++)
							doubles[k] = ((Token.LiteralDouble)literalTokens.get(k)).value; // type here was already checked by isValidLiteral() above
						return new Bin.Data(Bin.Data.convertFloating(dataType, doubles), i, j + 1);
					}
					else
					{
						// list of integral numbers
						// extract array of longs from ArrayList of tokens
						long[] longs = new long[literalTokens.size()];
						for (int k = 0; k < longs.length; k++)
							longs[k] = ((Token.LiteralLong)literalTokens.get(k)).value; // type here was already checked by isValidLiteral() above
						return new Bin.Data(Bin.Data.convertIntegral(dataType, longs), i, j + 1);
					}
				default:
					throw new ParsingException(i + 2, PRMT_InvalidPunctuation);
				}
			}
			else
			{
				// single number
				if (!isValidLiteral(dataType, firstLiteral)) // validate number
					throw new ParsingException(i + 1, PRMT_TypeMismatch);
				if (!floating)
				{
					// single byte, short, int, or long
					return new Bin.Data(Bin.Data.convertIntegral(dataType, 
							new long[] {((Token.LiteralLong)firstLiteral).value}), i, i + 2);
				}
				else
				{
					// single float or double
					return new Bin.Data(Bin.Data.convertFloating(dataType, 
							new double[] {((Token.LiteralDouble)firstLiteral).value}), i, i + 2);
				}
			}
		case ASCII:
		case ASCIIZ:
			// string literal. 
			if (i + 1 >= tokens.length) // check length
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.StringClass)) // check type of the string token
				throw new ParsingException(i + 1, PRMT_ExpectedLiteralString);
			byte[] stringData;
			if (dataType == Token.TagData.DataTagEnum.ASCII)
				// .ascii
				stringData = ByteUtils.convertAscii(((Token.StringClass)tokens[i + 1]).string);
			else
				// .asciiz
				stringData = ByteUtils.convertAsciiz(((Token.StringClass)tokens[i + 1]).string);
			return new Bin.Data(stringData, i, i + 2);
		default:
			throw new ParsingException(i, PRMT_UnsupportedDataType);
		}
	}

	// attempts to parse an instruction. (eg: "lw" + "$s0" + "," + "8" + "(" + "$sp" + ")")
	// Returns null on early failure. 
	private static Bin.BinBase parseInstr(Token.TokenBase[] tokens, int i) throws ParsingException
	{
		// check first token type
		if (!(tokens[i] instanceof Token.InstrName))
			return null; // not an instruction at all.
		Token.InstrName.InstrEnum instr = ((Token.InstrName)tokens[i]).instr; // cache the instruction enum value
		Token.LiteralLong tokenLong = null; // to be used later
		// validate the next few tokens according to this instruction's syntax format
		// and call a generateXXX() method with the appropriate paramenters
		switch (instrToSynt(instr))
		{
		case None: // eg: syscall
			return generate(instr, i, i + 1);
		case L: // eg: j label
			if (i + 1 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Label)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedLabel);
			return generateL(instr, i, i + 2, (Token.Label)tokens[i + 1]);
		case R: // eg: jr $1
			if (i + 1 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			return generateR(instr, i, i + 2, (Token.Register)tokens[i + 1]);
		case RC16: // eg: lui $1, 1000
			if (i + 3 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.LiteralLong)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedLiteralNumber);
			tokenLong = (Token.LiteralLong)tokens[i + 3];
			if (!(-0x8000 <= tokenLong.value && tokenLong.value <= 0x7FFF)) // check range
				throw new ParsingException(i + 3, PRMT_NumberOutOfRange);
			return generateRI(instr, i, i + 4, (Token.Register)tokens[i + 1], tokenLong);
		case RC16PRP: // eg: sw $1, 8($2)
			if (i + 6 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.LiteralLong)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedLiteralNumber);
			tokenLong = (Token.LiteralLong)tokens[i + 3];
			if (!(-0x8000 <= tokenLong.value && tokenLong.value <= 0x7FFF)) // check range
				throw new ParsingException(i + 3, PRMT_NumberOutOfRange);
			if (!(tokens[i + 4] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 4]).punct == Token.PunctClass.PunctEnum.OPEN_PARENTH)) // check punctuation
				throw new ParsingException(i + 4, PRMT_ExpectedOpenParenth);
			if (!(tokens[i + 5] instanceof Token.Register)) // check type
				throw new ParsingException(i + 5, PRMT_ExpectedRegister);
			if (!(tokens[i + 6] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 6]).punct == Token.PunctClass.PunctEnum.CLOSE_PARENTH)) // check punctuation
				throw new ParsingException(i + 6, PRMT_ExpectedCloseParenth);
			return generateRRI(instr, i, i + 7, (Token.Register)tokens[i + 1], (Token.Register)tokens[i + 5], tokenLong);
		case RC32: // eg: li $1, 0x01234567
			if (i + 3 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.LiteralLong)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedLiteralNumber);
			tokenLong = (Token.LiteralLong)tokens[i + 3];
			if (!(-0x80000000 <= tokenLong.value && tokenLong.value <= 0x7FFFFFFF)) // check range
				throw new ParsingException(i + 3, PRMT_NumberOutOfRange);
			return generateRI(instr, i, i + 4, (Token.Register)tokens[i + 1], tokenLong);
		case RCL: // eg: la $1, label
			if (i + 3 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.Label)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedLabel);
			return generateRL(instr, i, i + 4, (Token.Register)tokens[i + 1], (Token.Label)tokens[i + 3]);
		case RCR: // eg: div $1, $2
			if (i + 3 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.Register)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedRegister);
			return generateRR(instr, i, i + 4, (Token.Register)tokens[i + 1], (Token.Register)tokens[i + 3]);
		case RCRC16: // eg: addi $1, $2, 512
			if (i + 5 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.Register)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedRegister);
			if (!(tokens[i + 4] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 4]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 4, PRMT_ExpectedComma);
			if (!(tokens[i + 5] instanceof Token.LiteralLong)) // check type
				throw new ParsingException(i + 5, PRMT_ExpectedLiteralNumber);
			tokenLong = (Token.LiteralLong)tokens[i + 5];
			if (!(-0x8000 <= tokenLong.value && tokenLong.value <= 0x7FFF)) // check range
				throw new ParsingException(i + 5, PRMT_NumberOutOfRange);
			return generateRRI(instr, i, i + 6, (Token.Register)tokens[i + 1], (Token.Register)tokens[i + 3], tokenLong);
		case RCRC5: // eg: sll $1, $2, 16
			if (i + 5 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.Register)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedRegister);
			if (!(tokens[i + 4] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 4]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 4, PRMT_ExpectedComma);
			if (!(tokens[i + 5] instanceof Token.LiteralLong)) // check type
				throw new ParsingException(i + 5, PRMT_ExpectedLiteralNumber);
			tokenLong = (Token.LiteralLong)tokens[i + 5];
			if (!(0 <= tokenLong.value && tokenLong.value <= 0x1F)) // check range
				throw new ParsingException(i + 5, PRMT_NumberOutOfRange);
			return generateRRI(instr, i, i + 6, (Token.Register)tokens[i + 1], (Token.Register)tokens[i + 3], tokenLong);
		case RCRCL: // eg: bne $1, $2, label
			if (i + 5 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.Register)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedRegister);
			if (!(tokens[i + 4] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 4]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 4, PRMT_ExpectedComma);
			if (!(tokens[i + 5] instanceof Token.Label)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedLabel);
			return generateRRL(instr, i, i + 6, (Token.Register)tokens[i + 1], (Token.Register)tokens[i + 3], (Token.Label)tokens[i + 5]);
		case RCRCR: // eg: add $1, $2, $3
			if (i + 5 >= tokens.length) // check for enough tokens
				throw new ParsingException(tokens.length - 1, PRMT_UnexpectedEndOfFile);
			if (!(tokens[i + 1] instanceof Token.Register)) // check type
				throw new ParsingException(i + 1, PRMT_ExpectedRegister);
			if (!(tokens[i + 2] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 2]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 2, PRMT_ExpectedComma);
			if (!(tokens[i + 3] instanceof Token.Register)) // check type
				throw new ParsingException(i + 3, PRMT_ExpectedRegister);
			if (!(tokens[i + 4] instanceof Token.PunctClass && 
					((Token.PunctClass)tokens[i + 4]).punct == Token.PunctClass.PunctEnum.COMMA)) // check punctuation
				throw new ParsingException(i + 4, PRMT_ExpectedComma);
			if (!(tokens[i + 5] instanceof Token.Register)) // check type
				throw new ParsingException(i + 5, PRMT_ExpectedRegister);
			return generateRRR(instr, i, i + 6, (Token.Register)tokens[i + 1], (Token.Register)tokens[i + 3], (Token.Register)tokens[i + 5]);
		default: // unknown syntax format
			throw new ParsingException(i, PRMT_UnsupportedSyntaxFormat);
		}
	}
	
	// returns the syntax format of the passed instruction
	private static SyntEnum instrToSynt(Token.InstrName.InstrEnum instr)
	{
	    // see Instr declaration for an explanation of the syntax format
	    switch (instr) {
	        case ADD:     return SyntEnum.RCRCR;
	        case ADDI:    return SyntEnum.RCRC16;
	        case AND:     return SyntEnum.RCRCR;
	        case ANDI:    return SyntEnum.RCRC16;
	        case BEQ:     return SyntEnum.RCRCL;
	        case BNE:     return SyntEnum.RCRCL;
	        case BREAK:   return SyntEnum.None;
	        case DIV:     return SyntEnum.RCR;
	        case J:       return SyntEnum.L;
	        case JAL:     return SyntEnum.L;
	        case JALR:    return SyntEnum.RCR;
	        case JR:      return SyntEnum.R;
	        case LB:      return SyntEnum.RC16PRP;
	        case LH:      return SyntEnum.RC16PRP;
	        case LUI:     return SyntEnum.RC16;
	        case LW:      return SyntEnum.RC16PRP;
	        case MFHI:    return SyntEnum.R;
	        case MFLO:    return SyntEnum.R;
	        case MTHI:    return SyntEnum.R;
	        case MTLO:    return SyntEnum.R;
            case MUL:     return SyntEnum.RCRCR;
	        case MULT:    return SyntEnum.RCR;
	        case NOP:     return SyntEnum.None;
	        case NOR:     return SyntEnum.RCRCR;
	        case OR:      return SyntEnum.RCRCR;
	        case ORI:     return SyntEnum.RCRC16;
	        case SB:      return SyntEnum.RC16PRP;
	        case SH:      return SyntEnum.RC16PRP;
	        case SLL:     return SyntEnum.RCRC5;
	        case SLLV:    return SyntEnum.RCRCR;
	        case SLT:     return SyntEnum.RCRCR;
	        case SLTI:    return SyntEnum.RCRC16;
	        case SRA:     return SyntEnum.RCRC5;
	        case SRAV:    return SyntEnum.RCRCR;
	        case SRL:     return SyntEnum.RCRC5;
	        case SRLV:    return SyntEnum.RCRCR;
	        case SUB:     return SyntEnum.RCRCR;
	        case SW:      return SyntEnum.RC16PRP;
	        case SYSCALL: return SyntEnum.None;
	        case XOR:     return SyntEnum.RCRCR;
	        case XORI:    return SyntEnum.RCRC16;
	        case BGE:     return SyntEnum.RCRCL;
	        case BGEZ:    return SyntEnum.RCL;
	        case BGT:     return SyntEnum.RCRCL;
	        case BGTZ:    return SyntEnum.RCL;
	        case BLE:     return SyntEnum.RCRCL;
	        case BLEZ:    return SyntEnum.RCL;
	        case BLT:     return SyntEnum.RCRCL;
	        case BLTZ:    return SyntEnum.RCL;
	        case LA:      return SyntEnum.RCL;
	        case LI:      return SyntEnum.RC32;
	        case MOVE:    return SyntEnum.RCR;
	        default:      return null; // should never happen
	    }
	}

	// generates a binary element with no operands
	private static Bin.BinBase generate(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd)
	{
		switch (instr)
		{
		case NOP:
		case SYSCALL:
		case BREAK:
			return new Bin.InstrR(instr, 0, 0, 0, 0, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with a label operand
	private static Bin.BinBase generateL(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Label lab)
	{
		switch (instr)
		{
		case J:
		case JAL:
			return new Bin.InstrJ(instr, lab.labelName, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with a register operand
	private static Bin.BinBase generateR(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg)
	{
		switch (instr)
		{
		case JR:
		case MTHI:
		case MTLO:
			return new Bin.InstrR(instr, reg.regNum, 0, 0, 0, tokenStart, tokenEnd);
		case MFHI:
		case MFLO:
			return new Bin.InstrR(instr, 0, 0, reg.regNum, 0, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with a register operand and an immediate operand
	private static Bin.BinBase generateRI(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg, Token.LiteralLong imm)
	{
		switch (instr)
		{
		case LUI:
			return new Bin.InstrI(instr, 0, reg.regNum, (int)imm.value, tokenStart, tokenEnd);
		case LI:
//			 TODO: check for only 16 bit immediate and use "addi $[r], $zero, [imm]" instead of two instructions
			return new Bin.Pseudo(new Bin.Instr[] {
					new Bin.InstrI(Token.InstrName.InstrEnum.LUI,  0,          1, (int)((imm.value & 0xFFFF0000) >> 16), tokenStart, tokenEnd),
					new Bin.InstrI(Token.InstrName.InstrEnum.XORI, 1, reg.regNum, (int)((imm.value & 0x0000FFFF) >>  0), tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with a register operand and a label operand
	private static Bin.BinBase generateRL(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg, Token.Label lab)
	{
		switch (instr)
		{
		case BGEZ: // !(a < 0)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT, reg.regNum,          0, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BEQ, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case BGTZ: //  (0 < a)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT,          0, reg.regNum, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BNE, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case BLEZ: // !(0 < a)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT,          0, reg.regNum, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BEQ, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case BLTZ: //  (a < 0)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT, reg.regNum,          0, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BNE, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case LA:
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrIUpper(Token.InstrName.InstrEnum.LUI,  0,          1, lab.labelName, tokenStart, tokenEnd),
					new Bin.InstrIHalf (Token.InstrName.InstrEnum.XORI, 1, reg.regNum, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with two register operands
	private static Bin.BinBase generateRR(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg1, Token.Register reg2)
	{
		switch (instr)
		{
		case JALR:
			return new Bin.InstrR(instr, reg2.regNum, 0, reg1.regNum, 0, tokenStart, tokenEnd);
		case MULT:
		case DIV:
			return new Bin.InstrR(instr, reg1.regNum, reg2.regNum, 0, 0, tokenStart, tokenEnd);
		case MOVE:
			return new Bin.InstrI(Token.InstrName.InstrEnum.ADDI, reg2.regNum, reg1.regNum, 0, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with two register operands and an immediate operand
	private static Bin.BinBase generateRRI(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg1, Token.Register reg2, Token.LiteralLong imm)
	{
		switch (instr)
		{
		case LB:
		case LH:
		case LW:
		case SB:
		case SH:
		case SW:
		case ADDI:
		case SLTI:
		case ANDI:
		case ORI:
		case XORI:
			return new Bin.InstrI(instr, reg2.regNum, reg1.regNum, (int)imm.value, tokenStart, tokenEnd);
		case SLL:
		case SRL:
		case SRA:
			return new Bin.InstrR(instr, 0, reg2.regNum, reg1.regNum, (int)imm.value, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with two register operands and a label operand
	private static Bin.BinBase generateRRL(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg1, Token.Register reg2, Token.Label lab)
	{
		switch (instr)
		{
		case BEQ:
		case BNE:
			return new Bin.InstrIBranch(instr, reg1.regNum, reg2.regNum, lab.labelName, tokenStart, tokenEnd);
		case BGE: // !(a < b)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT, reg1.regNum, reg2.regNum, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BEQ, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case BGT: //  (b < a)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT, reg2.regNum, reg1.regNum, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BNE, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case BLE: // !(b < a)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT, reg2.regNum, reg1.regNum, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BEQ, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		case BLT: //  (a < b)
			return new Bin.Pseudo (new Bin.Instr[] {
					new Bin.InstrR(Token.InstrName.InstrEnum.SLT, reg1.regNum, reg2.regNum, 1, 0, tokenStart, tokenEnd),
					new Bin.InstrIBranch(Token.InstrName.InstrEnum.BNE, 1, 0, lab.labelName, tokenStart, tokenEnd),
			}, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// generates a binary element with three register operands
	private static Bin.BinBase generateRRR(Token.InstrName.InstrEnum instr, int tokenStart, int tokenEnd, 
			Token.Register reg1, Token.Register reg2, Token.Register reg3)
	{
		switch (instr)
		{
		case SLLV:
		case SRLV:
		case SRAV:
			return new Bin.InstrR(instr, reg1.regNum, reg3.regNum, reg2.regNum, 0, tokenStart, tokenEnd);
		case ADD:
		case SUB:
		case AND:
		case OR:
		case XOR:
		case NOR:
		case SLT:
			return new Bin.InstrR(instr, reg2.regNum, reg3.regNum, reg1.regNum, 0, tokenStart, tokenEnd);
		case MUL:
            return new Bin.Pseudo(new Bin.Instr[] {
                    new Bin.InstrR(Token.InstrName.InstrEnum.MULT, reg2.regNum, reg3.regNum, 0, 0, tokenStart, tokenEnd),
                    new Bin.InstrR(Token.InstrName.InstrEnum.MFLO, 0, 0, reg1.regNum, 0, tokenStart, tokenEnd),
            }, tokenStart, tokenEnd);
		default:
			return null;
		}
	}

	// validates a MipsTokenLieral according to the passed dataType
	private static boolean isValidLiteral(Token.TagData.DataTagEnum dataType, Token.Literal token)
	{
		long longMax = 0, longMin = 0; // init vals to satisfy compiler
		double doubleMax = 0, doubleMin = 0; // init vals to satisfy compiler
		boolean floating;
		// select appropriate max and min values
		switch (dataType)
		{
		case BYTE:   longMax   =    Byte.MAX_VALUE; longMin   =    Byte.MIN_VALUE; floating = false; break;
		case HALF:   longMax   =   Short.MAX_VALUE; longMin   =   Short.MIN_VALUE; floating = false; break;
		case WORD:   longMax   = Integer.MAX_VALUE; longMin   = Integer.MIN_VALUE; floating = false; break;
		case DWORD:  longMax   =    Long.MAX_VALUE; longMin   =    Long.MIN_VALUE; floating = false; break;
		case FLOAT:  doubleMax =   Float.MAX_VALUE; doubleMin =  -Float.MAX_VALUE; floating = true;  break;
		case DOUBLE: doubleMax =  Double.MAX_VALUE; doubleMin = -Double.MAX_VALUE; floating = true;  break;
		default: return false;
		}
		// validate according to selected values
		if (!floating)
		{ // integral
			if (!(token instanceof Token.LiteralLong))
				return false;
			long val = ((Token.LiteralLong)token).value;
			return (longMin <= val && val <= longMax);
		}
		else
		{ // floating point
			if (!(token instanceof Token.LiteralDouble))
				return false;
			double val = ((Token.LiteralDouble)token).value;
			return (doubleMin <= val && val <= doubleMax);
		}
	}

	// returns a byte array containing a given byte array repeated a given number of times
	private static byte[] repeatBytes(byte[] src, int reps)
	{
		byte[] rtnBytes = new byte[src.length * reps];
		for (int i = 0; i < reps; i++)
			System.arraycopy(src, 0, rtnBytes, i * src.length, src.length);
		return rtnBytes;
	}


	// Structure that contains all desirable information about a completed assemble
	public static class Binarization
	{
		public Bin.Header header;
		public Bin.BinBase[] dataElems;
		public int dataAddr;
		public Bin.BinBase[] textElems;
		public int textAddr;
		public HashMap<String, Long> labels;

		// take only required information and deduce the rest
        public Binarization(Bin.BinBase[] dataElems, Bin.BinBase[] textElems, HashMap<String, Long> labels, int dataAddress, int textAddress)
        {
			this.dataElems = dataElems;
			this.dataAddr = dataAddress;
			int dataLen = 0;
			for (Bin.BinBase binElem : this.dataElems)
				dataLen += binElem.getBinLen();

			this.textElems = textElems;
			this.textAddr = textAddress;
			int textLen = 0;
			for (Bin.BinBase binElem : this.textElems)
				textLen += binElem.getBinLen();

			this.header = new Bin.Header(dataAddr, dataLen, textAddr, textLen);

			this.labels = labels;
		}
	}
}





