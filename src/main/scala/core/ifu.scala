package core

import chisel3._
import chisel3.stage.ChiselStage

class InstBundle extends Bundle {
  val inst_pc = UInt(64.W)
  val inst = UInt(32.W)
  val instValid = Bool()
}

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
    npc := io.exceptionRedir.excePC
  }.elsewhen(io.branchRedir.redir) {
    npc := io.branchRedir.TargetPC
  }.otherwise {
    npc := pc + 4.U
  }
  io.stallReq := !io.rvalid

  io.inst_out.inst := io.rdata
  // TODO: Add exception handle

  io.inst_out.instValid := RegNext(Mux(io.pause, io.inst_out.instValid, Mux(io.branchRedir.redir, false.B, io.rvalid)))
  io.inst_out.inst_pc := RegNext(Mux(io.pause, io.inst_out.inst_pc, Mux(io.branchRedir.redir, 0.U, io.inst_pc)))
}

object IFU extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new IFU)
}
