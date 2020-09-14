package common

import chisel3._
import chisel3.util._

object Instructions {
  // Loads
  val LB = BitPat("b?????????????????000?????0000011")
  val LH = BitPat("b?????????????????001?????0000011")
  val LW = BitPat("b?????????????????010?????0000011")
  val LD = BitPat("b?????????????????011?????0000011")
  val LBU = BitPat("b?????????????????100?????0000011")
  val LHU = BitPat("b?????????????????101?????0000011")
  val LWU = BitPat("b?????????????????110?????0000011")

  // Stores
  val SB = BitPat("b?????????????????000?????0100011")
  val SH = BitPat("b?????????????????001?????0100011")
  val SW = BitPat("b?????????????????010?????0100011")
  val SD = BitPat("b?????????????????011?????0100011")

  // Shifts
  val SLL = BitPat("b0000000??????????001?????0110011")
  val SLLW = BitPat("b0000000??????????001?????0111011")
  val SLLI = BitPat("b000000???????????001?????0010011")
  val SLLIW = BitPat("b0000000??????????001?????0011011")
  val SRL = BitPat("b0000000??????????101?????0110011")
  val SRLW = BitPat("b0000000??????????101?????0111011")
  val SRLI = BitPat("b000000???????????101?????0010011")
  val SRLIW = BitPat("b0000000??????????101?????0011011")
  val SRA = BitPat("b0100000??????????101?????0110011")
  val SRAW = BitPat("b0100000??????????101?????0111011")
  val SRAI = BitPat("b010000???????????101?????0010011")
  val SRAIW = BitPat("b0100000??????????101?????0011011")

  // Arithmetic
  val ADD = BitPat("b0000000??????????000?????0110011")
  val ADDW = BitPat("b0000000??????????000?????0111011")
  val ADDI = BitPat("b?????????????????000?????0010011")
  val ADDIW = BitPat("b?????????????????000?????0011011")
  val SUB = BitPat("b0100000??????????000?????0110011")
  val SUBW = BitPat("b0100000??????????000?????0111011")
  val LUI = BitPat("b?????????????????????????0110111")
  val AUIPC = BitPat("b?????????????????????????0010111")

  // Logical
  val XOR = BitPat("b0000000??????????100?????0110011")
  val XORI = BitPat("b?????????????????100?????0010011")
  val OR = BitPat("b0000000??????????110?????0110011")
  val ORI = BitPat("b?????????????????110?????0010011")
  val AND = BitPat("b0000000??????????111?????0110011")
  val ANDI = BitPat("b?????????????????111?????0010011")
  // Compare
  val SLT = BitPat("b0000000??????????010?????0110011")
  val SLTI = BitPat("b?????????????????010?????0010011")
  val SLTU = BitPat("b0000000??????????011?????0110011")
  val SLTIU = BitPat("b?????????????????011?????0010011")
  // Branches
  val BEQ = BitPat("b?????????????????000?????1100011")
  val BNE = BitPat("b?????????????????001?????1100011")
  val BLT = BitPat("b?????????????????100?????1100011")
  val BGE = BitPat("b?????????????????101?????1100011")
  val BLTU = BitPat("b?????????????????110?????1100011")
  val BGEU = BitPat("b?????????????????111?????1100011")
  // Jump & Link
  val JALR = BitPat("b?????????????????000?????1100111")
  val JAL = BitPat("b?????????????????????????1101111")
  // Synch
  val FENCE = BitPat("b?????????????????000?????0001111")
  val FENCE_I = BitPat("b?????????????????001?????0001111")
  // ZiCSR
  val CSRRW = BitPat("b?????????????????001?????1110011")
  val CSRRS = BitPat("b?????????????????010?????1110011")
  val CSRRC = BitPat("b?????????????????011?????1110011")
  val CSRRWI = BitPat("b?????????????????101?????1110011")
  val CSRRSI = BitPat("b?????????????????110?????1110011")
  val CSRRCI = BitPat("b?????????????????111?????1110011")
  // RVM（暂不需要）
  val MUL = BitPat("b0000001??????????000?????0110011")
  val MULW = BitPat("b0000001??????????000?????0111011")
  val MULH = BitPat("b0000001??????????001?????0110011")
  val MULHSU = BitPat("b0000001??????????010?????0110011")
  val MULHU = BitPat("b0000001??????????011?????0110011")
  val DIV = BitPat("b0000001??????????100?????0110011")
  val DIVW = BitPat("b0000001??????????100?????0111011")
  val DIVU = BitPat("b0000001??????????101?????0110011")
  val DIVUW = BitPat("b0000001??????????101?????0111011")
  val REM = BitPat("b0000001??????????110?????0110011")
  val REMW = BitPat("b0000001??????????110?????0111011")
  val REMU = BitPat("b0000001??????????111?????0110011")
  val REMUW = BitPat("b0000001??????????111?????0111011")
  // SYSTEM Instruction
  val ECALL = BitPat("b00000000000000000000000001110011")
  val EBREAK = BitPat("b00000000000100000000000001110011")
  // TRAP RETURN INSTURCTIONS
  val URET = BitPat("b00000000001000000000000001110011")
  val SRET = BitPat("b00010000001000000000000001110011")
  val MRET = BitPat("b00110000001000000000000001110011")
  // SUPERVISOR MEMORY MANAGEMENT INSTRUCTION
  val SFENCE_VMA = BitPat("b0001001??????????000000001110011")
  // INTERRUPT MANAGEMENT INSTRUCTION
  val WFI = BitPat("b00010000010100000000000001110011")


}

