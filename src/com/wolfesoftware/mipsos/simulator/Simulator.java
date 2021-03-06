package com.wolfesoftware.mipsos.simulator;

import java.io.IOException;
import java.util.*;

import com.wolfesoftware.mipsos.assembler.*;
import com.wolfesoftware.mipsos.common.*;
import com.wolfesoftware.mipsos.debugger.Extras;

public class Simulator
{
    public static void main(String[] args) throws AssemblingException, IOException
    {
        // parse the args
        LinkedList<String> argList = Util.arrayToLinkedList(args);
        SimulatorOptions simulatorOptions = new SimulatorOptions();
        simulatorOptions.parse(argList, false);
        simulatorOptions.normalize();

        if (argList.size() != 1)
            throw new RuntimeException();
        String inputPath = argList.getFirst();

        // assemble source
        ExecutableBinary binary = Assembler.assembleToBinary(inputPath, simulatorOptions.assemblerOptions);

        // init the simulator.
        Simulator simulator = new Simulator(simulatorOptions, ISimulatorListener.STD_ADAPTER);
        simulator.loadBinary(binary);

        // run and don't look back
        simulator.run();
    }

    // registers
    private int registers[] = new int[32];
    private int pc;
    private int hi = 0;
    private int lo = 0;
    private int clock = 0;
    private int interruptHandler = 0;
    private int nextTimerInterrupt = -1;
    private int epc = 0;

    private Memory memory;

    private SimulatorStatus status = SimulatorStatus.Ready;
    public ISimulatorListener listener = null;
    private SimulatorOptions options;

    public Simulator(SimulatorOptions options, ISimulatorListener listener)
    {
        this.options = options;
        this.listener = listener;
        memory = new Memory(options.pageSizeExponent);
    }

    public void loadBinary(ExecutableBinary binary)
    {
        for (Segment segment : binary.segments) {
            byte[] type = segment.attributes.get(Segment.ATTRIBUTE_TYPE);
            if (Arrays.equals(type, Segment.TYPE_MEMORY)) {
                byte[] addressBytes = segment.attributes.get(Segment.ATTRIBUTE_ADDRESS);
                int address = ByteUtils.readInt(addressBytes, 0);
                memory.storeBytes(segment.bytes, segment.offset, segment.length, address);
            } else if (Arrays.equals(type, Segment.TYPE_ENTRYPOINT)) {
                byte[] addressBytes = segment.attributes.get(Segment.ATTRIBUTE_ADDRESS);
                int address = ByteUtils.readInt(addressBytes, 0);
                pc = address;
            }
        }
    }
    public int getPc()
    {
        return pc;
    }
    public SimulatorStatus getStatus()
    {
        return status;
    }
    public int getClock()
    {
        return clock;
    }
    public int[] getRegisters()
    {
        return registers.clone();
    }
    public Extras getExtras()
    {
        return new Extras(hi, lo, interruptHandler, nextTimerInterrupt, epc);
    }
    public byte[] getMemory(int address, int length)
    {
        return memory.getMemory(address, length);
    }

    public void run()
    {
        while (status != SimulatorStatus.Done)
            internalStep();
    }
    public SimulatorStatus step()
    {
        internalStep();
        return status;
    }

