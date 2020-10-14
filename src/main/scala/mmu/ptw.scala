package mmu

import chisel3._
import chisel3.util._

class PTE extends Bundle {
  val reversed = UInt(10.W)
  val ppn2 = UInt(26.W)
  val ppn1 = UInt(9.W)
  val ppn0 = UInt(9.W)
  val rsw = UInt(2.W)
  val D = Bool()
  val A = Bool()
  val G = Bool()
  val U = Bool()
  val X = Bool()
  val W = Bool()
  val R = Bool()
  val V = Bool()

  def getPPN = Cat(ppn2, ppn1, ppn0)
}

class DMEMReq extends Bundle {
  val rreq = Output(Bool())
  val rvalid = Input(Bool())
  val rdata = Input(UInt(64.W))
}

class PTWIO extends Bundle {
  // ptw active ???
  val flush = Input(Bool())
  val busy = Output(Bool()) // PTW Busy
  val translation_global_en = Input(Bool()) // Enable All tranlation
  val translation_ls_en = Input(Bool()) // Enable Load/Store translation
  // Request
  val reqVAddr = Input(UInt(64.W))
  val reqReady = Input(Bool())
  val reqIsInstr = Input(Bool())
  val reqIsStore = Input(Bool())
  // Response
  val respPaddr = Output(UInt(64.W))
  val respValid = Output(Bool())
  val pageFault = Output(Bool())

  // From CSR
  val satp_ASID = Input(UInt(16.W))
  val satp_PPN = Input(UInt(44.W))
  val mxr = Input(Bool())
  // DMem request
  val memReq = new DMEMReq
  // TLB Query
  val iTlbQuery = new TLBQuery
  val dTlbQuery = new TLBQuery
  // TLB Update
  val tlbUpdate = Flipped(new TLBUpdate)
  // Response

  // Exceptions

  // TODO: PMP Access Exception
}

class PTW extends Module {
  val io = IO(new PTWIO)
  val sIDLE :: sWAIT_PTE_Entry :: sHANDLE_PTE_Entry :: sERROR :: sWAIT_AFTER_FLUSH :: Nil = Enum(4)
  val pteLevel = Reg(UInt(2.W))
  val state = Reg(UInt())
  val ptr = Reg(UInt(64.W))
  val isITLB = Reg(Bool())
  val isGlobalMapping = Reg(Bool())
  val pte = Reg(UInt(64.W)).asTypeOf(new PTE)
  io.respValid := false.B
  io.memReq.rreq := false.B
  switch(state) {
    is(sIDLE) {
      pteLevel := 1.U
      when(io.translation_global_en && io.reqReady && io.reqIsInstr && !io.iTlbQuery.hit) { // Instruction Request
        state := sWAIT_PTE_Entry
        isITLB := true.B
        ptr := Cat(io.satp_PPN, io.reqVAddr(63, 30), 0.U(3.W)) // Root Page Table PPN
      }.elsewhen(io.translation_global_en && io.translation_ls_en && io.reqReady && !io.reqIsInstr && !io.dTlbQuery.hit) {
        state := sWAIT_PTE_Entry
        isITLB := false.B
        ptr := Cat(io.satp_PPN, io.reqVAddr(63, 30), 0.U(3.W)) // Root Page Table PPN
      }
    }
    is(sWAIT_PTE_Entry) {
      io.memReq.rreq := true.B
      when(io.memReq.rvalid) {
        state := sHANDLE_PTE_Entry
        pte := io.memReq.rdata.asTypeOf(new PTE)
      }
    }
    is(sHANDLE_PTE_Entry) {
      when(pte.G) {
        isGlobalMapping := true.B
      }
      when(!pte.V || (!pte.R && pte.W)) { // Page Fault
        state := sERROR
      }.otherwise {
        when(pte.R || pte.X) { // The Leaf PTE
          /* TODO:
          A leaf PTE has been found.
          Determine if the requested memory access is allowed by the pte.r, pte.w, pte.x, and pte.u bits,
          given the current privilege mode and the value of the SUM and MXR fields of the mstatus register.
          If not, stop and raise a page-fault exception corresponding to the original access type.
          */
          when(io.reqIsInstr) { // Instruction Translation
            when(!pte.X || !pte.A) { // Instr, not eXecutable
              state := sERROR
            }.otherwise {
              io.respValid := true.B
            }
          }.otherwise { // Data
            when(pte.A && (pte.R || (pte.X && io.mxr))) {
              io.respValid := true.B
            }.otherwise {
              state := sERROR
            }
            when(io.reqIsStore && !pte.W) { // Is store, but not writable
              state := sERROR
            }
            when(!pte.A ||
              (io.reqIsStore && !pte.D)) { // pte.a = 0,
              // or if the memory access is a store and pte.d = 0
              state := sERROR
            }
          }
          // 6. If i > 0 and pa.ppn[i − 1 : 0] != 0, this is a misaligned superpage; stop and raise a page-fault
          // exception.
          when((pteLevel === 1.U && Cat(pte.ppn2, pte.ppn1) =/= 0.U) ||
            (pteLevel === 2.U && pte.ppn1 =/= 0.U)) {
            state := sERROR
            io.respValid := false.B
          }
        }.otherwise {
          state := sWAIT_PTE_Entry
          when(pteLevel === 1.U) {
            pteLevel := 2.U
            ptr := Cat(pte.getPPN, io.reqVAddr(29, 21), 0.U(3.W))
          }
          when(pteLevel === 2.U) {
            pteLevel := 3.U
            ptr := Cat(pte.getPPN, io.reqVAddr(20, 12), 0.U(3.W))
          }
          when(pteLevel === 3.U) { // Should be a fault
            pteLevel := 3.U
            state := sERROR
          }
        }
      }
    }
  }
}
