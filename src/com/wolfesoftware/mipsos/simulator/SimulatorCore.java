package com.wolfesoftware.mipsos.simulator;

import com.wolfesoftware.mipsos.common.Segment;


public class SimulatorCore
{
    /** Register File */
    private int registers[] = new int[32];
    /** Main memory */
    private Memory memory;
    /** Program Counter */
    private int pc;
    /** Hi register */
    private int hi = 0;
    /** Lo register */
    private int lo = 0;
    /** Current status */
    private SimulatorStatus status = SimulatorStatus.Ready;
    /** Current listener */
    private ISimulatorListener listener = null;

    /** Init with default options */
    public SimulatorCore()
    {
        this(new SimulatorOptions());
    }

    public SimulatorCore(SimulatorOptions options)
    {
        memory = new Memory(options.pageSizeExponent);
    }

    public void setSimulatorListener(ISimulatorListener listener)
    {
        this.listener = listener;
    }

    public void storeSegment(Segment segment)
    {
        memory.storeSegment(segment);
    }

    public void setPc(int address)
    {
        pc = address;
    }

    public SimulatorStatus getStatus()
    {
        return status;
    }

    public void run()
    {
        while (status != SimulatorStatus.Done)
            internalStep();
    }

    private void internalStep()
    {
        int instruction = memory.loadWord(pc);
        pc += 4;
        status = SimulatorStatus.Ready; // assume success
        executeInstruction(instruction);
    }

    /** executes one unit of code and returns the status */
    public SimulatorStatus step()
    {
        switch (status) {
            case Break:
            case Ready:
                internalStep();
                break;
            case Done:
                throw new RuntimeException();
            default:
                throw null;
        }
        return status;
    }

    /** has a big switch in it */
    private void executeInstruction(int instruction)
    {
        // get all possible fields
        int opcode = instruction >>> 26;
        int rs = instruction >> 21 & 0x1F;
        int rt = instruction >> 16 & 0x1F;
        int rd = instruction >> 11 & 0x1F;
        int shamt = instruction >> 6 & 0x1F;
        int funct = instruction & 0x2F;
        int zeroExtImm = instruction & 0xFFFF;
        int signExtImm = ((zeroExtImm & 0x8000) == 0 ? zeroExtImm : zeroExtImm - 0x10000);
        int target = instruction & 0x02FFFFFF;
        int targetAddress = target << 2;

        // get instruction from opcode and maybe funct
        MipsInstr instr = MipsInstr.fromOpcodeAndFunct(opcode, funct);
        // execute
        switch (instr) {
            case ADD:
                registers[rd] = registers[rs] + registers[rt];
                break;
            case ADDI:
                registers[rt] = registers[rs] + signExtImm;
                break;
            case AND:
                registers[rd] = registers[rs] & registers[rt];
                break;
            case ANDI:
                registers[rt] = registers[rs] & zeroExtImm;
                break;
            case BEQ:
                if (registers[rt] == registers[rs])
                    pc += signExtImm;
                break;
            case BNE:
                if (registers[rt] != registers[rs])
                    pc += signExtImm;
                break;
            case BREAK:
                status = SimulatorStatus.Break;
                break;
            case DIV:
                hi = registers[rs] / registers[rt];
                lo = registers[rs] % registers[rt];
                break;
            case J:
                pc = (pc & 0xF0000000) | targetAddress;
                break;
            case JAL:
                registers[31] = pc;
                pc = (pc & 0xF0000000) | targetAddress;
                break;
            case JALR:
                registers[rd] = pc;
                pc = registers[rs];
                break;
            case JR:
                pc = registers[rs];
                break;
            case LB:
                registers[rt] = memory.loadByte(signExtImm + registers[rs]);
                break;
            case LH:
                registers[rt] = memory.loadHalf(signExtImm + registers[rs]);
                break;
            case LUI:
                registers[rt] = zeroExtImm << 16;
                break;
            case LW:
                registers[rt] = memory.loadWord(signExtImm + registers[rs]);
                break;
            case MFHI:
                registers[rd] = hi;
                break;
            case MFLO:
                registers[rd] = lo;
                break;
            case MTHI:
                registers[rs] = hi;
                break;
            case MTLO:
                registers[rs] = lo;
                break;
            case MULT:
                long rslt = (long)registers[rs] * (long)registers[rt];
                hi = (int)(0xFFFFFFFFL & (rslt >> 32));
                lo = (int)(0xFFFFFFFFL & rslt);
                break;
            case NOP:
                break;
            case NOR:
                registers[rd] = ~(registers[rs] | registers[rt]);
                break;
            case OR:
                registers[rd] = registers[rs] | registers[rt];
                break;
            case ORI:
                registers[rt] = registers[rs] | zeroExtImm;
                break;
            case SB:
                memory.storeByte(signExtImm + registers[rs], (byte)(registers[rt] & 0xFF));
                break;
            case SH:
                memory.storeHalf(signExtImm + registers[rs], (short)(registers[rt] & 0xFFFF));
                break;
            case SLL:
                registers[rd] = registers[rt] << shamt;
                break;
            case SLLV:
                registers[rd] = registers[rt] << registers[rs];
                break;
            case SLT:
                registers[rd] = (registers[rs] < registers[rt] ? 1 : 0);
                break;
            case SLTI:
                registers[rt] = (registers[rs] < signExtImm ? 1 : 0);
                break;
            case SRA:
                registers[rd] = registers[rt] >> shamt;
                break;
            case SRAV:
                registers[rd] = registers[rt] >> registers[rs];
                break;
            case SRL:
                registers[rd] = registers[rt] >>> shamt;
                break;
            case SRLV:
                registers[rd] = registers[rt] >>> registers[rs];
                break;
            case SUB:
                registers[rd] = registers[rs] - registers[rt];
                break;
            case SW:
                memory.storeWord(signExtImm + registers[rs], registers[rt]);
                break;
            case SYSCALL:
                syscall();
                break;
            case XOR:
                registers[rd] = registers[rs] ^ registers[rt];
                break;
            case XORI:
                registers[rt] = registers[rs] ^ zeroExtImm;
                break;
            default:
                throw new RuntimeException(); // TODO
        }
    }

    private void syscall()
    {
        // spim syscall codes
        int syscallCode = registers[2];
        switch (syscallCode) {
            case 10: // exit
                status = SimulatorStatus.Done;
                break;
            case 11: // print_character
                listener.printCharacter((char)registers[4]);
            default:
                throw new RuntimeException(); // TODO
        }
    }

    public static class SimulatorOptions
    {
        public int pageSizeExponent;

        public SimulatorOptions()
        {
            this(6);
        }

        public SimulatorOptions(int pageSizeExponent)
        {
            this.pageSizeExponent = pageSizeExponent;
        }
    }
}