abstract class Instruction extends Bundle {
  // Should implement the logic of immediate generation logic
  def imm_gen: UInt

  def imm_ext = util.signExt64(imm_gen)
}

class RTypeInstruction extends Instruction {
  val funct7 = UInt(7.W)
  val rs2 = UInt(5.W)
  val rs1 = UInt(5.W)
  val funct3 = UInt(3.W)
  val rd = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = 0.U
}

class ITypeInstruction extends Instruction {
  val imm = UInt(12.W)
  val rs1 = UInt(5.W)
  val funct3 = UInt(3.W)
  val rd = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = imm
}

class STypeInstruction extends Instruction {
  val imm_hi = UInt(7.W)
  val rs2 = UInt(5.W)
  val rs1 = UInt(5.W)
  val funct3 = UInt(3.W)
  val imm_lo = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = Cat(imm_hi, imm_lo)
}

class BTypeInstruction extends Instruction {
  val imm_12 = UInt(1.W)
  val imm_10_5 = UInt(6.W)
  val rs2 = UInt(5.W)
  val rs1 = UInt(5.W)
  val funct3 = UInt(3.W)
  val imm_4_1 = UInt(4.W)
  val imm_11 = UInt(1.W)
  val opcode = UInt(7.W)

  def imm_gen = Cat(imm_12, imm_11, imm_10_5, imm_4_1, 0.U(1.W))
}

class UTypeInstruction extends Instruction {
  val imm = UInt(20.W)
  val rd = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = Cat(imm, 0.U(12.W))
}

class JTypeInstruction extends Instruction {
  val imm_20 = UInt(1.W)
  val imm_10_1 = UInt(10.W)
  val imm_11 = UInt(1.W)
  val imm_19_12 = UInt(8.W)
  val rd = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = Cat(imm_20, imm_19_12, imm_11, imm_10_1, 0.U(1.W))
}

class CSRRInstruction extends Instruction {
  val csr = UInt(12.W)
  val rs1 = UInt(5.W)
  val funct3 = UInt(3.W)
  val rd = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = 0.U
}

class CSRIInstruction extends Instruction {
  val csr = UInt(12.W)
  val uimm = UInt(5.W)
  val funct3 = UInt(3.W)
  val rd = UInt(5.W)
  val opcode = UInt(7.W)

  def imm_gen = 0.U

  override def imm_ext = WireInit(UInt(64.W), uimm)
}

class test extends Module {
  val io = IO(new Bundle() {
    val instr = Input(UInt(32.W))
    val detached = Output(new CSRIInstruction)
    val imm = Output(UInt(64.W))
  })
  io.detached := io.instr.asTypeOf(new CSRIInstruction)
  io.imm := io.detached.imm_ext
}

object test extends App {
  val stage = new chisel3.stage.ChiselStage()
  stage.emitVerilog(new test)
}