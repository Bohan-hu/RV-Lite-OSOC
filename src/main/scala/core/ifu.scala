package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

class InstBundle extends Bundle {
  val inst_pc = UInt(64.W)
  val inst = UInt(32.W)
  val instValid = Bool()
}

// If IF is waiting for a instruction, and received a flush request, then go to recover state
// PC saves the new pc, then wait for rvalid
// We set a register for pending flush request, if we are busy, and is ready to transfer to a IDLE
// We will take a look at pending flush flag, if the flag is set, we will not propogate 'valid' signal to the following stages

// In AXI interface, we leave 2 ports, 1 for inst req, 1 for mem req
class IFUIO extends Bundle {
  val inst_out = Output(new InstBundle)
  val branchRedir = Input(new BranchRedir)
  val exceptionRedir = Input(new ExceptionRedir)
  val pause = Input(Bool())
  val ifu2mmu = new MEM2MMU
  val ifu2dmem = new MEM2dmem
  val exceInfoOut = Output(new ExceptionInfo)
}

/* TODOs:
1. Integrate 2 dmem into 1, and apply delay model on it(fixed delay with 5 cycs)
2. the dmem bridge provides 2 ports, one for inst, the other for data
3. I/D Cache combined as one, use unified ports
*/
class IFU extends Module {
  val io = IO(new IFUIO)
  val sIDLE :: sWAIT_PADDR :: sWAIT_INST :: sPENDING :: sERROR :: Nil = Enum(5)
  val state = RegInit(sIDLE)
  val pendingRedirect = RegInit(false.B)
  val pendingRedirectAddr = RegInit(0.U(64.W))
  val pc = RegInit(0x80000000L.U(64.W)-4.U)
  val thisInstValid = WireInit(false.B)
  val thisPC = WireInit(0.U)
  val thisInst = WireInit(0.U)
  thisInst := io.ifu2dmem.memRdata
  thisPC := pc
  val thisExce = Wire(new ExceptionInfo)
    
  val savedPC = Reg(UInt(64.W))
  val savedInst = Reg(UInt(32.W))
  val pendingExce = Reg(Bool())
  val npc = Wire(UInt(64.W))
  val paddrPCReg = RegInit(0.U(64.W))

  thisExce.cause := ExceptionNo.instrPageFault.U
  thisExce.tval := thisInst
  thisExce.epc := pc
  thisExce.valid := false.B
  io.ifu2dmem.memAddr := paddrPCReg
  io.ifu2dmem.memWen := false.B
  io.ifu2dmem.memWdata := 0.U
  io.ifu2dmem.memWmask := 0.U
  io.ifu2dmem.memRreq := false.B
  io.ifu2mmu.reqVAddr := pc
  io.ifu2mmu.reqReady := false.B

  switch(state) {
    is(sIDLE) {
      pc := Mux(pendingRedirect, pendingRedirectAddr, npc)
      pendingRedirect := false.B
      io.ifu2mmu.reqReady := true.B
      io.ifu2mmu.reqVAddr := Mux(pendingRedirect, pendingRedirectAddr, npc)
      state := sWAIT_PADDR
      pendingExce := false.B
    }
    is(sWAIT_PADDR) {
      io.ifu2mmu.reqReady := true.B
      io.ifu2mmu.reqVAddr := pc
      when( io.ifu2mmu.respValid & !io.ifu2mmu.respPageFault ) {
        paddrPCReg := io.ifu2mmu.respPAddr
        state := sWAIT_INST
      }.elsewhen( io.ifu2mmu.respValid && io.ifu2mmu.respPageFault ) {
        when(io.pause) {
          state := sPENDING
          pendingExce := true.B
          savedPC := pc
        }.otherwise {
          thisExce.valid := true.B
          io.inst_out.instValid := ~pendingRedirect
          state := sIDLE
        }
      }
      when(io.exceptionRedir.redir) {
        pendingRedirect := true.B
        pendingRedirectAddr := io.exceptionRedir.redirPC
      }.elsewhen(io.branchRedir.redir) {
        pendingRedirect := true.B
        pendingRedirectAddr := io.branchRedir.TargetPC
      }
    }
    is(sWAIT_INST) {
      io.ifu2dmem.memRreq := true.B
      when(io.ifu2dmem.memRvalid & ~io.pause) { // We don't need to care about redir at this moment
      // because the pipeline register will be cleared at next clock edge
        state := sIDLE
        thisInstValid := ~pendingRedirect
      }
      when(io.ifu2dmem.memRvalid & io.pause) { 
        savedInst := io.ifu2dmem.memRdata
        savedPC := pc
      }
      when(io.exceptionRedir.redir) {
        pendingRedirect := true.B
        pendingRedirectAddr := io.exceptionRedir.redirPC
      }.elsewhen(io.branchRedir.redir) {
        pendingRedirect := true.B
        pendingRedirectAddr := io.branchRedir.TargetPC
      }
    }
    is(sPENDING) {
      when(~io.pause) {
        state := sIDLE
        thisInstValid := ~pendingRedirect
        thisInst := io.ifu2dmem.memRdata
        thisPC := pc
        thisExce.valid := pendingExce
      }
    }
  }
  // Add 'pausepending' state, when we want to transfer to IDLE state, 
  // but the next pipeline stages are stalled, they are not allowing new instructions in
  // Just accept the instruction and go to the PENDING state, save the instruction
  // Everytime we want to transfer to IDLE state(update PC), 
  // Remember to check whether we have pending or incoming flush or redirect request 

  when(io.exceptionRedir.redir) {
    npc := io.exceptionRedir.redirPC
  }.elsewhen(io.branchRedir.redir) {
    npc := io.branchRedir.TargetPC
  }.otherwise {
    npc := pc + 4.U
  }

  io.inst_out.instValid := RegNext(Mux(io.pause, io.inst_out.instValid, Mux(io.branchRedir.redir || io.exceptionRedir.redir, false.B, thisInstValid)))
  io.inst_out.inst_pc := RegNext(Mux(io.pause, io.inst_out.inst_pc, Mux(io.branchRedir.redir || io.exceptionRedir.redir, 0.U, thisPC)))
  io.inst_out.inst := RegNext(Mux(io.pause, io.inst_out.inst, Mux(io.branchRedir.redir || io.exceptionRedir.redir, 0.U, thisInst)))
  io.exceInfoOut := RegNext(Mux(io.pause, io.exceInfoOut, Mux(io.branchRedir.redir || io.exceptionRedir.redir, 0.U.asTypeOf(new ExceptionInfo), thisExce)))
}

object IFU extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new IFU)
}
