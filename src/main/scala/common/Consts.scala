package common {

  import chisel3._

  object OpConstants {

    //************************************
    // Control Signals
    val Y = true.B
    val N = false.B

    // PC Select Signal
    val PC_4     = 0.asUInt(2.W)  // PC + 4
    val PC_BRJMP = 1.asUInt(2.W)  // brjmp_target
    val PC_JALR  = 2.asUInt(2.W)  // jump_reg_target
    val PC_EXC   = 3.asUInt(2.W)  // exception

    // Branch Type
    val BR_N   = 0.asUInt(4.W)  // Next
    val BR_NE  = 1.asUInt(4.W)  // Branch on NotEqual
    val BR_EQ  = 2.asUInt(4.W)  // Branch on Equal
    val BR_GE  = 3.asUInt(4.W)  // Branch on Greater/Equal
    val BR_GEU = 4.asUInt(4.W)  // Branch on Greater/Equal Unsigned
    val BR_LT  = 5.asUInt(4.W)  // Branch on Less Than
    val BR_LTU = 6.asUInt(4.W)  // Branch on Less Than Unsigned
    val BR_J   = 7.asUInt(4.W)  // Jump
    val BR_JR  = 8.asUInt(4.W)  // Jump Register

    // Operand #1 Select Signal
    // Operand #1 could be RS1, PC.
    val OP1_RS1 = 0.asUInt(2.W)  // Register Source #1
    val OP1_PC  = 1.asUInt(2.W)  // PC
    val OP1_X   = 0.asUInt(2.W)  // Don't care

    // Operand #2 Select Signal
    // Operand #2 could be RS2, sign-extended or zero-extended(for CSRs) immediate
    val OP2_RS2   = 0.asUInt(3.W)  // Register Source #2
    val IMM_ITYPE = 1.asUInt(3.W)  // I-type
    val IMM_STYPE = 2.asUInt(3.W)  // S-type
    val IMM_BTYPE = 3.asUInt(3.W)  // B-type
    val IMM_UTYPE = 4.asUInt(3.W)  // U-type
    val IMM_JTYPE = 5.asUInt(3.W)  // J-type
    val IMM_ZEXT  = 6.asUInt(3.W)  // Zero-extended Immediate from RS1 field, for use by CSRI instructions
    val IMM_ZERO  = 7.asUInt(3.W)  // Zero-extended Immediate from RS1 field, for use by CSRI instructions
    val OP2_X     = 0.asUInt(3.W)

    // ALU Operation Signal
    val ALU_ADD    = 0.asUInt(4.W)
    val ALU_SUB    = 1.asUInt(4.W)
    val ALU_SLL    = 2.asUInt(4.W)
    val ALU_SRL    = 3.asUInt(4.W)
    val ALU_SRA    = 4.asUInt(4.W)
    val ALU_AND    = 5.asUInt(4.W)
    val ALU_OR     = 6.asUInt(4.W)
    val ALU_XOR    = 7.asUInt(4.W)
    val ALU_SLT    = 8.asUInt(4.W)
    val ALU_SLTU   = 9.asUInt(4.W)
    val ALU_COPY_1 = 10.asUInt(4.W)
    val ALU_COPY_2 = 11.asUInt(4.W)
    val ALU_X      = 0.asUInt(4.W)

    val LSU_X     = 0.asUInt(4.W)
    val LSU_LOAD  = 1.asUInt(4.W)
    val LSU_STORE = 2.asUInt(4.W)
    val LSU_LR    = 3.asUInt(4.W)
    val LSU_SC    = 4.asUInt(4.W)
    val LSU_ASWAP = 5.asUInt(4.W)
    val LSU_AADD  = 6.asUInt(4.W)
    val LSU_AXOR  = 7.asUInt(4.W)
    val LSU_AAND  = 8.asUInt(4.W)
    val LSU_AOR   = 9.asUInt(4.W)
    val LSU_AMIN  = 10.asUInt(4.W)
    val LSU_AMAX  = 11.asUInt(4.W)
    val LSU_AMINU = 12.asUInt(4.W)
    val LSU_AMAXU = 13.asUInt(4.W)

    // ALU Type
    val OP_ARITH   = 0.asUInt(3.W)
    val OP_COMPARE = 1.asUInt(3.W)
    val OP_LOGIC   = 2.asUInt(3.W)
    val OP_SHIFT   = 3.asUInt(3.W)
    val OP_COPY    = 4.asUInt(3.W)
    val OP_BR      = 5.asUInt(3.W)

    // Writeback Select Signal
    val WB_X   = 0.asUInt(2.W)
    val WB_ALU = 0.asUInt(2.W)
    val WB_MEM = 1.asUInt(2.W)
    val WB_PC4 = 2.asUInt(2.W)
    val WB_CSR = 3.asUInt(2.W)

    // Mem Op
    val MEM_NOP   = 0.asUInt(2.W)
    val MEM_READ  = 1.asUInt(2.W)
    val MEM_WRITE = 2.asUInt(2.W)
    val MEM_AMO   = 3.asUInt(2.W)

    // Mem Size and sign
    val SZ_X  = 0.asUInt(3.W)
    val SZ_W  = 0.asUInt(3.W)
    val SZ_B  = 1.asUInt(3.W)
    val SZ_H  = 2.asUInt(3.W)
    val SZ_HU = 3.asUInt(3.W)
    val SZ_BU = 4.asUInt(3.W)
    val SZ_D  = 5.asUInt(3.W)
    val SZ_WU = 6.asUInt(3.W)

    val CSR_X  = 0.asUInt(3.W)
    val CSR_W  = 1.asUInt(3.W)
    val CSR_S  = 2.asUInt(3.W)
    val CSR_C  = 3.asUInt(3.W)
    val CSR_SI = 4.asUInt(3.W)
    val CSR_CI = 5.asUInt(3.W)
    val CSR_I  = 6.asUInt(3.W)  // Trp

    val FU_ALU = 0.asUInt(2.W)
    val FU_LSU = 1.asUInt(2.W)
    val FU_MUL = 2.asUInt(2.W)
    val FU_DIV = 3.asUInt(2.W)

    val MDU_MUL    = 0.asUInt(4.W)
    val MDU_MULH   = 1.asUInt(4.W)
    val MDU_MULHS  = 2.asUInt(4.W)
    val MDU_MULHSU = 3.asUInt(4.W)
    val MDU_MULHU  = 4.asUInt(4.W)
    val MDU_MULW   = 5.asUInt(4.W)
    val MDU_DIV    = 6.asUInt(4.W)
    val MDU_DIVU   = 7.asUInt(4.W)
    val MDU_REM    = 8.asUInt(4.W)
    val MDU_REMU   = 9.asUInt(4.W)
    val MDU_DIVW   = 10.asUInt(4.W)
    val MDU_DIVUW  = 11.asUInt(4.W)
    val MDU_REMW   = 12.asUInt(4.W)
    val MDU_REMUW  = 13.asUInt(4.W)
  }

}