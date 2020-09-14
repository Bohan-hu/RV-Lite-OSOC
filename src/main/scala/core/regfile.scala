package core

import chisel3._

class RegFileIO extends Bundle with ConfigParams {
  val raddr1 = Input(UInt(XLEN.W))
  val raddr2 = Input(UInt(PHY_REG_IDX_LEN.W))
  val rdata1 = Output(UInt(XLEN.W))
  val rdata2 = Output(UInt(XLEN.W))
  val wen = Input(Bool())
  val waddr = Input(UInt(PHY_REG_IDX_LEN.W))
  val wdata = Input(UInt(XLEN.W))
}

class Regfile extends Module {
  val io = IO(new RegFileIO) // 以后如果需要改成多发射，直接改变这个IO的数量即可
  val Regs = Mem(32, UInt(64.W))
  io.rdata1 := Mux(io.raddr1.orR, Regs(io.raddr1), 0.U)
  io.rdata2 := Mux(io.raddr2.orR, Regs(io.raddr2), 0.U)
  when(io.wen & io.waddr.orR) {
    Regs(io.waddr) := io.wdata
  }
}

object Regfile extends App {
  val stage = new chisel3.stage.ChiselStage()
  stage.emitVerilog(new Regfile)}
