package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils

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
  val exe2Mem = Input(new Exe2Mem)
  val mem2Wb = Output(new Mem2Wb)
  val instBundleOut = Output(new InstBundle)
}

object MMIO {
  val MMIORange = List(
    (0x40600000L, 0x10L), // uart
    (0x50000000L, 0x400000L), // vmem
    (0x40001000L, 0x8L), // vga ctrl
    (0x40000000L, 0x1000L), // flash
    (0x40002000L, 0x1000L), // dummy sdcard
    (0x42000000L, 0x1000L), // DiffTestCtrl
    (0x40004000L, 0x1000L), // meipGen
    (0x40003000L, 0x1000L) // dma
  )

  def inMMIORange(Addr: UInt) = {
    MMIORange map ({
      case (base, size) => Addr >= base.U && Addr <= (base + size).U
    }) reduce {
      _ | _
    }
  }
}

class MEM extends Module {
  val io = IO(new MEMIO)
  val memResult = 0.U(64.W) // TODO

  io.mem2Wb.aluResult := io.exe2Mem.aluResult
  io.mem2Wb.memResult := memResult
  io.mem2Wb.WBSel := io.exe2Mem.WBSel
  io.mem2Wb.RFWen := io.exe2Mem.RFWen
  io.mem2Wb.CSRCmd := io.exe2Mem.CSRCmd
  io.mem2Wb.isFence := io.exe2Mem.isFence
  io.mem2Wb.RdNum := io.exe2Mem.RdNum
  // passthrough
  io.instBundleOut := io.instBundleIn
  BoringUtils.addSource(RegNext(io.exe2Mem.isMemOp & MMIO.inMMIORange(io.exe2Mem.aluResult)), "difftestIsMMIO")
}

object MEM extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new MEM)
}
