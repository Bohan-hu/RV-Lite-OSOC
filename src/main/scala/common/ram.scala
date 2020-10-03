package common
import core._
import chisel3._
import chisel3.util._
class RAMHelper extends BlackBox {
  //  module RAMHelper(
  //    input         clk,
  //    input  [`RAMWIDTH-1:0] rIdx,
  //  output [`RAMWIDTH-1:0] rdata,
  //  input  [`RAMWIDTH-1:0] wIdx,
  //  input  [`RAMWIDTH-1:0] wdata,
  //  input  [`RAMWIDTH-1:0] wmask,
  //  input         wen
  //  );
  val io = IO(new Bundle() {
    val clk = Input(Bool())
    val rIdx = Input(UInt(64.W))
    val rdata = Output(UInt(64.W))
    val wIdx = Input(UInt(64.W))
    val wdata = Input(UInt(64.W))
    val wmask = Input(UInt(64.W))
    val wen = Input(Bool())
  })
}

class SyncReadOnlyMem extends Module {
  val io = IO(new Bundle() {
    val clk = Input(Bool())
    val reset = Input(Bool())
    val rreq = Input(Bool())
    val raddr = Input(UInt(64.W))
    val rdata = Output(UInt(32.W))
    val data_valid = Output(Bool())
    val pause = Input(Bool())
  })
  val ram = Module(new RAMHelper)
  ram.io.clk := io.clk
  io.data_valid := RegNext(!io.reset)
  val raddr = RegInit(0.U)
  raddr := Mux(io.pause, raddr, io.raddr)
  ram.io.rIdx := Mux(io.pause, raddr(63,3), io.raddr(63, 3))
  io.rdata := Mux(raddr(2), ram.io.rdata(63, 32), ram.io.rdata(31, 0))
  ram.io.wIdx := 0.U
  ram.io.wdata := 0.U
  ram.io.wmask := 0.U
  ram.io.wen := 0.U
}

/*
  val memAddr = Output(UInt(64.W))
  val memRdata = Input(UInt(64.W))
  val memRvalid = Input(Bool())
  val memWdata = Output(UInt(64.W))
  val memWmask = Output(UInt(8.W))
  val memWen = Output(Bool())
 */

class SyncReadWriteMem extends Module {
  val io = IO(new Bundle() {
    val clk = Input(Bool())
    val reset = Input(Bool())
    val mem2dmem = Flipped(new MEM2dmem)
  })
  val ram = Module(new RAMHelper)
  ram.io.clk := io.clk
//  io.data_valid := RegNext(!io.reset)
  val ack = Reg(Bool())

  when(ack){
    ack := false.B
  }.elsewhen (io.mem2dmem.memRreq && !ack) {
    ack := true.B
  }.otherwise {
    ack := false.B
  }
  ram.io.rIdx := io.mem2dmem.memAddr(63, 3)
  io.mem2dmem.memRdata := ram.io.rdata
  ram.io.wIdx := io.mem2dmem.memAddr(63, 3)
  ram.io.wdata := io.mem2dmem.memWdata
  ram.io.wmask := io.mem2dmem.memWmask
  ram.io.wen := io.mem2dmem.memWen
  io.mem2dmem.memRvalid := ack
}
