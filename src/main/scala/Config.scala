package core
import chisel3._
import chisel3.util._

trait ConfigParams {
  val XLEN = 64
  val NUM_PHY_REGS = 32
  val PHY_REG_IDX_LEN = log2Up(NUM_PHY_REGS)
  val ALUOPWidth = 5
}