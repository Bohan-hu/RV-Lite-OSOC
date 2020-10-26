package core

import chisel3._
import chisel3.stage.ChiselStage

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
  val inst_pc = Output(UInt(64.W))
  val inst_req = Output(Bool())
  val rdata = Input(UInt(32.W))
  val rvalid = Input(Bool())
  val inst_out = Output(new InstBundle)
  //  val branchInfo = Flipped(new(BranchCtrlIO))
  //  val excInfo = Flipped(new (exceptionRedir))
  val stallReq = Output(Bool())
  val branchRedir = Input(new BranchRedir)
  val exceptionRedir = Input(new ExceptionRedir)
  val pause = Input(Bool())
  val exceInfo = Output(new ExceptionInfo)
}

class IFU extends Module {
  val io = IO(new IFUIO)

  val pc = RegInit(0x80000000L.U(64.W))
  io.inst_pc := pc
  io.inst_req := true.B
  val npc = Wire(UInt(64.W))
  when(io.rvalid & !io.pause) {
    pc := npc
  }
  when(io.exceptionRedir.redir) {
    npc := io.exceptionRedir.redirPC
  }.elsewhen(io.branchRedir.redir) {
    npc := io.branchRedir.TargetPC
  }.otherwise {
    npc := pc + 4.U
  }
  io.stallReq := !io.rvalid

  io.inst_out.inst := io.rdata
  // TODO: Add exception handle

  val exceptioInfo2decode = Wire(new ExceptionInfo)
  exceptioInfo2decode.cause := 0.U
  exceptioInfo2decode.epc := io.inst_out.inst_pc
  exceptioInfo2decode.valid := false.B
  exceptioInfo2decode.tval := io.rdata
  io.inst_out.instValid := RegNext(Mux(io.pause, io.inst_out.instValid, Mux(io.branchRedir.redir || io.exceptionRedir.redir, false.B, io.rvalid)))
  io.inst_out.inst_pc := RegNext(Mux(io.pause, io.inst_out.inst_pc, Mux(io.branchRedir.redir || io.exceptionRedir.redir, 0.U, io.inst_pc)))
  io.exceInfo := exceptioInfo2decode
}

object IFU extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new IFU)
}
