package core
import chisel3._
import chisel3.stage.ChiselStage
class IFUIO extends Bundle {
  val inst_pc = Output(UInt(64.W))
  val inst_req = Output(Bool())
  val rdata = Input(UInt(32.W))
  val rvalid = Input(Bool())
  val inst = Output(UInt(32.W))
  val instValid = Output(Bool())
//  val branchInfo = Flipped(new(BranchCtrlIO))
//  val excInfo = Flipped(new (exceptionRedir))
  val stallReq = Output(Bool())
}
class IFU extends Module {
  val io = IO(new IFUIO)
  val pc = RegInit(0x80000000L.U(64.W))
  io.inst_pc := pc
  io.inst_req := true.B
  val npc = Wire(UInt(64.W))
  when(io.rvalid){
    pc := npc
  }
  npc := pc + 4.U   // todo
  io.stallReq := !io.rvalid

  io.inst := io.rdata
  io.instValid := io.rvalid
}

object IFU extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new IFU)
}
