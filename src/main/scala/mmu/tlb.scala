package mmu
import chisel3._
class TLBEntry extends Bundle {
  val valid = Bool()
  val vpn = UInt(27.W)
  val asid =  UInt(16.W)      // From SATP
  val pte = new PTE
}
class TLBUpdate extends Bundle {
  val enable = Input(Bool())
  val addr = Input(UInt(64.W))
  val entry = Input(new TLBEntry)
}
class TLBQuery extends Bundle {
  val hit = Output(Bool())
  val vaddr = Input(UInt(64.W))
  val paddr = Output(UInt(64.W))
}
//
class tlb {

}
