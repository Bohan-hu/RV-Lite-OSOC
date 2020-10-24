package core

import common.OpConstants._
import common.Instructions._
import common._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class Decode2Exe extends Bundle {
  val instValid = Bool()
  val BrType = UInt(4.W)
  val R1ren = Bool()
  val R2ren = Bool()
  val Raddr1 = UInt(5.W)
  val Raddr2 = UInt(5.W)
  val RdNum = UInt(5.W)
  val Op1 = UInt(64.W)
  val Op2 = UInt(64.W)
  val R2val = UInt(64.W) // For L/S
  val ALUOp = UInt(4.W)
  val FUType = UInt()
  val isWordOp = Bool()
  val WBSel = UInt(2.W)
  val RFWen = Bool()
  val isMemOp = Bool()
  val MemOp = UInt(2.W)
  val MemType = UInt(3.W)
  val CSRCmd = UInt(3.W)
  val isFence = Bool()
  val exceInfo = new ExceptionInfo
}

class Decode extends Module {
  val io = IO(new Bundle() {
    val instBundleIn = Input(new InstBundle)
    val exceptionInfoIF = Input(new ExceptionInfo)
    val regfileIO = Flipped(new RegRead)
    val decode2Exe = Output(new Decode2Exe)
    val instBundleOut = Output(new InstBundle)
    val intCtrl = Input(new INTCtrl)
    val PLIC_SEI = Input(Bool())
  })

  def extractImm[T <: Instruction](inst: T): UInt = io.instBundleIn.inst.asTypeOf(inst).imm_ext

