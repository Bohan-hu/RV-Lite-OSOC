package core
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import common.SyncReadWriteMem
import mmu._

class Top extends Module {
  val io = IO(new Bundle() {
    val pc = Output(UInt(64.W))
    val instBundleOut = Output(new InstBundle)
  })
  val ifu = Module(new IFU)
  val imem = Module(new common.SyncReadOnlyMem)
  val immu = Module(new MMU(isDMMU = false))
  val dmem = Module(new common.SyncReadWriteMem)
  val decoder = Module(new Decode)
  val exu = Module(new EXU)
  val wb = Module(new WB)
  val regfile = Module(new Regfile)
  val csrFile = Module(new CSRFile)

  // IMEM < clk
  imem.io.clk := clock.asBool()
  imem.io.reset := reset.asBool()
  imem.io.pause := exu.io.pauseReq
  // IFU <> IMEM

  ifu.io.ifu2dmem.memRvalid:= imem.io.data_valid
  ifu.io.ifu2dmem.memRdata := imem.io.rdata
  ifu.io.ifu2dmem.memWrDone := false.B
  immu.io.dmemreq.memRvalid:= imem.io.data_valid
  immu.io.dmemreq.memRdata := imem.io.rdata
  immu.io.dmemreq.memWrDone := false.B
  immu.io.isStore := false.B
  immu.io.flush := csrFile.io.ifRedir.redir
  immu.io.csr2mmu <> csrFile.io.csrMMU
  ifu.io.branchRedir := exu.io.exe2IF
  ifu.io.exceptionRedir := csrFile.io.ifRedir
  ifu.io.pause := exu.io.pauseReq
  ifu.io.ifu2mmu <> immu.io.mem2mmu
  ifu.io.intCtrl <> csrFile.io.intCtrl

  imem.io.rreq := ifu.io.ifu2dmem.memRreq | immu.io.dmemreq.memRreq
  imem.io.raddr := Mux(immu.io.dmemreq.memRreq, immu.io.dmemreq.memAddr, ifu.io.ifu2dmem.memAddr)
  

  // IFU <> DECODER
  decoder.io.instBundleIn := ifu.io.inst_out
  decoder.io.intCtrl <> csrFile.io.intCtrl
  decoder.io.regfileIO <> regfile.io.rdPort
  decoder.io.exceptionInfoIF <> ifu.io.exceInfoOut
  decoder.io.PLIC_SEI := false.B
  decoder.io.decodePrivCheck <> csrFile.io.decodePrivCheck

  // DECODER <> EXU
  exu.io.instBundleIn := decoder.io.instBundleOut
  exu.io.decode2Exe := decoder.io.decode2Exe
  exu.io.flush := csrFile.io.ifRedir.redir
  exu.io.csr2mmu <> csrFile.io.csrMMU
  exu.io.commit2Exe := wb.io.commit2Exe
  exu.io.intCtrl <> csrFile.io.intCtrl
  // MEM <> WB / MEM <> dmem
  wb.io.instBundleIn := exu.io.instBundleOut
  wb.io.exe2Commit := exu.io.exe2Commit
  wb.io.regfileWrite <> regfile.io.wrPort

  // WB <> csr
  wb.io.csrRw <> csrFile.io.commitCSR
  dmem.io.clk := clock.asBool()
  dmem.io.reset := reset.asBool()
  dmem.io.mem2dmem <> exu.io.mem2dmem

  io.pc := wb.io.instBundleOut.inst_pc
  BoringUtils.addSource(RegNext(wb.io.instBundleOut.inst_pc), "difftestThisPC")
  BoringUtils.addSource(RegNext(wb.io.instBundleOut.inst), "difftestThisINST")

  io.instBundleOut := wb.io.instBundleOut

  // Consts
  val clint_tmp = Module(new CLINT)
  csrFile.io.clintIn := clint_tmp.io.tocsr
  clint_tmp.io.memport <> exu.io.toclint

}

object Top extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Top)
}
