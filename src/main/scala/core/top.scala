package core
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import common.SyncReadWriteMem
class Top extends Module {
  val io = IO(new Bundle() {
    val pc = Output(UInt(64.W))
    val instBundleOut = Output(new InstBundle)
  })
  val ifu = Module(new IFU)
  val imem = Module(new common.SyncReadOnlyMem)
  val dmem = Module(new common.SyncReadWriteMem)
  val decoder = Module(new Decode)
  val exu = Module(new EXU)
  val mem = Module(new MEM)
  val wb = Module(new WB)
  val regfile = Module(new Regfile)
  val csrFile = Module(new CSRFile)

  // IMEM < clk
  imem.io.clk := clock.asBool()
  imem.io.reset := reset.asBool()
  imem.io.pause := mem.io.pauseReq || exu.io.pauseReq
  // IFU <> IMEM
  imem.io.rreq := ifu.io.inst_req
  imem.io.raddr := (ifu.io.inst_pc - 0x80000000L.U(64.W))
  ifu.io.rvalid:= imem.io.data_valid
  ifu.io.rdata := imem.io.rdata
  ifu.io.branchRedir := exu.io.exe2IF
  ifu.io.exceptionRedir := csrFile.io.ifRedir
  ifu.io.pause := mem.io.pauseReq || exu.io.pauseReq  // Todo: Modify as 5 stage pipeline

  // IFU <> DECODER
  decoder.io.instBundleIn := ifu.io.inst_out
  decoder.io.intCtrl <> csrFile.io.intCtrl
  decoder.io.regfileIO <> regfile.io.rdPort
  decoder.io.exceptionInfoIF <> ifu.io.exceInfo
  decoder.io.PLIC_SEI := false.B

  // DECODER <> EXU
  exu.io.instBundleIn := decoder.io.instBundleOut
  exu.io.decode2Exe := decoder.io.decode2Exe

  // EXU <> MEM
  mem.io.instBundleIn := exu.io.instBundleOut
  mem.io.exe2Mem := exu.io.exe2Mem

  // MEM <> WB / MEM <> dmem
  wb.io.instBundleIn := mem.io.instBundleOut
  wb.io.mem2Wb := mem.io.mem2Wb
  wb.io.regfileWrite <> regfile.io.wrPort

  // WB <> csr
  wb.io.csrRw <> csrFile.io.commitCSR
  dmem.io.clk := clock.asBool()
  dmem.io.reset := reset.asBool()
  dmem.io.mem2dmem <> mem.io.mem2dmem

  io.pc := wb.io.instBundleOut.inst_pc
  BoringUtils.addSource(RegNext(wb.io.instBundleOut.inst_pc), "difftestThisPC")
  BoringUtils.addSource(RegNext(wb.io.instBundleOut.inst), "difftestThisINST")

  io.instBundleOut := wb.io.instBundleOut

  // Consts
  // TODO: TEST
  val clint_tmp = Module(new CLINT)
  clint_tmp.io.tocsr <> csrFile.io.clintIn
  clint_tmp.io.memport <> mem.io.toclint

}

object Top extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Top)
}