  val dummy = List(N, BR_N, OP1_RS1, OP2_RS2, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_READ, SZ_W, CSR_X, N);
  val decodeops =
    Array(/* val  |  BR  |  op1  |   op2     |  R1  |  R2  |    FU      |   FU    | ALU is   |  wb    | rf   | is   |   mem     | mask  | csr | fence.i */
      /* inst | type |   sel |    sel    |  ren |  ren |     op     |  TYPE   | WordOp   |  sel   | wen  |  mem |    op     | type  | cmd |         */
      LD ->     List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_D, CSR_X, N),
      LW ->     List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_W, CSR_X, N),
      LWU ->    List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_WU, CSR_X, N),
      LB -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_B, CSR_X, N),
      LBU -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_BU, CSR_X, N),
      LH -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_H, CSR_X, N),
      LHU -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, LSU_LOAD, FU_LSU, N, WB_MEM, Y, Y, MEM_READ, SZ_HU, CSR_X, N),
      SD -> List(Y, BR_N, OP1_RS1, IMM_STYPE, Y, Y, LSU_STORE, FU_LSU, N, WB_X, N, Y, MEM_WRITE, SZ_D, CSR_X, N),
      SW -> List(Y, BR_N, OP1_RS1, IMM_STYPE, Y, Y, LSU_STORE, FU_LSU, N, WB_X, N, Y, MEM_WRITE, SZ_W, CSR_X, N),
      SB -> List(Y, BR_N, OP1_RS1, IMM_STYPE, Y, Y, LSU_STORE, FU_LSU, N, WB_X, N, Y, MEM_WRITE, SZ_B, CSR_X, N),
      SH -> List(Y, BR_N, OP1_RS1, IMM_STYPE, Y, Y, LSU_STORE, FU_LSU, N, WB_X, N, Y, MEM_WRITE, SZ_H, CSR_X, N),
      LR_D    -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, N, LSU_LR, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      SC_D    -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_SC, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      LR_W    -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, N, LSU_LR, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      SC_W    -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_SC, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOSWAP_D -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_ASWAP, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOADD_D  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AADD, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOXOR_D  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AXOR, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOAND_D  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AAND, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOOR_D   -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AOR, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOMIN_D  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMIN, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOMAX_D  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMAX, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOMINU_D -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMINU, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOMAXU_D -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMAXU, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_D, CSR_X, N),
      AMOSWAP_W -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_ASWAP, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOADD_W  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AADD, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOXOR_W  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AXOR, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOAND_W  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AAND, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOOR_W   -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AOR, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOMIN_W  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMIN, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOMAX_W  -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMAX, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOMINU_W -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMINU, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      AMOMAXU_W -> List(Y, BR_N, OP1_RS1, IMM_ZERO, Y, Y, LSU_AMAXU, FU_LSU, N, WB_MEM, Y, Y, MEM_AMO, SZ_W, CSR_X, N),
      // IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII_I
      AUIPC -> List(Y, BR_N, OP1_PC, IMM_UTYPE, N, N, ALU_ADD, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      LUI -> List(Y, BR_N, OP1_X, IMM_UTYPE, N, N, ALU_COPY_2, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      ADDI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_ADD, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      ANDI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_AND, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      ORI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_OR, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      XORI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_XOR, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SLTI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SLT, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SLTIU -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SLTU, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SLLI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SLL, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRAI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SRA, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRLI -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SRL, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),

      ADDIW -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_ADD, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SLLIW -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SLL, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRAIW -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SRA, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRLIW -> List(Y, BR_N, OP1_RS1, IMM_ITYPE, Y, N, ALU_SRL, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      // RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR_
      SLL -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SLL, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      ADD -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_ADD, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SUB -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SUB, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SLT -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SLT, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SLTU -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SLTU, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      AND -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_AND, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      OR -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_OR, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      XOR -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_XOR, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRA -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SRA, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRL -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SRL, FU_ALU, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),

      SLLW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SLL, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      ADDW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_ADD, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SUBW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SUB, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRAW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SRA, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      SRLW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, ALU_SRL, FU_ALU, Y, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),

      JAL -> List(Y, BR_J, OP1_RS1, IMM_JTYPE, N, N, ALU_X, FU_ALU, N, WB_PC4, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      JALR -> List(Y, BR_JR, OP1_RS1, IMM_ITYPE, Y, N, ALU_X, FU_ALU, N, WB_PC4, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      BEQ -> List(Y, BR_EQ, OP1_RS1, IMM_BTYPE, Y, Y, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N),
      BNE -> List(Y, BR_NE, OP1_RS1, IMM_BTYPE, Y, Y, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N),
      BGE -> List(Y, BR_GE, OP1_RS1, IMM_BTYPE, Y, Y, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N),
      BGEU -> List(Y, BR_GEU, OP1_RS1, IMM_BTYPE, Y, Y, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N),
      BLT -> List(Y, BR_LT, OP1_RS1, IMM_BTYPE, Y, Y, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N),
      BLTU -> List(Y, BR_LTU, OP1_RS1, IMM_BTYPE, Y, Y, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N),

      MUL -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_MUL, FU_MUL, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      MULH -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_MULH, FU_MUL, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      MULHU -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_MULHU, FU_MUL, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      MULHSU -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_MULHSU, FU_MUL, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      MULW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_MULW, FU_MUL, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      DIV -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_DIV, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      DIVU -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_DIVU, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      REM -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_REM, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      REMU -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_REMU, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      DIVW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_DIVW, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      DIVUW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_DIVUW, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      REMW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_REMW, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),
      REMUW -> List(Y, BR_N, OP1_RS1, OP2_RS2, Y, Y, MDU_REMUW, FU_DIV, N, WB_ALU, Y, N, MEM_NOP, SZ_X, CSR_X, N),

      CSRRWI -> List(Y, BR_N, OP1_X, IMM_ZEXT, Y, Y, ALU_COPY_2, FU_ALU, N, WB_CSR, Y, N, MEM_NOP, SZ_X, CSR_W, N),
      CSRRSI -> List(Y, BR_N, OP1_X, IMM_ZEXT, Y, Y, ALU_COPY_2, FU_ALU, N, WB_CSR, Y, N, MEM_NOP, SZ_X, CSR_S, N),
      CSRRW -> List(Y, BR_N, OP1_RS1, OP2_X, Y, Y, ALU_COPY_1, FU_ALU, N, WB_CSR, Y, N, MEM_NOP, SZ_X, CSR_W, N),
      CSRRS -> List(Y, BR_N, OP1_RS1, OP2_X, Y, Y, ALU_COPY_1, FU_ALU, N, WB_CSR, Y, N, MEM_NOP, SZ_X, CSR_S, N),
      CSRRC -> List(Y, BR_N, OP1_RS1, OP2_X, Y, Y, ALU_COPY_1, FU_ALU, N, WB_CSR, Y, N, MEM_NOP, SZ_X, CSR_C, N),
      CSRRCI -> List(Y, BR_N, OP1_X, IMM_ZEXT, Y, Y, ALU_COPY_2, FU_ALU, N, WB_CSR, Y, N, MEM_NOP, SZ_X, CSR_C, N),
      ECALL -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_I, N),
      MRET -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_I, N),
      SRET -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_I, N),
      URET -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_I, N),
      EBREAK -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_I, N),
      WFI -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, N), // implemented as a NOP

      FENCE_I -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, N, MEM_NOP, SZ_X, CSR_X, Y),
      // kill pipeline and refetch instructions since the pipeline will be holding stall instructions.
      FENCE -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, Y, MEM_NOP, SZ_X, CSR_X, N),
      // we are already sequentially consistent, so no need to honor the fence instruction
      SFENCE_VMA -> List(Y, BR_N, OP1_X, OP2_X, N, N, ALU_X, FU_ALU, N, WB_X, N, Y, MEM_NOP, SZ_X, CSR_X, N)
    )
  val decode_ops = ListLookup(io.instBundleIn.inst, dummy, decodeops)
  val (inst_valid: Bool) :: br_Type :: op1Sel :: op2Sel :: (rs1Ren: Bool) :: (rs2Ren: Bool) :: aluOp :: fuType :: (isWordOp: Bool) :: wbSel :: (wbEn: Bool) :: (memEn: Bool) :: memOp :: memMask :: csrOp :: (isFence: Bool) :: Nil = decode_ops
  // Decode 2 Exe
  val Rd = io.instBundleIn.inst.asTypeOf(new RTypeInstruction).rd
  // Regfile connection
  io.regfileIO.raddr1 := io.instBundleIn.inst.asTypeOf(new RTypeInstruction).rs1
  io.regfileIO.raddr2 := io.instBundleIn.inst.asTypeOf(new RTypeInstruction).rs2
  val RS1 = io.regfileIO.rdata1
  val RS2 = io.regfileIO.rdata2
  // Get Operands
  val op1 = MuxLookup(op1Sel, RS1,
    Array(
      OP1_RS1 -> RS1,
      OP1_PC -> io.instBundleIn.inst_pc
    ))
  val op2 = MuxLookup(op2Sel, RS2,
    Array(
      OP2_RS2 -> RS2,
      IMM_ITYPE -> extractImm(new ITypeInstruction),
      IMM_STYPE -> extractImm(new STypeInstruction),
      IMM_UTYPE -> extractImm(new UTypeInstruction),
      IMM_JTYPE -> extractImm(new JTypeInstruction),
      IMM_BTYPE -> extractImm(new BTypeInstruction),
      IMM_ZEXT -> extractImm(new CSRIInstruction),
      IMM_ZERO -> 0.U
    ))
  io.decode2Exe.instValid := inst_valid & io.instBundleIn.instValid
  io.decode2Exe.BrType := br_Type
  io.decode2Exe.R1ren := rs1Ren
  io.decode2Exe.R2ren := rs2Ren
  io.decode2Exe.Raddr1 := io.regfileIO.raddr1
  io.decode2Exe.Raddr2 := io.regfileIO.raddr2
  io.decode2Exe.RdNum := Rd
  io.decode2Exe.Op1 := op1
  io.decode2Exe.Op2 := op2
  io.decode2Exe.R2val := io.regfileIO.rdata2
  io.decode2Exe.ALUOp := aluOp
  io.decode2Exe.FUType := fuType
  io.decode2Exe.isWordOp := isWordOp
  io.decode2Exe.WBSel := wbSel
  io.decode2Exe.RFWen := wbEn
  io.decode2Exe.isMemOp := memEn
  io.decode2Exe.MemOp := memOp
  io.decode2Exe.MemType := memMask
  io.decode2Exe.CSRCmd := csrOp
  io.decode2Exe.isFence := isFence
  // Assign the inst
  io.instBundleOut := io.instBundleIn
  io.instBundleOut.instValid := io.decode2Exe.instValid

  val M = "b11".U
  val S = "b01".U
  val U = "b00".U
  // Exception info
  val exceptionInfo = WireInit(io.exceptionInfoIF)

  def MipAndMie(no: Int) = io.intCtrl.mip(no) && io.intCtrl.mie(no)

  def makeInt(no: Int) = (no.U | 1.U << 63)

  when(!io.exceptionInfoIF.valid) {
    exceptionInfo.tval := io.instBundleIn.inst
    // TODO: Illegal instruction on xRET when x > privMode

    // ================= Handle ECALL and EBREAK begins ===================
    when(io.instBundleIn.inst === "b00000000000000000000000001110011".U) { // ECALL
      exceptionInfo.valid := true.B
      switch(io.intCtrl.privMode) {
        is(M) {
          exceptionInfo.cause := ExceptionNo.ecallM.U
        }
        is(S) {
          exceptionInfo.cause := ExceptionNo.ecallS.U
        }
        is(U) {
          exceptionInfo.cause := ExceptionNo.ecallU.U
        }
      }
    }.elsewhen(io.instBundleIn.inst === "b00000000000100000000000001110011".U) { // EBREAK
      exceptionInfo.valid := true.B
      exceptionInfo.cause := ExceptionNo.breakPoint.U
    }

    // ================= Handle ECALL and EBREAK ends ===================
    // If the instruction did not throw any exception in IF, we can attach the interrupt on this instruction
    // the interrupt is order by priority, highest last
    // We need a cause int signal to show whether the corresponding int is enabled
    val causeInt = Wire(Bool())
    causeInt := false.B
    when(MipAndMie(IntNo.STI)) {
      exceptionInfo.cause := makeInt(IntNo.STI)
      causeInt := true.B
    }
    when(MipAndMie(IntNo.SSI)) {
      exceptionInfo.cause := makeInt(IntNo.SSI)
      causeInt := true.B
    }
    /*
    the platform-level interrupt controller may generate supervisor-level external interrupts.
    Supervisor-level external interrupts are made pending based on the
    logical-OR of the software- writable SEIP bit and the signal from the external interrupt controller
     */
    when((io.intCtrl.mip(IntNo.SEI) | io.PLIC_SEI) && io.intCtrl.mie(IntNo.SEI)) {
      exceptionInfo.cause := makeInt(IntNo.SEI)
      causeInt := true.B
    }
    when(MipAndMie(IntNo.MTI)) {
      exceptionInfo.cause := makeInt(IntNo.MTI)
      causeInt := true.B
    }
    when(MipAndMie(IntNo.MSI)) {
      exceptionInfo.cause := makeInt(IntNo.MSI)
      causeInt := true.B
    }
    when(MipAndMie(IntNo.MEI)) {
      exceptionInfo.cause := makeInt(IntNo.MEI)
      causeInt := true.B
    }
    // Until here, the value of exceptionInfo.cause has been the cause with the highest priority
    // If we are in M mode, the S INT is disabled (unless is delegated)
    // If we are in S mode,
    // If M mode interrupts are disabled and we are in S mode,
    when(exceptionInfo.cause(63) & io.intCtrl.intGlobalEnable & causeInt & io.instBundleIn.instValid) { // If is interrupt, we need to consider whether the interrupt can be taken
      when(io.intCtrl.mideleg(exceptionInfo.cause(62, 0))        // If SxI is delegated
        && ((io.intCtrl.sie && io.intCtrl.privMode === S) || io.intCtrl.privMode === U)) { // If is delegated to S mode
        exceptionInfo.valid := true.B
      }.otherwise {
        exceptionInfo.valid := true.B
      }
    }
  }
  io.decode2Exe.exceInfo := exceptionInfo

}


object Decode extends App {
  chisel3.Driver.execute(args, () => new Decode)
}
