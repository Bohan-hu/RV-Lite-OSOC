package core

import common.OpConstants._
import common.Instructions._
import common._
import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class DecodeSignal extends Bundle {
  val instValid = Bool()
  val BrType = UInt()
  val Op1Sel = UInt()
  val Op2Sel = UInt()
  val R1ren = Bool()
  val R2ren = Bool()
  val ALUOp = UInt()
  val WBSel = UInt()
  val RFWen = Bool()
  val isMemOp = Bool()
  val MemOp = UInt()
  val MemType = UInt()
  val CSRCmd = UInt()
  val isFence = Bool()
}

class Decode extends Module {
  val io = IO(new Bundle() {
    val PC = Input(UInt(64.W))
    val inst = Input(UInt(32.W))
    val regfileIO = Flipped(new RegRead)
    val op1Sel = Input(UInt(2.W))
    val op2Sel = Input(UInt(3.W))
    val op2 = Output(UInt())
    val op1 = Output(UInt())
    val outops = Output(UInt())
  })

  def extractImm[T <: Instruction](inst: T): UInt = io.inst.asTypeOf(inst).imm_ext

  val op1Sel = io.op1Sel
  val op2Sel = io.op2Sel
  // Regfile connection
  io.regfileIO.raddr1 := io.inst.asTypeOf(new RTypeInstruction).rs1
  io.regfileIO.raddr2 := io.inst.asTypeOf(new RTypeInstruction).rs2
  val RS1 = io.regfileIO.rdata1
  val RS2 = io.regfileIO.rdata2
  // Get Operands
  val op1 = MuxLookup(op1Sel, RS1,
    Array(
      OP1_RS1 -> RS1,
      OP1_PC -> io.PC
    ))
  val op2 = MuxLookup(op2Sel, RS2,
    Array(
      OP2_RS2 -> RS1,
      IMM_ITYPE -> extractImm(new ITypeInstruction),
      IMM_STYPE -> extractImm(new STypeInstruction),
      IMM_UTYPE -> extractImm(new UTypeInstruction),
      IMM_JTYPE -> extractImm(new JTypeInstruction),
      IMM_BTYPE -> extractImm(new BTypeInstruction),
      IMM_ZEXT -> extractImm(new CSRIInstruction)
    ))
  io.op1 := op1
  io.op2 := op2

  // TODO: Use a muxlookup and the constant definition and the array table to define the source operand
  val dummy = List(N, BR_N  , OP1_RS1, OP2_RS2 ,  N   ,  N   , ALU_X    , WB_X ,  N   ,  N   , MEM_READ  , SZ_W  , CSR_X, N);
  val decodeops =
      Array(      /* val  |  BR  |  op1  |   op2     |  R1  |  R2  |    ALU     |  wb    | rf   | is   |   mem     | mask  | csr | fence.i */
                  /* inst | type |   sel |    sel    |  ren |  ren |     op     |  sel   | wen  |  mem |    wr     | type  | cmd |         */
        LW     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_W  , CSR_X, N),
        LB     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_B  , CSR_X, N),
        LBU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_BU , CSR_X, N),
        LH     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_H  , CSR_X, N),
        LHU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_HU , CSR_X, N),
        SW     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    , WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_W  , CSR_X, N),
        SB     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    , WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_B  , CSR_X, N),
        SH     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    , WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_H  , CSR_X, N),
        // IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII_I
        AUIPC  -> List(Y, BR_N  , OP1_PC , IMM_UTYPE ,  N   ,  N   , ALU_ADD    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        LUI    -> List(Y, BR_N  , OP1_X  , IMM_UTYPE ,  N   ,  N   , ALU_COPY_2 , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ADDI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ANDI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_AND    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ORI    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_OR     , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        XORI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_XOR    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLTI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLT    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLTIU  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLTU   , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLLI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRAI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRA    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRLI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        // RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR_
        SLL    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ADD    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_ADD    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SUB    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SUB    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLT    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLT    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLTU   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLTU   , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        AND    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_AND    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        OR     -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_OR     , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        XOR    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_XOR    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRA    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRA    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRL    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        JAL    -> List(Y, BR_J  , OP1_RS1, IMM_JTYPE ,  N   ,  N   , ALU_X      , WB_PC4 ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        JALR   -> List(Y, BR_JR , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_X      , WB_PC4 ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BEQ    -> List(Y, BR_EQ , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BNE    -> List(Y, BR_NE , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BGE    -> List(Y, BR_GE , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BGEU   -> List(Y, BR_GEU, OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BLT    -> List(Y, BR_LT , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BLTU   -> List(Y, BR_LTU, OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        CSRRWI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 , WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_W, N),
        CSRRSI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 , WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_S, N),
        CSRRW  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 , WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_W, N),
        CSRRS  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 , WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_S, N),
        CSRRC  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 , WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_C, N),
        CSRRCI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 , WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_C, N),
        ECALL  -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        MRET   -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        URET   -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        EBREAK -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        WFI    -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N), // implemented as a NOP

        FENCE_I-> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, Y),
        // kill pipeline and refetch instructions since the pipeline will be holding stall instructions.
        FENCE  -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  Y   , MEM_NOP   , SZ_X  , CSR_X, N)
        // we are already sequentially consistent, so no need to honor the fence instruction
    )
  val decode_ops = ListLookup(io.inst, dummy, decodeops)
  io.outops := decode_ops.reduce( Cat(_,_) )
}

object decode extends App {
  chisel3.Driver.execute(args, () => new Decode)
}
