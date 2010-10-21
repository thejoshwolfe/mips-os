package com.wolfesoftware.mipsos.mips.simulator;

import com.wolfesoftware.mipsos.common.*;
import com.wolfesoftware.mipsos.mips.EMipsInstr;

public class SimulatorCore implements ISimulatorCore
{
    /** Register File */
    private int registers[] = new int[32];
    /** Main memory */
    private IMultisizeMemory memory;
    /** Program Counter */
    private int pc;
    /** Hi register */
    private int hi = 0;
    /** Lo register */
    private int lo = 0;
    /** Current status */
    private EStatus status;
    /** Current listener */
    ISimulatorListener listener = null;
    /** Input waiting to be consumed */
    private String inputBuffer = "";
    /** The executable to run */
    private IExecutable executable = null;

    /** Init with default options */
    public SimulatorCore()
    {
        this(new SimulatorOptions());
    }

    public SimulatorCore(SimulatorOptions options)
    {
        memory = new Memory(options.pageSizeExponent);
        this.listener = options.listener;
    }

    public void setSimulatorListener(ISimulatorListener listener)
    {
        this.listener = listener;
    }

    public EStatus loadExecutable(IExecutable executable)
    {
        if (!isValidExecutable(executable))
        {
            status = EStatus.NotInitialized;
            return status;
        }
        this.executable = executable;
        initExecutable();
        return status;
    }

    public EStatus reload()
    {
        if (executable == null)
        {
            assert (status == EStatus.NotInitialized); // TODO: assert
            return status;
        }
        initExecutable();
        return status;
    }

    public EStatus getStatus()
    {
        return status;
    }

    public void input(String inText)
    {
        inputBuffer += inText;
    }

    public EStatus step()
    {
        switch (status)
        {
            case NotInitialized:
                return status;
            case Break:
            case Ready:
                int instruction = memory.loadWord(pc);
                pc += 4;
                status = EStatus.Ready; // assume success
                executeInstruction(instruction);
                break;
            case NeedInput:
                break;
            case Done:
                break;
            default:
                throw new RuntimeException(); // TODO
        }
        return status;
    }

    private void initExecutable()
    {
        memory.clearAllSegments();
        for (IDataSegment segment : executable.getDataSegnemnts())
            memory.initSegment(segment);
        pc = executable.getEntryPoint();
        status = EStatus.Ready;
    }

    private boolean isValidExecutable(IExecutable executable)
    {
        return true;
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
        EMipsInstr instr = EMipsInstr.fromOpcodeAndFunct(opcode, funct);
        if (instr == null)
            throw new RuntimeException(); // TODO

        // execute
        switch (instr)
        {
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
                status = EStatus.Break;
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
        // http://www.inf.pucrs.br/~eduardob/disciplinas/arqi/mips/spim/syscall_codes.html
        int syscallCode = registers[3];
        switch (syscallCode)
        {
            case 1: // print_int
                listener.output(Integer.toString(registers[4]));
                break;
            case 4: // print_str
                listener.output(com.wolfesoftware.mipsos.mips.ByteUtils.readAsciiz(memory, registers[4]));
                break;
            case 5:
                // TODO
                break;
            case 8:
                // TODO
                break;
            case 10:
                status = EStatus.Done;
                break;
            default:
                throw new RuntimeException(); // TODO
        }
    }

    public static class SimulatorOptions
    {
        public int pageSizeExponent;
        public ISimulatorListener listener;

        public SimulatorOptions()
        {
            this(6, null);
        }

        public SimulatorOptions(int pageSizeExponent, ISimulatorListener listener)
        {
            this.pageSizeExponent = pageSizeExponent;
            this.listener = listener;
        }
    }

}
