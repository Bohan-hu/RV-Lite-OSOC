package core

import chisel3._

class RegRead extends Bundle {
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(64.W))
  val rdata2 = Output(UInt(64.W))
}

class RegWrite extends Bundle {
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
  val wdata = Input(UInt(64.W))
}

class RegFileIO extends Bundle with ConfigParams {
  val rdPort = new RegRead
  val wrPort = new RegWrite
}

class Regfile extends Module {
  val io = IO(new RegFileIO)
  val Regs = Mem(32, UInt(64.W))
  io.rdPort.rdata1 :=
    Mux(io.rdPort.raddr1.orR,
      Mux(io.rdPort.raddr1 === io.wrPort.waddr,
        io.wrPort.wdata,
        Regs(io.rdPort.raddr1)),
      0.U)
  io.rdPort.rdata2 :=
    Mux(io.rdPort.raddr2.orR,
      Mux(io.rdPort.raddr2 === io.wrPort.waddr,
        io.wrPort.wdata,
        Regs(io.rdPort.raddr2)),
      0.U)
  when(io.wrPort.wen & io.wrPort.waddr.orR) {
    Regs(io.wrPort.waddr) := io.wrPort.wdata
  }
}

object Regfile extends App {
  val stage = new chisel3.stage.ChiselStage()
  stage.emitVerilog(new Regfile)
}
