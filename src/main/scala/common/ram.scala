package common

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
  })
  val ram = Module(new RAMHelper)
  ram.io.clk := io.clk
  io.data_valid := true.B
  val raddr = RegNext(io.raddr)
  ram.io.rIdx := Cat(io.raddr(63,3), 0.U(3.W))
  io.rdata := Mux(!raddr(2), ram.io.rdata(63,32), ram.io.rdata(31,0))
  ram.io.wIdx := 0.U
  ram.io.wdata := 0.U
  ram.io.wmask := 0.U
  ram.io.wen := 0.U
}
