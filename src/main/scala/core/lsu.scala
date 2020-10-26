package core

import chisel3._
import chisel3.util.Decoupled
import defines._
import mmu.DMEMReq
import mmu._
import chisel3.stage.ChiselStage
class MemReq extends Bundle {
  val addr = UInt(64.W)
  val mask = UInt(8.W)
  val bytes = UInt(3.W)
  val isStore = Bool()
}

// A wrapper module 
class LSUIO extends Bundle {
  val memIO = new MEMIO
  val lsu2Dmem = new MEM2dmem
}

class LSU extends Module {
  val io = IO(new LSUIO())
  val mem = Module(new MEM)
  val dmmu = Module(new MMU(isDMMU = true))
  
  val memio = mem.io.mem2dmem
  val mmuio = dmmu.io.dmemreq
  io.lsu2Dmem.memRreq       := memio.memRreq | mmuio.memRreq
  io.lsu2Dmem.memWdata      := memio.memWdata
  io.lsu2Dmem.memWen        := memio.memWen
  io.lsu2Dmem.memWmask      := memio.memWmask
  memio.memWrDone           := io.lsu2Dmem.memWrDone
  io.lsu2Dmem.memAddr       := Mux(mmuio.memRreq, mmuio.memAddr, memio.memAddr)
  memio.memRvalid           := io.lsu2Dmem.memRvalid
  mmuio.memRvalid           := io.lsu2Dmem.memRvalid
  mmuio.memWrDone           := false.B
}

object LSU extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new LSU)
}
