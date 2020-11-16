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
  // io.data_valid := RegNext(RegNext(RegNext(!io.reset & io.rreq)))
  val sIDLE :: sHOLD :: sVALID :: Nil = Enum(3)
  val state = RegInit(sIDLE)
  val addrReg = RegInit(0.U)
  val cnt = Reg(UInt(5.W))
  io.rdata := 0.U
  io.data_valid := false.B
  switch(state) {
    is(sIDLE) {
      when(io.rreq) {
        addrReg := io.raddr - 0x80000000L.U
        state := sHOLD
      }
    }
    is(sHOLD) {
      cnt := cnt + 1.U
      when(cnt === 5.U) {
        state := sVALID
        cnt := 0.U
      }
    }
    is(sVALID) {
      io.data_valid := true.B
      io.rdata := Mux(addrReg(2), ram.io.rdata(63, 32), ram.io.rdata(31, 0))
      state := sIDLE
    }
  }
  ram.io.rIdx := addrReg(63,3)
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
    val mem2dmem = Flipped(new NaiveBusM2S)
  })
  val ram = Module(new RAMHelper)
  ram.io.clk := io.clk
//  io.data_valid := RegNext(!io.reset)
  val ack = Reg(Bool())
  io.mem2dmem.memWrDone := true.B
  val isMMIO = MMIO.inMMIORange(io.mem2dmem.memAddr)
    // Fake UART
  when(ack){
    ack := false.B
  }.elsewhen (io.mem2dmem.memRreq && !ack) {
    ack := true.B
  }.otherwise {
    ack := false.B
  }
  val accessAddr = io.mem2dmem.memAddr - 0x80000000L.U
  ram.io.rIdx := accessAddr(26, 3)
  io.mem2dmem.memRdata := ram.io.rdata
  ram.io.wIdx := accessAddr(26, 3)
  ram.io.wdata := io.mem2dmem.memWdata
  ram.io.wmask := io.mem2dmem.memWmask
  ram.io.wen := io.mem2dmem.memWen
  io.mem2dmem.memRvalid := ack
  
}
