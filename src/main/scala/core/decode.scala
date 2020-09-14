package core
import common.OpConstants._
import common.Instructions._
import chisel3._

class Decode {
  // Always put the imm at the position OP2
  // TODO: Use a muxlookup and the constant definition and the array table to define the source operand
//  val decodeops =
//    Array(      /* val  |  BR  |  op1  |   op2     |  R1  |  R2  |    ALU     |  wb    | rf   | mem  |   mem     | mask  | csr | fence.i */
//                /* inst | type |   sel |    sel    |  ren |  ren |     op     |  sel   | wen  |  en  |    wr     | type  | cmd |         */
//      LW     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , MT_W  , CSR.N, N),
//      LB     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , MT_B  , CSR.N, N),
//      LBU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , MT_BU , CSR.N, N),
//      LH     -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , MT_H  , CSR.N, N),
//      LHU    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_MEM ,  Y   ,  Y   , MEM_READ  , MT_HU , CSR.N, N),
//      SW     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    , WB_X   ,  N   ,  Y   , MEM_WRITE , MT_W  , CSR.N, N),
//      SB     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    , WB_X   ,  N   ,  Y   , MEM_WRITE , MT_B  , CSR.N, N),
//      SH     -> List(Y, BR_N  , OP1_RS1, IMM_STYPE ,  Y   ,  Y   , ALU_ADD    , WB_X   ,  N   ,  Y   , MEM_WRITE , MT_H  , CSR.N, N),
//      // IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII
//      AUIPC  -> List(Y, BR_N  , OP1_PC , IMM_UTYPE ,  N   ,  N   , ALU_ADD    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      LUI    -> List(Y, BR_N  , OP1_X  , IMM_UTYPE ,  N   ,  N   , ALU_COPY_2 , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      ADDI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_ADD    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      ANDI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_AND    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      ORI    -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_OR     , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      XORI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_XOR    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SLTI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLT    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SLTIU  -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLTU   , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SLLI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SLL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SRAI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRA    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SRLI   -> List(Y, BR_N  , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_SRL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      // RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR
//      SLL    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      ADD    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_ADD    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SUB    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SUB    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SLT    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLT    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SLTU   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SLTU   , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      AND    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_AND    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      OR     -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_OR     , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      XOR    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_XOR    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SRA    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRA    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      SRL    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   ,  Y   ,  Y   , ALU_SRL    , WB_ALU ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      JAL    -> List(Y, BR_J  , OP1_RS1, IMM_JTYPE ,  N   ,  N   , ALU_X      , WB_PC4 ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      JALR   -> List(Y, BR_JR , OP1_RS1, IMM_ITYPE ,  Y   ,  N   , ALU_X      , WB_PC4 ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      BEQ    -> List(Y, BR_EQ , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      BNE    -> List(Y, BR_NE , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      BGE    -> List(Y, BR_GE , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      BGEU   -> List(Y, BR_GEU, OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      BLT    -> List(Y, BR_LT , OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      BLTU   -> List(Y, BR_LTU, OP1_RS1, IMM_BTYPE ,  Y   ,  Y   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N),
//      CSRRWI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 , WB_CSR ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.W, N),
//      CSRRSI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 , WB_CSR ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.S, N),
//      CSRRW  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 , WB_CSR ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.W, N),
//      CSRRS  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 , WB_CSR ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.S, N),
//      CSRRC  -> List(Y, BR_N  , OP1_RS1, OP2_X     ,  Y   ,  Y   , ALU_COPY_1 , WB_CSR ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.C, N),
//      CSRRCI -> List(Y, BR_N  , OP1_X  , IMM_ZEXT  ,  Y   ,  Y   , ALU_COPY_2 , WB_CSR ,  Y   ,  N   , MEM_NOP   , MT_X  , CSR.C, N),
//      ECALL  -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.I, N),
//      MRET   -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.I, N),
//      URET   -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.I, N),
//      EBREAK -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.I, N),
//      WFI    -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, N), // implemented as a NOP
//
//      FENCE_I-> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  N   , MEM_NOP   , MT_X  , CSR.N, Y),
//      // kill pipeline and refetch instructions since the pipeline will be holding stall instructions.
//      FENCE  -> List(Y, BR_N  , OP1_X  , OP2_X     ,  N   ,  N   , ALU_X      , WB_X   ,  N   ,  Y   , MEM_NOP   , MT_X  , CSR.N, N)
//      // we are already sequentially consistent, so no need to honor the fence instruction
//  )
}

//class decode extends Module with defines.decodeConsts {
//  val io = IO(new Bundle {
//    val ifetch = new defines.IFetch
//    val decodeOps = Output(new defines.DecodeOps)
//  })
//  // 如果需要Chisel自动推断类型，那么也需要有推断的根据，不能两边都是空的
//  io.decodeOps.pc := io.ifetch.pc
//  io.decodeOps.nPcSel := pc_br
//  io.decodeOps.instValid := true.B
//}
//
//object decode extends App {
//  chisel3.Driver.execute(args, ()=>new decode)
//}
