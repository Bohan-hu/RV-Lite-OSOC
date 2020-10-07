package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import common.OpConstants._
import common.util._
import chisel3.util._


class Mem2Wb extends Bundle {
  val aluResult = UInt(64.W)
  val memResult = UInt(64.W)
  val RdNum = UInt(5.W)
  val WBSel = UInt(2.W)
  val RFWen = Bool()
  val CSRCmd = UInt(3.W)
  val isFence = Bool()
}

class MEM2dmem extends Bundle {
  val memRreq = Output(Bool())
  val memAddr = Output(UInt(64.W))
  val memRdata = Input(UInt(64.W))
  val memRvalid = Input(Bool())
  val memWdata = Output(UInt(64.W))
  val memWmask = Output(UInt(64.W))
  val memWen = Output(Bool())
}

class MEMIO extends Bundle {
  val instBundleIn = Input(new InstBundle)
  val exe2Mem = Input(new Exe2Mem)
  val mem2Wb = Output(new Mem2Wb)
  val instBundleOut = Output(new InstBundle)
  val mem2dmem = new MEM2dmem
  val pauseReq = Output(Bool())
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
    (0x40003000L, 0x1000L), // dma

    (0x38000000L, 0x00010000L), // CLINT
    (0x3c000000L, 0x04000000L)  // PLIC
  )

  def inMMIORange(Addr: UInt) = {
    MMIORange map ({
      case (base, size) => Addr >= base.U && Addr <= (base + size).U
    }) reduce {
      _ | _
    }
  }
}

object DataTypesUtils {
  val Words = Vec(2, UInt(32.W))
  val HalfWords = Vec(4, UInt(16.W))
  val Bytes = Vec(8, UInt(8.W))

  def ByteMaskGen(dataSize: UInt, Addr: UInt) = {
    MuxLookup(dataSize, 8.U,
      Array(
        1.U -> UIntToOH(Addr(2, 0)),
        2.U -> UIntToOH(Addr(2, 1)).asBools().map(Fill(2, _)).reduce(Cat(_, _)),
        4.U -> UIntToOH(Addr(2)).asBools().map(Fill(4, _)).reduce(Cat(_, _)),
        8.U -> Fill(8, 1.U)
      )
    )
  }

  def Byte2BitMask(ByteMask: UInt) = { // Accept byte mask, convert it to bit mask
    ByteMask.asBools() map (Fill(8, _)) reduce (Cat(_, _))
  }
}


class MEM extends Module {
  val io = IO(new MEMIO)
  val isMMIO = MMIO.inMMIORange(io.exe2Mem.aluResult)
  val memRdata = io.mem2dmem.memRdata
  val address = io.exe2Mem.aluResult - 0x80000000L.U
  val signExt = io.exe2Mem.MemType === SZ_B || io.exe2Mem.MemType === SZ_H || io.exe2Mem.MemType === SZ_W
  val memRead = io.exe2Mem.isMemOp & io.exe2Mem.MemOp === MEM_READ & !isMMIO
  io.mem2dmem.memRreq := memRead
  val memPending = !io.mem2dmem.memRvalid & memRead
  when(memRead) {
//    printf("memRAddr = 0x%x, memRdata = 0x%x\n", io.exe2Mem.aluResult, memRdata)
  }
  io.pauseReq := memPending
  val memWrite = io.exe2Mem.isMemOp & io.exe2Mem.MemOp === MEM_WRITE
  val dataSize = MuxLookup(io.exe2Mem.MemType, 8.U,
    Array(
      SZ_D -> 8.U,
      SZ_W -> 4.U,
      SZ_WU -> 4.U,
      SZ_H -> 2.U,
      SZ_HU -> 2.U,
      SZ_B -> 1.U,
      SZ_BU -> 1.U
    )
  )
  val memRdataRaw = MuxLookup(dataSize, memRdata, // Including Word Select
    Array( // Byte, Addressed by addr[2:0]
      1.U -> memRdata.asTypeOf(DataTypesUtils.Bytes)(address(2, 0)),
      2.U -> memRdata.asTypeOf(DataTypesUtils.HalfWords)(address(2, 1)),
      4.U -> memRdata.asTypeOf(DataTypesUtils.Words)(address(2)),
      8.U -> memRdata
    )
  )
  val memRdataRawExt = MuxLookup(dataSize, memRdata, // Including Word Select
    Array( // Byte, Addressed by addr[2:0]
      1.U -> memRdata.asTypeOf(DataTypesUtils.Bytes)(address(2, 0)),
      2.U -> memRdata.asTypeOf(DataTypesUtils.HalfWords)(address(2, 1)),
      4.U -> memRdata.asTypeOf(DataTypesUtils.Words)(address(2)),
      8.U -> memRdata
    ).map( kw => { kw._1 -> signExt64(kw._2) }
  )
  )
  io.mem2Wb.aluResult := io.exe2Mem.aluResult // Mem Address
  io.mem2dmem.memAddr := address
  io.mem2dmem.memWdata := io.exe2Mem.R2val
  io.mem2dmem.memWmask := DataTypesUtils.Byte2BitMask(DataTypesUtils.ByteMaskGen(dataSize, address))
  io.mem2dmem.memWen := io.instBundleIn.instValid & io.exe2Mem.isMemOp & io.exe2Mem.MemOp === MEM_WRITE & !isMMIO
  io.mem2Wb.memResult := Mux(signExt, memRdataRawExt, memRdataRaw)
  when(memPending) {
    io.instBundleOut := io.instBundleIn
    io.instBundleOut.instValid := false.B
  } otherwise {
    io.instBundleOut := io.instBundleIn
  }
  io.mem2Wb.WBSel := io.exe2Mem.WBSel
  io.mem2Wb.RFWen := io.exe2Mem.RFWen
  io.mem2Wb.CSRCmd := io.exe2Mem.CSRCmd
  io.mem2Wb.isFence := io.exe2Mem.isFence
  io.mem2Wb.RdNum := io.exe2Mem.RdNum
  // passthrough

  // MMIO Flag
  BoringUtils.addSource(RegNext(io.exe2Mem.isMemOp & isMMIO), "difftestIsMMIO")
}

object MEM extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new MEM)
}
