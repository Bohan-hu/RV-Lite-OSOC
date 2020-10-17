package mmu
import chisel3._
class MMUIO extends Bundle {
  val addrReq = Input(Bool())
  val vAddr = Input(UInt(64.W))
  val storeReq = Input(Bool())
  val enableSv39 = Input(Bool())
  val enableLSPaged = Input(Bool())
}
// LSU send VAddr to MMU, MMU returns the PAddr with valid signal
// LSU then use the translated address to access the memory
// When LSU is accessing memory, the bus is busy, and the IF is paused
// When IF is busy, and a load/store reached the execution stage, ?
// PTW need to offer the addr handshake and the data handshake interface
// IFU send PC to MMU, MMU returns the PAddr
// LSU AddrTranslate -> Send Load -> Get response / Exception
// If LSU is busy, inst req is blocked

class MMU (isDMMU: Boolean)extends Module {
  val io = IO(new MMUIO)
  val tlb = Module(new TLB)


}
