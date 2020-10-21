package core
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import chisel3.util.MuxLookup

class MEMCLINT extends Bundle {
      val addr = Input(UInt(64.W))
      val data = Input(UInt(64.W))
      val rdata = Output(UInt(64.W))
      val wen = Input(Bool())
}
class CLINT extends Module {
  val io = IO(new Bundle{
      val memport = new MEMCLINT
      val tocsr = Output(new CLINTCSR)
  })
  val mtime = RegInit(0.U(64.W))
  val mtimecmp = RegInit(0.U(64.W))
  val msip = RegInit(0.U(64.W))
  val clk = 100
  val freq = RegInit(clk.U(16.W))
  val inc = RegInit(1.U(16.W))

  val cnt = RegInit(0.U(16.W))
  val nextCnt = cnt + 1.U
  cnt := Mux(nextCnt < freq, nextCnt, 0.U)
  val tick = (nextCnt === freq)
  when(tick) { mtime := mtime + inc }
  val offsets = List(
    0x0 -> msip,
    0x4000 -> mtimecmp,
    0x8000 -> freq,
    0x8008 -> inc,
    0xbff8 -> mtime
  )
  val addrs = List(
    0x38000000L.U -> msip,
    0x38004000L.U -> mtimecmp,
    0x38008000L.U -> freq,
    0x38008008L.U -> inc,
    0x3800bff8L.U -> mtime
  )
  io.memport.rdata := MuxLookup(io.memport.addr, 0.U, addrs)
  val offset = io.memport.addr(15,0)
  offsets.map( kv => { when(io.memport.wen && offset === kv._1.U) {kv._2 := io.memport.data}} )
  io.tocsr.mtip := RegNext((mtime >= mtimecmp))
  io.tocsr.msip := RegNext(msip =/= 0.U)
}

object CLINT extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new CLINT)
}
