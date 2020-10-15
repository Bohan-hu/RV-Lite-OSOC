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
  val isInstrPgFault = Output(Bool())
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
  val pteLevelReg = Reg(UInt(2.W))
  val stateReg = Reg(UInt())
  val ptrReg = Reg(UInt(64.W))
  val isITLBReg = Reg(Bool())
  val isGlobalMappingReg = Reg(Bool())
  val pteReg = Reg(UInt(64.W)).asTypeOf(new PTE)
  io.respValid := false.B
  io.memReq.rreq := false.B
  // If TLB hit, stay in IDLE mode
  // Also need to consider whether the Sv39 translation is enabled
  switch(stateReg) {
    is(sIDLE) {
      pteLevelReg := 1.U
      // Data request has higher priority
      when(io.translation_global_en && io.translation_ls_en && io.reqReady && !io.reqIsInstr && !io.dTlbQuery.hit) {
        stateReg := sWAIT_PTE_Entry
        isITLBReg := false.B
        ptrReg := Cat(io.satp_PPN, io.reqVAddr(63, 30), 0.U(3.W)) // Root Page Table PPN
      }.elsewhen(io.translation_global_en && io.reqReady && io.reqIsInstr && !io.iTlbQuery.hit) { // Instruction Request
        stateReg := sWAIT_PTE_Entry
        isITLBReg := true.B
        ptrReg := Cat(io.satp_PPN, io.reqVAddr(63, 30), 0.U(3.W)) // Root Page Table PPN
      }
    }
    is(sWAIT_PTE_Entry) {
      io.memReq.rreq := true.B
      when(io.memReq.rvalid) {
        stateReg := sHANDLE_PTE_Entry
        pteReg := io.memReq.rdata.asTypeOf(new PTE)
      }
    }
    is(sHANDLE_PTE_Entry) {
      when(pteReg.G) {
        isGlobalMappingReg := true.B
      }
      /* If PTE.v = 0, or PTE.r = 0 and PTE.w = 1,
         Stop and raise a page-fault exception corresponding to the original access type
      */
      when(!pteReg.V || (!pteReg.R && pteReg.W)) {
        stateReg := sERROR
      }.otherwise {
        when(pteReg.R || pteReg.X) { // If pte.r = 1 or pte.x = 1
          /*
          A leaf PTE has been found.
          Determine if the requested memory access is allowed by the pte.r, pte.w, pte.x, and pte.u bits,
          given the current privilege mode and the value of the SUM and MXR fields of the mstatus register.
          If not, stop and raise a page-fault exception corresponding to the original access type.
          */
          when(io.reqIsInstr) { // Instruction Translation
            /*
            Attempting to fetch an instruction from a page that does not have execute permissions
            raises a fetch page-fault exception
             */
            when(!pteReg.X || !pteReg.A) { // Instr, not eXecutable
              stateReg := sERROR
            }.otherwise {
              io.respValid := true.B
              stateReg := sIDLE
              pteLevelReg := 1.U
            }
          }.otherwise { // Data
            when(pteReg.A && (pteReg.R || (pteReg.X && io.mxr))) {
              stateReg := sIDLE
              io.respValid := true.B
              pteLevelReg := 1.U
            }.otherwise {
              stateReg := sERROR
            }
            when(io.reqIsStore && !pteReg.W) { // Is store, but not writable
              stateReg := sERROR
            }
            when(!pteReg.A ||
              (io.reqIsStore && !pteReg.D)) { // pte.a = 0,
              // or if the memory access is a store and pte.d = 0
              stateReg := sERROR
            }
          }
          // 6. If i > 0 and pa.ppn[i − 1 : 0] != 0, this is a misaligned superpage; stop and raise a page-fault
          // exception.
          when((pteLevelReg === 1.U && Cat(pteReg.ppn2, pteReg.ppn1) =/= 0.U) ||
            (pteLevelReg === 2.U && pteReg.ppn1 =/= 0.U)) {
            stateReg := sERROR
            io.respValid := false.B
          }
        }.otherwise {     // the PTE is a pointer to the next level of the page table
          stateReg := sWAIT_PTE_Entry
          when(pteLevelReg === 1.U) {
            pteLevelReg := 2.U
            ptrReg := Cat(pteReg.getPPN, io.reqVAddr(29, 21), 0.U(3.W))     // VPN 1
          }
          when(pteLevelReg === 2.U) {
            pteLevelReg := 3.U
            ptrReg := Cat(pteReg.getPPN, io.reqVAddr(20, 12), 0.U(3.W))     // VPN 2
          }
          when(pteLevelReg === 3.U) { // Should be a fault
            pteLevelReg := 3.U
            stateReg := sERROR
          }
        }
      }
    }
    is(sERROR) {
      io.pageFault := true.B
      io.isInstrPgFault := isITLBReg
      stateReg := sIDLE
      pteLevelReg := 1.U
    }
  }
}
