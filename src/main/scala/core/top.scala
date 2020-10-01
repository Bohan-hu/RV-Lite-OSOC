package core
import chisel3._
import chisel3.stage.ChiselStage
class top extends Module {
  val io = IO(new Bundle() {
    val pc = Output(UInt(64.W))
    val instBundleOut = Output(new InstBundle)
  })
  val ifu = Module(new IFU)
  val imem = Module(new SyncReadOnlyMem)
  val decoder = Module(new Decode)
  val exu = Module(new EXU)
  val mem = Module(new MEM)
  val wb = Module(new WB)
  val regfile = Module(new Regfile)
  val branchRedir = Wire(new BranchRedir)
  val exceptionRedir = Wire(new ExceptionRedir)

  branchRedir.redir := false.B
  exceptionRedir.redir := false.B
  branchRedir.TargetPC := 0.U
  exceptionRedir.excePC := 0.U

  // IMEM < clk
  imem.io.clk := clock.asBool()
  imem.io.reset := reset.asBool()

  // IFU <> IMEM
  imem.io.rreq := ifu.io.inst_req
  imem.io.raddr := ifu.io.inst_pc
  ifu.io.rvalid:= imem.io.data_valid
  ifu.io.rdata := imem.io.rdata
  ifu.io.branchRedir := branchRedir
  ifu.io.exceptionRedir := exceptionRedir

  // IFU <> DECODER
  decoder.io.instBundleIn := ifu.io.inst_out
  decoder.io.regfileIO <> regfile.io.rdPort

  // DECODER <> EXU
  exu.io.instBundleIn := decoder.io.instBundleOut
  exu.io.decode2Exe := decoder.io.decode2Exe

  // EXU <> MEM
  mem.io.instBundleIn := exu.io.instBundleOut
  mem.io.exe2Mem := exu.io.exe2Mem

  // MEM <> WB
  wb.io.instBundleIn := exu.io.instBundleOut
  wb.io.mem2Wb := mem.io.mem2Wb
  wb.io.regfileWrite <> regfile.io.wrPort

  io.pc := ifu.io.inst_pc
  io.instBundleOut := wb.io.instBundleOut
}

object top extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new top)
}
