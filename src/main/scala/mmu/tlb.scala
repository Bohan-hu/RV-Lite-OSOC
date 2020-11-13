package mmu
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
class TLBEntry extends Bundle {
  val valid = Bool()
  val vpn = UInt(27.W)
  val pte = new PTE
  val is4K = Bool()
  val is2M = Bool()
  val is1G = Bool()
}

class TLBQuery extends Bundle {
  val hit = Output(Bool())
  val vaddr = Input(UInt(64.W))
  val paddr = Output(UInt(64.W))
}
//
class TLB extends Module{
  val io = IO(new Bundle() {
    val tlbUpdate = Input(new TLBEntry)
    val tlbQuery = new TLBQuery
    val flush = Input(Bool())
  })

  val vpn0In = io.tlbQuery.vaddr(20,12)
  val vpn1In = io.tlbQuery.vaddr(29,21)
  val vpn2In = io.tlbQuery.vaddr(38,30)
  
  val cnt = Reg(UInt(3.W))    // FIFO

  val tlbUpdate = WireInit(io.tlbUpdate)

  val tlbRegs = Mem(8, new TLBEntry)
  when(io.tlbUpdate.valid) {
    tlbRegs(cnt) := tlbUpdate
    tlbRegs(cnt).valid := true.B
    cnt := cnt + 1.U
  }
  io.tlbQuery.hit := false.B
  io.tlbQuery.paddr := 0.U
  val tlbEntry = Wire(new TLBEntry)
  val tlbEntryTag = Wire(UInt(39.W))
  val vpn0_hit = Wire(Bool())
  val vpn1_hit = Wire(Bool())
  val vpn2_hit = Wire(Bool())
  for( i <- 0 until 8 ) {
    tlbEntry := tlbRegs(i)
    tlbEntryTag := Cat(tlbEntry.vpn, 0.U(12.W))
    vpn0_hit := (vpn0In === tlbEntryTag(20,12))
    vpn1_hit := (vpn1In === tlbEntryTag(29,21))
    vpn2_hit := (vpn2In === tlbEntryTag(38,30))
    when(vpn2_hit && tlbEntry.valid) {
      when(tlbEntry.is1G) {
        // 1G Page
        io.tlbQuery.hit := true.B
        io.tlbQuery.paddr := Cat(tlbEntry.pte.ppn2, io.tlbQuery.vaddr(29,0))
      }.elsewhen(vpn1_hit) {
        when(tlbEntry.is2M) {
          // 2M Page
          io.tlbQuery.hit := true.B
          io.tlbQuery.paddr := Cat(tlbEntry.pte.ppn2, tlbEntry.pte.ppn1, io.tlbQuery.vaddr(20,0))
        }.elsewhen(vpn0_hit) {
          // 4K Page
          io.tlbQuery.hit := true.B
          io.tlbQuery.paddr := Cat(tlbEntry.pte.ppn2, tlbEntry.pte.ppn1, tlbEntry.pte.ppn0, io.tlbQuery.vaddr(11,0))
        }
      }
    }
    when(io.flush) {
      tlbRegs(i).valid := false.B
    }
  }
}

object tlb extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new TLB)
}
