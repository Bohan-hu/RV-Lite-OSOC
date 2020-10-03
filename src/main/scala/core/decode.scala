package core

import common.OpConstants._
import common.Instructions._
import common._
import chisel3._
import chisel3.util._

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
  val R2val = UInt(64.W)  // For L/S
  val ALUOp = UInt(4.W)
  val isWordOp = Bool()
  val WBSel = UInt(2.W)
  val RFWen = Bool()
  val isMemOp = Bool()
  val MemOp = UInt(2.W)
  val MemType = UInt(3.W)
  val CSRCmd = UInt(3.W)
  val isFence = Bool()
}

class Decode extends Module {
  val io = IO(new Bundle() {
    val instBundleIn = Input(new InstBundle)
    val regfileIO = Flipped(new RegRead)
    val decode2Exe = Output(new Decode2Exe)
    val instBundleOut = Output(new InstBundle)
  })

  def extractImm[T <: Instruction](inst: T): UInt = io.instBundleIn.inst.asTypeOf(inst).imm_ext

  val dummy =     List(N, BR_N  , OP1_RS1, OP2_RS2 ,  N   ,  N   , ALU_X    , N, WB_X ,  N   ,  N   , MEM_READ  , SZ_W  , CSR_X, N);
  val decodeops =
      Array(      /* val  |  BR  |  op1  |   op2     |  R1  |  R2  |    ALU     | ALU is   |  wb    | rf   | is   |   mem     | mask  | csr | fence.i */
                  /* inst | type |   sel |    sel    |  ren |  ren |     op     | WordOp   |  sel   | wen  |  mem |    op     | type  | cmd |         */
        LD     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_D  , CSR_X, N),
        LW     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_W  , CSR_X, N),
        LWU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_WU , CSR_X, N),
        LB     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_B  , CSR_X, N),
        LBU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_BU , CSR_X, N),
        LH     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_H  , CSR_X, N),
        LHU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_MEM ,  Y   ,  Y   , MEM_READ  , SZ_HU , CSR_X, N),
        SD     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    ,   N    ,  WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_D  , CSR_X, N),
        SW     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    ,   N    ,  WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_W  , CSR_X, N),
        SB     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    ,   N    ,  WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_B  , CSR_X, N),
        SH     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    ,   N    ,  WB_X   ,  N   ,  Y   , MEM_WRITE , SZ_H  , CSR_X, N),
        // IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII_I
        AUIPC  -> List(Y, BR_N  , OP1_PC , IMM_UTYPE ,  N   ,  N   , ALU_ADD    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        LUI    -> List(Y, BR_N  , OP1_X  , IMM_UTYPE ,  N   ,  N   , ALU_COPY_2 ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ADDI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ANDI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_AND    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ORI    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_OR     ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        XORI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_XOR    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLTI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLT    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLTIU  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLTU   ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLLI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLL    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRAI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRA    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRLI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRL    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),

        ADDIW  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLLIW  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLL    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRAIW  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRA    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRLIW  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRL    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        // RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR_
        SLL    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLL    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ADD    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_ADD    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SUB    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SUB    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLT    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLT    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SLTU   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLTU   ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        AND    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_AND    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        OR     -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_OR     ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        XOR    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_XOR    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRA    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRA    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRL    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRL    ,   N    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),

        SLLW   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLL    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        ADDW   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_ADD    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SUBW   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SUB    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRAW   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRA    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        SRLW   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRL    ,   Y    ,  WB_ALU ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),

        JAL    -> List(Y, BR_J  , OP1_RS1, IMM_JTYPE ,  N   ,  N   , ALU_X      ,   N    ,  WB_PC4 ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        JALR   -> List(Y, BR_JR , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_X      ,   N    ,  WB_PC4 ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BEQ    -> List(Y, BR_EQ , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BNE    -> List(Y, BR_NE , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BGE    -> List(Y, BR_GE , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BGEU   -> List(Y, BR_GEU, OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BLT    -> List(Y, BR_LT , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        BLTU   -> List(Y, BR_LTU, OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N),
        CSRRWI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 ,   N    ,  WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_W, N),
        CSRRSI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 ,   N    ,  WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_S, N),
        CSRRW  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 ,   N    ,  WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_W, N),
        CSRRS  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 ,   N    ,  WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_S, N),
        CSRRC  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 ,   N    ,  WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_C, N),
        CSRRCI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 ,   N    ,  WB_CSR ,  Y   ,  N   , MEM_NOP   , SZ_X  , CSR_C, N),
        ECALL  -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        MRET   -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        URET   -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        EBREAK -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_I, N),
        WFI    -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, N), // implemented as a NOP

        FENCE_I-> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  N   , MEM_NOP   , SZ_X  , CSR_X, Y),
        // kill pipeline and refetch instructions since the pipeline will be hold   N    , ing stall instructions.
        FENCE  -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      ,   N    ,  WB_X   ,  N   ,  Y   , MEM_NOP   , SZ_X  , CSR_X, N)
        // we are already sequentially consistent, so no need to honor the fence instruction
    )
  val decode_ops = ListLookup(io.instBundleIn.inst, dummy, decodeops)
  val (inst_valid: Bool) :: br_Type :: op1Sel :: op2Sel :: (rs1Ren: Bool) :: (rs2Ren: Bool) :: aluOp :: (isWordOp: Bool) :: wbSel :: (wbEn: Bool) :: (memEn: Bool) :: memOp :: memMask :: csrOp :: (isFence : Bool) :: Nil = decode_ops
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
      IMM_ZEXT -> extractImm(new CSRIInstruction)
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
}

object Decode extends App {
  chisel3.Driver.execute(args, () => new Decode)
}
