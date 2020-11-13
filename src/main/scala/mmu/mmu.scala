package mmu
import chisel3._
import _root_.core.MEM2MMU
import _root_.core.CSRMMU
import chisel3.stage.ChiselStage
import _root_.core.MEM2dmem
class MMUIO extends Bundle {
  val mem2mmu = Flipped(new MEM2MMU)
  val isStore = Input(Bool())
  val flush = Input(Bool())
  val dmemreq = new MEM2dmem
  val csr2mmu = Flipped(new CSRMMU)
}
// LSU send VAddr to MMU, MMU returns the PAddr with valid signal
// LSU then use the translated address to access the memory
// When LSU is accessing memory, the bus is busy, and the IF is paused
// When IF is busy, and a load/store reached the execution stage, ?
// PTW need to offer the addr handshake and the data handshake interface
// IFU send PC to MMU, MMU returns the PAddr
// LSU AddrTranslate -> Send Load -> Get response / Exception
// If LSU is busy, inst req is blocked

class MMU (isDMMU: Boolean) extends Module {
  val io = IO(new MMUIO)
  // TODO： Add TLB Here
  val tlb = Module(new TLB)
  val ptw = Module(new PTW(isDMMU))
  // PTW <> MMU
  ptw.io.reqReady          := io.mem2mmu.reqReady
  ptw.io.reqVAddr          := io.mem2mmu.reqVAddr
  io.mem2mmu.respPAddr     := ptw.io.respPaddr
  io.mem2mmu.respValid     := ptw.io.respValid
  io.mem2mmu.respPageFault := ptw.io.pageFault
  
  // TLB
  ptw.io.tlbQuery <> tlb.io.tlbQuery
  ptw.io.tlbUpdate <> tlb.io.tlbUpdate
  ptw.io.flush := io.flush
  tlb.io.flush := io.flush
  // CSR ----> PTW Signals
  ptw.io.enableSv39        := io.csr2mmu.enableSv39
  ptw.io.translation_ls_en := io.csr2mmu.enableLSVM
  ptw.io.satp_PPN          := io.csr2mmu.satpPPN
  ptw.io.mxr               := io.csr2mmu.mxr
  ptw.io.satp_ASID         := io.csr2mmu.asid
  
  // ptw ctrl
  ptw.io.flush             := io.flush
  if(!isDMMU) {
    ptw.io.reqIsStore      := io.isStore
  } else {
    ptw.io.reqIsStore      := false.B
  }
  io.dmemreq <> ptw.io.memReq
  
}

object MMU extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new MMU(false))
}
