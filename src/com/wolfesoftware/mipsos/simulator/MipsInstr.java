package com.wolfesoftware.mipsos.simulator;

import java.util.HashMap;

public enum MipsInstr {
    // basic instructions
    ADD, ADDI, AND, ANDI, BEQ, BNE, BREAK, DIV, J, JAL, JALR, JR, LB, LH, LUI, LW, MFHI, MFLO, MTHI, MTLO, MULT, NOP, NOR, OR, ORI, SB, SH, SLL, SLLV, SLT, SLTI, SRA, SRAV, SRL, SRLV, SUB, SW, SYSCALL, XOR, XORI,
    // pseudo instructions
    BGE, BGEZ, BGT, BGTZ, BLE, BLEZ, BLT, BLTZ, LA, LI, MOVE,
    // used in decoding
    Ambiguous, ;
    // TODO: support unsigned arithmetic instructions

    private static HashMap<MipsInstr, Integer> hashToOpcode;
    private static HashMap<Integer, MipsInstr> hashFromOpcode;
    private static HashMap<MipsInstr, Integer> hashToFunct;
    private static HashMap<Integer, MipsInstr> hashFromFunct;
    private static HashMap<MipsInstr, String> hashToString;
    private static HashMap<String, MipsInstr> hashFromString;

    public Integer toOpcode()
    {
        return hashToOpcode.get(this);
    }

    public static MipsInstr fromOpcode(int opcode)
    {
        return hashFromOpcode.get(opcode);
    }

    public Integer getFunct()
    {
        return hashToFunct.get(this);
    }

    public static MipsInstr fromOpcodeAndFunct(int opcode, int funct)
    {
        if (opcode == 0)
            return hashFromFunct.get(funct);
        else
            return fromOpcode(opcode);
    }

    static {
        // to opcode
        hashToOpcode = new HashMap<MipsInstr, Integer>(40);
        hashToOpcode.put(ADD, 0x00);
        hashToOpcode.put(ADDI, 0x08);
        hashToOpcode.put(AND, 0x00);
        hashToOpcode.put(ANDI, 0x0C);
        hashToOpcode.put(BEQ, 0x04);
        hashToOpcode.put(BNE, 0x05);
        hashToOpcode.put(BREAK, 0x00);
        hashToOpcode.put(DIV, 0x00);
        hashToOpcode.put(J, 0x02);
        hashToOpcode.put(JAL, 0x03);
        hashToOpcode.put(JALR, 0x00);
        hashToOpcode.put(JR, 0x00);
        hashToOpcode.put(LB, 0x20);
        hashToOpcode.put(LH, 0x21);
        hashToOpcode.put(LUI, 0x0F);
        hashToOpcode.put(LW, 0x23);
        hashToOpcode.put(MFHI, 0x00);
        hashToOpcode.put(MFLO, 0x00);
        hashToOpcode.put(MTHI, 0x00);
        hashToOpcode.put(MTLO, 0x00);
        hashToOpcode.put(MULT, 0x00);
        hashToOpcode.put(NOP, 0x00);
        hashToOpcode.put(NOR, 0x00);
        hashToOpcode.put(OR, 0x00);
        hashToOpcode.put(ORI, 0x0D);
        hashToOpcode.put(SB, 0x28);
        hashToOpcode.put(SH, 0x29);
        hashToOpcode.put(SLL, 0x00);
        hashToOpcode.put(SLLV, 0x00);
        hashToOpcode.put(SLT, 0x00);
        hashToOpcode.put(SLTI, 0x0A);
        hashToOpcode.put(SRA, 0x00);
        hashToOpcode.put(SRAV, 0x00);
        hashToOpcode.put(SRL, 0x00);
        hashToOpcode.put(SRLV, 0x00);
        hashToOpcode.put(SUB, 0x00);
        hashToOpcode.put(SW, 0x2B);
        hashToOpcode.put(SYSCALL, 0x00);
        hashToOpcode.put(XOR, 0x00);
        hashToOpcode.put(XORI, 0x0E);

        // from opcode
        hashFromOpcode = new HashMap<Integer, MipsInstr>(16);
        hashFromOpcode.put(0x00, MipsInstr.Ambiguous);
        for (MipsInstr instr : hashToOpcode.keySet()) {
            Integer opcode = hashToOpcode.get(instr);
            if (opcode != 0x00)
                hashFromOpcode.put(opcode, instr);
        }

        // to funct
        hashToFunct = new HashMap<MipsInstr, Integer>(24);
        hashToFunct.put(ADD, 0x20);
        hashToFunct.put(AND, 0x24);
        hashToFunct.put(BREAK, 0x0D);
        hashToFunct.put(DIV, 0x1A);
        hashToFunct.put(JALR, 0x09);
        hashToFunct.put(JR, 0x08);
        hashToFunct.put(MFHI, 0x10);
        hashToFunct.put(MFLO, 0x12);
        hashToFunct.put(MTHI, 0x11);
        hashToFunct.put(MTLO, 0x13);
        hashToFunct.put(MULT, 0x18);
        hashToFunct.put(NOP, 0x00);
        hashToFunct.put(NOR, 0x27);
        hashToFunct.put(OR, 0x25);
        hashToFunct.put(SLL, 0x00);
        hashToFunct.put(SLLV, 0x04);
        hashToFunct.put(SLT, 0x2A);
        hashToFunct.put(SRA, 0x03);
        hashToFunct.put(SRAV, 0x07);
        hashToFunct.put(SRL, 0x02);
        hashToFunct.put(SRLV, 0x06);
        hashToFunct.put(SUB, 0x22);
        hashToFunct.put(SYSCALL, 0x0C);
        hashToFunct.put(XOR, 0x26);

        // from funct
        hashFromFunct = new HashMap<Integer, MipsInstr>(hashToFunct.size());
        for (MipsInstr instr : hashToFunct.keySet())
            hashFromFunct.put(hashToFunct.get(instr), instr);

        // to/from String
        MipsInstr[] instrs = MipsInstr.values();
        hashToString = new HashMap<MipsInstr, String>(instrs.length);
        hashFromString = new HashMap<String, MipsInstr>(instrs.length);
        for (MipsInstr instr : instrs) {
            hashToString.put(instr, instr.name());
            hashFromString.put(instr.name(), instr);
        }
    }
}
