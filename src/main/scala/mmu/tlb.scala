package mmu
import chisel3._
import chisel3.util.Counter
class TLBEntry extends Bundle {
  val valid = Bool()
  val vpn = UInt(27.W)
  val pte = new PTE
  val is4K = Bool()
  val is2M = Bool()
  val is1G = Bool()
}

class TLBUpdate extends Bundle {
  val enable = Input(Bool())
  val entry = Input(new TLBEntry)
}

class TLBQuery extends Bundle {
  val hit = Output(Bool())
  val vaddr = Input(UInt(64.W))
  val paddr = Output(UInt(64.W))
}
//
class TLB extends Module{
  val io = IO(new Bundle() {
    val tlbUpdate = new TLBUpdate
    val tlbQuery = new TLBQuery
    val flush = Input(Bool())
  })

  val vpn0In = io.tlbQuery.vaddr(20,12)
  val vpn1In = io.tlbQuery.vaddr(29,21)
  val vpn2In = io.tlbQuery.vaddr(38,30)
  
  val cnt = RegInit(0.U)
  when(cnt < 8.U) {
    cnt := cnt + 1.U
    when(cnt === 7.U) {
      cnt := 0.U
    }
  }
  val tlbUpdate = WireInit(io.tlbUpdate.entry)

  val tlbRegs = Mem(8, new TLBEntry)
  when(io.tlbUpdate.enable) {
    tlbRegs(cnt) := tlbUpdate
    tlbRegs(cnt).valid := true.B
  }
  io.tlbQuery.hit := false.B
  io.paddr := 0.U
  for( i <- 0 to 8 ) {
    val tlbEntry = tlbRegs(i)
    val tlbEntryTag = tlbEntry.vpn
    val vpn0_hit = (vpn0In === tlbEntryTag(20,12))
    val vpn1_hit = (vpn1In === tlbEntryTag(29,21))
    val vpn2_hit = (vpn2In === tlbEntryTag(38,30))
    when(vpn2_hit && tlbEntry.valid) {
      when(tlbEntry.is1G) {
        // 1G Page
        io.tlbQuery.hit := true.B
        io.paddr := Cat(tlbEntry.pte.ppn2, io.reqVAddr(29,0)))
      }.elsewhen(vpn1_hit) {
        when(tlbEntry.is2M) {
          // 2M Page
          io.tlbQuery.hit := true.B
          io.paddr := Cat(tlbEntry.pte.ppn2, tlbEntry.pte.ppn1, io.reqVAddr(20,0))
        }.elsewhen(vpn0_hit) {
          // 4K Page
          io.tlbQuery.hit := true.B
          io.paddr := Cat(tlbEntry.pte.ppn2, tlbEntry.pte.ppn1, tlbEntry.pte.ppn0, io.reqVAddr(11,0))
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
  stage.emitVerilog(new tlb)
}
