package core
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import common.SyncReadWriteMem
import mmu._
import bus.AXIMaster
import bus.AXIBridge
import bus.AXILiteMaster

class Top extends Module {
  val io = IO(new Bundle() {
    // val pc = Output(UInt(64.W))
    // val instBundleOut = Output(new InstBundle)
    val axiMaster = new AXIMaster
    val axiLiteMaster = new AXILiteMaster
    val meip = Input(Bool())
  })
  val axiBridge = Module(new AXIBridge)
  io.axiMaster <> axiBridge.io.axiMaster
  io.axiLiteMaster <> axiBridge.io.axiLiteMaster
  val ifuTop = Module(new IFUTop)
  val ifuReq = Flipped(new NaiveBusM2S)
  val lsuReq = Flipped(new NaiveBusM2S)
  // val
  val decoder = Module(new Decode)
  val exu = Module(new EXU)
  val wb = Module(new WB)
  val regfile = Module(new Regfile)
  val csrFile = Module(new CSRFile)
  csrFile.io.meip := io.meip
  ifuTop.io.branchRedir := exu.io.exe2IF
  ifuTop.io.exceptionRedir := csrFile.io.ifRedir
  ifuTop.io.pause := exu.io.pauseReq
  ifuTop.io.csr2mmu <> csrFile.io.csrMMU
  ifuTop.io.flush := false.B


  // IFU <> DECODER
  decoder.io.instBundleIn := ifuTop.io.inst_out
  decoder.io.intCtrl <> csrFile.io.intCtrl
  decoder.io.regfileIO <> regfile.io.rdPort
  decoder.io.exceptionInfoIF <> ifuTop.io.exceInfoOut
  decoder.io.PLIC_SEI := false.B
  decoder.io.decodePrivCheck <> csrFile.io.decodePrivCheck

  // DECODER <> EXU
  exu.io.instBundleIn := decoder.io.instBundleOut
  exu.io.decode2Exe := decoder.io.decode2Exe
  exu.io.flush := csrFile.io.ifRedir.redir
  exu.io.csr2mmu <> csrFile.io.csrMMU
  exu.io.commit2Exe := wb.io.commit2Exe

  // MEM <> WB / MEM <> dmem
  wb.io.instBundleIn := exu.io.instBundleOut
  wb.io.exe2Commit := exu.io.exe2Commit
  wb.io.regfileWrite <> regfile.io.wrPort

  // WB <> csr
  wb.io.csrRw <> csrFile.io.commitCSR
  // dmem.io.mem2dmem <> exu.io.mem2dmem
  axiBridge.io.ifuPort <> ifuTop.io.IFUMemReq
  axiBridge.io.lsuPort <> exu.io.mem2dmem

  // io.pc := wb.io.instBundleOut.inst_pc
  BoringUtils.addSource(RegNext(wb.io.instBundleOut.inst_pc), "difftestThisPC")
  BoringUtils.addSource(RegNext(wb.io.instBundleOut.inst), "difftestThisINST")

  // io.instBundleOut := wb.io.instBundleOut

  // Consts
  // TODO: TEST
  val clint = Module(new CLINT)
  csrFile.io.clintIn := clint.io.tocsr
  clint.io.memport <> axiBridge.io.toClint

}

object Top extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Top)
}
