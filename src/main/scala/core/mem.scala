package core

import chisel3._
import chisel3.stage.ChiselStage

class Mem2Wb extends Bundle {
  val aluResult = UInt(64.W)
  val memResult = UInt(64.W)
  val RdNum = UInt(5.W)
  val WBSel = UInt(2.W)
  val RFWen = Bool()
  val CSRCmd = UInt(3.W)
  val isFence = Bool()
}

class MEMIO extends Bundle {
  val instBundleIn = Input(new InstBundle)
  val exe2Mem  =Input(new Exe2Mem)
  val mem2Wb = Output(new Mem2Wb)
  val instBundleOut = Output(new InstBundle)
}

class MEM extends Module {
  val io = IO(new MEMIO)
  val memResult = 0.U(64.W)   // TODO
  io.mem2Wb.aluResult := io.exe2Mem.aluResult
  io.mem2Wb.memResult := memResult
  io.mem2Wb.WBSel := io.exe2Mem.WBSel
  io.mem2Wb.RFWen := io.exe2Mem.RFWen
  io.mem2Wb.CSRCmd := io.exe2Mem.CSRCmd
  io.mem2Wb.isFence := io.exe2Mem.isFence
  io.mem2Wb.RdNum := io.exe2Mem.RdNum
  // passthrough
  io.instBundleOut := io.instBundleIn
}

object MEM extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new MEM)
}
