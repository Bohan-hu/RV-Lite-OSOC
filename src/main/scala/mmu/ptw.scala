package mmu
import core.MMIO
import chisel3._
import chisel3.util._
import _root_.core.NaiveBusM2S

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
  val enableSv39 = Input(Bool()) // Enable All tranlation
  val translation_ls_en = Input(Bool()) // Enable Load/Store translation
  // Request
  val reqVAddr = Input(UInt(64.W))
  val reqReady = Input(Bool())
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
  val memReq = new NaiveBusM2S
  // TODO: TLB Query
  val tlbQuery = Flipped(new TLBQuery)
  // TODO: TLB Update
   val tlbUpdate = Output(new TLBEntry)

  // TODO: PMP Access Exception
}

class PTW(isDPTW: Boolean) extends Module {
  val io = IO(new PTWIO)
  val sIDLE :: sWAIT_PTE_Entry :: sHANDLE_PTE_Entry :: sERROR :: sWAIT_AFTER_FLUSH :: Nil = Enum(5)
  val pteLevelReg        = Reg(UInt(2.W))
  val stateReg           = RegInit(sIDLE)
  val ptrReg             = Reg(UInt(64.W))
  val isGlobalMappingReg = Reg(Bool())
  val pteReg             = Reg(UInt(64.W))
  val pteConverted       = Wire(new PTE)
  pteConverted           := pteReg.asTypeOf(new PTE)
  pteConverted.A         := true.B
  io.respValid           := false.B
  io.memReq.memRreq      := false.B
  io.pageFault           := false.B
  io.busy                := stateReg =/= sIDLE
  io.respPaddr           := 0.U
  io.memReq.memWdata     := 0.U
  io.memReq.memWen       := 0.U
  io.memReq.memWmask     := 0.U
  io.memReq.memAddr      := ptrReg
  // TLB Update
  io.tlbUpdate.pte := pteConverted
  io.tlbUpdate.vpn := io.reqVAddr(38,12)
  io.tlbUpdate.is1G := false.B
  io.tlbUpdate.is2M := false.B
  io.tlbUpdate.is4K := false.B
  io.tlbUpdate.valid := false.B
  io.tlbQuery.vaddr := io.reqVAddr
  io.memReq.memSize      := "b011".U
  // TODO: Handle SUM
  // If TLB hit, stay in IDLE mode
  // Also need to consider whether the Sv39 translation is enabled
  switch(stateReg) {
    is(sIDLE) {
      pteReg := 0.U
      pteLevelReg := 1.U
      // Data request has higher priority
      if (isDPTW) {
        when(!io.translation_ls_en) {
          io.respValid := true.B
          io.respPaddr := io.reqVAddr
        }
        when( io.translation_ls_en && io.reqReady && !io.tlbQuery.hit) {
          stateReg := sWAIT_PTE_Entry
          ptrReg := Cat(io.satp_PPN, io.reqVAddr(38, 30), 0.U(3.W)) // Root Page Table PPN
        }
        when( io.translation_ls_en && io.reqReady && io.tlbQuery.hit) {
          // stateReg := sIDLE
          // io.respValid := true.B
          // io.respPaddr := io.tlbQuery.paddr
          io.respValid := false.B
          stateReg := sHANDLE_PTE_Entry
          pteReg := io.tlbQuery.pte.asUInt()
          pteLevelReg := io.tlbQuery.level
        }
      } else {
        when(!io.enableSv39) {
          io.respValid := true.B
          io.respPaddr := io.reqVAddr
        }
        when(io.enableSv39 && io.reqReady && !io.tlbQuery.hit) { // Instruction Request
          stateReg := sWAIT_PTE_Entry
          ptrReg := Cat(io.satp_PPN, io.reqVAddr(38, 30), 0.U(3.W)) // Root Page Table PPN
        }
        when( io.enableSv39 && io.reqReady && io.tlbQuery.hit) {
          // stateReg := sIDLE
          // io.respValid := true.B
          // io.respPaddr := io.tlbQuery.paddr
          io.respValid := false.B
          stateReg := sHANDLE_PTE_Entry
          pteReg := io.tlbQuery.pte.asUInt()
          pteLevelReg := io.tlbQuery.level
        }
      }
    }
    is(sWAIT_PTE_Entry) {
      io.memReq.memRreq := true.B
      when(io.flush) {
        stateReg := sWAIT_AFTER_FLUSH
      }
      when(io.memReq.memRvalid) {
        stateReg := sHANDLE_PTE_Entry
        pteReg := io.memReq.memRdata
      }
    }
    is(sHANDLE_PTE_Entry) {
      when(io.flush) {
        stateReg := sIDLE
        pteLevelReg := 1.U
      }
      when(pteConverted.G) {
        isGlobalMappingReg := true.B
      }
      /* If PTE.v = 0, or PTE.r = 0 and PTE.w = 1,
         Stop and raise a page-fault exception corresponding to the original access type
      */
      when(!pteConverted.V || (!pteConverted.R && pteConverted.W)) {
        stateReg := sERROR
      }.otherwise {
        when(pteConverted.R || pteConverted.X) { // If pte.r = 1 or pte.x = 1
          /*
          A leaf PTE has been found.
          Determine if the requested memory access is allowed by the pte.r, pte.w, pte.x, and pte.u bits,
          given the current privilege mode and the value of the SUM and MXR fields of the mstatus register.
          If not, stop and raise a page-fault exception corresponding to the original access type.
          */
          switch(pteLevelReg) {
            is(1.U) { io.respPaddr := Cat(pteConverted.ppn2, io.reqVAddr(29,0))}
            is(2.U) { io.respPaddr := Cat(pteConverted.ppn2, pteConverted.ppn1, io.reqVAddr(20,0))}
            is(3.U) { io.respPaddr := Cat(pteConverted.ppn2, pteConverted.ppn1, pteConverted.ppn0, io.reqVAddr(11,0))}
          }
          switch(pteLevelReg) {
            is(1.U) { io.tlbUpdate.is1G := true.B }
            is(2.U) { io.tlbUpdate.is2M := true.B }
            is(3.U) { io.tlbUpdate.is4K := true.B }
          }
          if (isDPTW) { // isDPTW, check the following conditions
            when(pteConverted.A && (pteConverted.R || (pteConverted.X && io.mxr))) {
            // when((pteConverted.R || (pteConverted.X && io.mxr))) {
              stateReg := sIDLE
              io.respValid := true.B
              io.tlbUpdate.valid := ~io.tlbQuery.hit
              pteLevelReg := 1.U
            }.otherwise {
              io.respValid := false.B
              stateReg := sERROR
            }
            when(io.reqIsStore && !pteConverted.W) { // Is store, but not writable
              io.respValid := false.B
              stateReg := sERROR
            }
            // TODO: Recover the condition
            when(!pteConverted.A ||
              (io.reqIsStore && !pteConverted.D)) { // pte.a = 0,
              // or if the memory access is a store and pte.d = 0
              io.respValid := false.B
              stateReg := sERROR
            }
          } else {  // is IPTW, check the following conditions
            /*
            Attempting to fetch an instruction from a page that does not have execute permissions
            raises a fetch page-fault exception
             */
            // TODO: Recover the condition
            when(!pteConverted.X || !pteConverted.A) { // Instr, not eXecutable
            // when(!pteConverted.X) { // Instr, not eXecutable
              io.respValid := false.B
              stateReg := sERROR
            }.otherwise {
              io.respValid := true.B
              io.tlbUpdate.valid := ~io.tlbQuery.hit
              stateReg := sIDLE
              pteLevelReg := 1.U
            }
          }
          // 6. If i > 0 and pa.ppn[i − 1 : 0] != 0, this is a misaligned superpage; stop and raise a page-fault
          // exception.
          when((pteLevelReg === 1.U && Cat(pteConverted.ppn1, pteConverted.ppn0) =/= 0.U) ||
            (pteLevelReg === 2.U && pteConverted.ppn0 =/= 0.U)) {
            io.respValid := false.B
            stateReg := sERROR
          }
        }.otherwise { // the PTE is a pointer to the next level of the page table
          stateReg := sWAIT_PTE_Entry
          when(pteLevelReg === 1.U) {
            pteLevelReg := 2.U
            ptrReg := Cat(pteConverted.getPPN, io.reqVAddr(29, 21), 0.U(3.W)) // VPN 1
          }
          when(pteLevelReg === 2.U) {
            pteLevelReg := 3.U
            ptrReg := Cat(pteConverted.getPPN, io.reqVAddr(20, 12), 0.U(3.W)) // VPN 2
          }
          when(pteLevelReg === 3.U) { // Should be a fault
            pteLevelReg := 3.U
            stateReg := sERROR
          }
        }
      }
    }
    is(sWAIT_AFTER_FLUSH) { // Recover from a flush
      when(io.memReq.memRvalid) {
        stateReg := sIDLE
        pteLevelReg := 1.U
      }
    }
    is(sERROR) {
      io.respValid := true.B
      io.pageFault := true.B
      stateReg := sIDLE
      pteLevelReg := 1.U
    }
  }
}