    private void internalStep()
    {
        // fetch
        int instruction = memory.loadWord(pc);
        pc += 4;
        // execute
        status = SimulatorStatus.Ready; // assume success
        executeInstruction(instruction);
        // fix the zero register
        registers[0] = 0;
        // bump the clock and check for timer interrupts
        clock++;
        if (clock == nextTimerInterrupt) {
            // interrupt
            epc = pc;
            pc = interruptHandler;
        }
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
        int funct = instruction & 0x3F;
        int zeroExtImm = instruction & 0xFFFF;
        int signExtImm = ((zeroExtImm & 0x8000) == 0 ? zeroExtImm : zeroExtImm - 0x10000);
        int target = instruction & 0x02FFFFFF;
        int targetAddress = (pc & 0xF0000000) | (target << 2);

        // get instruction from opcode and maybe funct
        MipsInstr instr = MipsInstr.fromOpcodeRsFunct(opcode, rs, funct);
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
                    pc += signExtImm << 2;
                break;
            case BNE:
                if (registers[rt] != registers[rs])
                    pc += signExtImm << 2;
                break;
            case BREAK:
                status = SimulatorStatus.Break;
                break;
            case DIV:
                lo = registers[rs] / registers[rt];
                hi = registers[rs] % registers[rt];
                break;
            case J:
                pc = targetAddress;
                break;
            case JAL:
                registers[31] = pc;
                pc = targetAddress;
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
            case MFC0:
                switch (rt) {
                    case 2:
                        registers[rd] = interruptHandler;
                        break;
                    case 4:
                        registers[rd] = lo;
                        break;
                    case 5:
                        registers[rd] = hi;
                        break;
                    case 9:
                        registers[rd] = clock;
                        break;
                    case 11:
                        registers[rd] = nextTimerInterrupt;
                        break;
                    case 14:
                        registers[rd] = epc;
                        break;
                    default:
                        throw null;
                }
                break;
            case MTC0:
                switch (rd) {
                    case 2:
                        interruptHandler = registers[rt];
                        break;
                    case 4:
                        lo = registers[rt];
                        break;
                    case 5:
                        hi = registers[rt];
                        break;
                    case 9:
                        // whatever
                        clock = registers[rt];
                        break;
                    case 11:
                        nextTimerInterrupt = registers[rt];
                        break;
                    case 14:
                        // whatever
                        epc = registers[rt];
                        break;
                    default:
                        throw null;
                }
                break;
            default:
                throw null;
        }
    }

    private void syscall()
    {
        // spim syscall codes
        int syscallCode = registers[2];
        switch (syscallCode) {
            case 1: // print_int  $a0 = integer
                checkFancyIoSupport();
                for (char c : Integer.toString(registers[4]).toCharArray())
                    listener.printCharacter(c);
                break;
            case 2: // print_float   $f12 = float
            case 3: // print_double   $f12 = double
                throw new RuntimeException("floating point operations are not supported");
            case 4: // print_string   $a0 = string    
            {
                checkFancyIoSupport();
                int cursor = registers[4];
                while (true) {
                    byte c = memory.loadByte(cursor);
                    if (c == 0)
                        break;
                    listener.printCharacter((char)c);
                    cursor++;
                }
                break;
            }
            case 5: // read_int   integer (in $v0)
            {
                checkFancyIoSupport();
                StringBuilder builder = new StringBuilder();
                while (true) {
                    char c = readCharacter();
                    if (c == '\n')
                        break;
                    builder.append(c);
                }
                registers[2] = Integer.parseInt(builder.toString().trim());
                break;
            }
            case 6: // read_float   float (in $f0)
            case 7: // read_double   double (in $f0)
                throw new RuntimeException("floating point operations are not supported");
            case 8: // read_string   $a0 = buffer, $a1 = length  
            {
                checkFancyIoSupport();
                int cursor = registers[4];
                int maxLenght = registers[5];
                for (int i = 0; i < maxLenght; i++) {
                    char c = readCharacter();
                    memory.storeByte(cursor, (byte)c);
                    if (c == '\n')
                        break;
                    cursor++;
                }
                break;
            }
            case 9: // sbrk   $a0 = amount    address (in $v0)
                throw new RuntimeException("sbrk is not supported");
            case 10: // exit
                status = SimulatorStatus.Done;
                break;
            case 11: // print_character   $a0 = character
                listener.printCharacter((char)registers[4]);
                break;
            case 12: // read_character   character (in $v0)
                registers[2] = readCharacter();
                break;
            case 13: // open   $a0 = filename, $a1 = flags, $a2 = mode   file descriptor (in $v0)
            case 14: // read   $a0 = file descriptor, $a1 = buffer, $a2 = count   bytes read (in $v0)
            case 15: // write   $a0 = file descriptor, $a1 = buffer, $a2 = count   bytes written (in $v0)
            case 16: // close   $a0 = file descriptor   0 (in $v0)
                throw new RuntimeException("file io is not supported");
            case 17: // exit2   $a0 = value 
                throw new RuntimeException("exit2 is not supported");
            default:
                throw new RuntimeException("illegal syscall code");
        }
    }

    private char readCharacter()
    {
        status = SimulatorStatus.Stdin;
        try {
            return listener.readCharacter();
        } finally {
            status = SimulatorStatus.Ready;
        }
    }

    private void checkFancyIoSupport()
    {
        if (options.fancyIoSupport)
            return;
        throw new RuntimeException("fancy IO is not enabled");
    }
}
