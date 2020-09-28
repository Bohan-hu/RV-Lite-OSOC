package core
import chisel3._
import chisel3.stage.ChiselStage
class top extends Module {
  val io = IO(new Bundle() {
    val pc = Output(UInt(64.W))
    val decodeOps = Output(UInt())
  })
  val ifu = Module(new IFU)
  val imem = Module(new SyncReadOnlyMem)
  val decoder = Module(new Decode)
  val regfile = Module(new Regfile)
  // IMEM < clk
  imem.io.clk := clock.asBool()
  imem.io.reset := reset.asBool()

  // IFU <> IMEM
  imem.io.rreq := ifu.io.inst_req
  imem.io.raddr := ifu.io.inst_pc
  ifu.io.rvalid:= imem.io.data_valid
  ifu.io.rdata := imem.io.rdata

  // IFU <> DECODER
  decoder.io.inst := ifu.io.inst
  decoder.io.instValid := ifu.io.instValid
  decoder.io.pc := ifu.io.inst_pc
  decoder.io.regfileIO <> regfile.io.rdPort

  // dummy
  val dummy_rfwr = Wire(new RegWrite)
  dummy_rfwr.wen := false.B
  dummy_rfwr.waddr := 0.U
  dummy_rfwr.wdata := 0.U

  regfile.io.wrPort <> dummy_rfwr

  io.pc := ifu.io.inst_pc
}

object top extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new top)
}
