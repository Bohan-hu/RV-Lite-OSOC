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
  val RdNum     = UInt(5.W)
  val WBSel     = UInt(2.W)
  val RFWen     = Bool()
  val CSRCmd    = UInt(3.W)
  val isFence   = Bool()
  val exceInfo  = new ExceptionInfo
}

class MEM2dmem extends Bundle {
  val memRreq   = Output(Bool())
  val memAddr   = Output(UInt(64.W))
  val memRdata  = Input(UInt(64.W))
  val memRvalid = Input(Bool())
  val memWdata  = Output(UInt(64.W))
  val memWmask  = Output(UInt(64.W))
  val memWen    = Output(Bool())
}

class MEMIO extends Bundle {
  val isMemOp     = Input(Bool())
  val MemOp       = Input(UInt(2.W))
  val MemType     = Input(UInt(3.W))
  val baseAddr    = Input(UInt(64.W))
  val imm         = Input(UInt(64.W))
  val R2Val       = Input(UInt(64.W))
  val exceInfoIn  = Input(new ExceptionInfo)
  val exceInfoOut = Output(new ExceptionInfo)
  val memResult   = Output(UInt(64.W))
  val pauseReq    = Output(Bool())
  // Will be passed directly by exu to outside
  val mem2dmem = new MEM2dmem
  val toclint  = Flipped(new MEMCLINT)
}

object MMIO {
  val MMIORange = List(
    (0x40600000L, 0x10L), // uart
    (0x50000000L, 0x400000L), // vmem
    (0x40001000L, 0x8L), // vga ctrl
    (0x40000000L, 0x1000L), // flash
    (0x40002000L, 0x1000L), // dummy sdcard
    (0x42000000L, 0x1000L),  // DiffTestCtrl
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
        1.U -> Reverse(UIntToOH(Addr(2, 0))),
        2.U -> UIntToOH(Addr(2, 1)).asBools().map(Fill(2, _)).reduce(Cat(_, _)),
        4.U -> UIntToOH(Addr(2)).asBools().map(Fill(4, _)).reduce(Cat(_, _)),
        8.U -> Fill(8, 1.U)
      )
    )
  }

  def WDataGen(dataSize: UInt, Addr: UInt, WData: UInt) = {
    MuxLookup(dataSize, 8.U,
      Array(
        1.U -> {
          MuxLookup(Addr(2,0),0.U,
            Array(
              0.U -> Cat(Fill(56, 0.U(1.W)), WData(7,0)),
              1.U -> Cat(Fill(48, 0.U(1.W)), WData(7,0),Fill(8, 0.U(1.W))),
              2.U -> Cat(Fill(40, 0.U(1.W)), WData(7,0),Fill(16,0.U(1.W))),
              3.U -> Cat(Fill(32, 0.U(1.W)), WData(7,0),Fill(24,0.U(1.W))),
              4.U -> Cat(Fill(24, 0.U(1.W)), WData(7,0),Fill(32,0.U(1.W))),
              5.U -> Cat(Fill(16, 0.U(1.W)), WData(7,0),Fill(40,0.U(1.W))),
              6.U -> Cat(Fill(8, 0.U(1.W)),  WData(7,0),Fill(48,0.U(1.W))),
              7.U -> Cat(WData(7,0),Fill(56,0.U(1.W)))))
        },
        2.U -> {
          MuxLookup(Addr(2,1),0.U,
            Array(
              0.U -> Cat(Fill(48, 0.U(1.W)), WData(15,0)),
              1.U -> Cat(Fill(32, 0.U(1.W)), WData(15,0),Fill(16, 0.U(1.W))),
              2.U -> Cat(Fill(16, 0.U(1.W)), WData(15,0),Fill(32, 0.U(1.W))),
              3.U -> Cat(WData(15,0),Fill(48,0.U(1.W)))))
        },
        4.U -> {
          MuxLookup(Addr(2),0.U,
            Array(
              0.U -> Cat(Fill(32, 0.U(1.W)), WData(31,0)),
              1.U -> Cat(WData(31,0), Fill(32, 0.U(1.W)))
            ))
        },
        8.U -> WData
      )
    )
  }

  def Byte2BitMask(ByteMask: UInt) = { // Accept byte mask, convert it to bit mask
    ByteMask.asBools() map (Fill(8, _)) reduce (Cat(_, _))
  }
}


class MEM extends Module {
  val io = IO(new MEMIO)
  val memWrite = Wire(Bool())
  val accessVAddr = io.baseAddr + io.imm
  val accessPAddr = accessVAddr - 0x80000000L.U   // TODO: Handle the Translation
  val isMMIO = MMIO.inMMIORange(accessVAddr)
  // TODO:
  val readClint = accessVAddr >= 0x38000000L.U && accessVAddr <= 0x00010000L.U + 0x38000000L.U
  io.exceInfoOut := io.exceInfoIn
  io.toclint.wen := accessVAddr >= 0x38000000L.U && accessVAddr <= 0x00010000L.U + 0x38000000L.U && memWrite
  io.toclint.data := io.R2Val
  io.toclint.addr := accessVAddr
  // TODO Ends
  val memRdata = Mux(readClint, io.toclint.rdata, io.mem2dmem.memRdata)
  val signExt = io.MemType === SZ_B || io.MemType === SZ_H || io.MemType === SZ_W

  // LR/SC Handler
  val isLR = WireInit(false.B)
  val isSC = WireInit(false.B)
  val reservationSet = Reg(UInt(64.W))
  val reservationValid = RegInit(false.B)
  when(isLR) {  // Update the reservation set
    reservationValid := true.B
    reservationSet := accessVAddr
  }.elsewhen(isSC){
    reservationValid := false.B
  }

  // No prior Exception happens, and the op type is read, notice the signal is for dmem
  val memRead = io.isMemOp & io.MemOp === MEM_READ & !isMMIO & !io.exceInfoIn.valid
  
  io.mem2dmem.memRreq := memRead
  val memPending = !io.mem2dmem.memRvalid & memRead
  when(memRead) {
//    printf("memRAddr = 0x%x, memRdata = 0x%x\n", io.exe2Mem.aluResult, memRdata)
  }
  io.pauseReq := memPending
  memWrite := io.isMemOp & io.MemOp === MEM_WRITE & !io.exceInfoIn.valid
  val dataSize = MuxLookup(io.MemType, 8.U,
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
      1.U -> memRdata.asTypeOf(DataTypesUtils.Bytes)(accessVAddr(2, 0)),
      2.U -> memRdata.asTypeOf(DataTypesUtils.HalfWords)(accessVAddr(2, 1)),
      4.U -> memRdata.asTypeOf(DataTypesUtils.Words)(accessVAddr(2)),
      8.U -> memRdata
    )
  )
  val memRdataRawExt = MuxLookup(dataSize, memRdata, // Including Word Select
    Array( // Byte, Addressed by addr[2:0]
      1.U -> memRdata.asTypeOf(DataTypesUtils.Bytes)(accessVAddr(2, 0)),
      2.U -> memRdata.asTypeOf(DataTypesUtils.HalfWords)(accessVAddr(2, 1)),
      4.U -> memRdata.asTypeOf(DataTypesUtils.Words)(accessVAddr(2)),
      8.U -> memRdata
    ).map( kw => { kw._1 -> signExt64(kw._2) }
  )
  )

  io.mem2dmem.memAddr := accessPAddr
  io.mem2dmem.memWdata := DataTypesUtils.WDataGen(dataSize, accessVAddr, io.R2Val)
  io.mem2dmem.memWmask := DataTypesUtils.Byte2BitMask(DataTypesUtils.ByteMaskGen(dataSize, accessVAddr))
  io.mem2dmem.memWen := memWrite
  io.memResult := Mux(signExt, memRdataRawExt, memRdataRaw)
  io.pauseReq := memPending

  // Fake UART
  when(isMMIO & memWrite &  0x40600000L.U <= accessVAddr & (0x40600000L+10L).U >= accessVAddr) {
    printf("%c", io.R2Val(7,0))
  }
  // MMIO Flag
  BoringUtils.addSource(RegNext(io.isMemOp & isMMIO), "difftestIsMMIO")

  // LSU 
  // IDLE -> ReqPADDR -> OP -> IDLE
  val scWillSuccess = reservationValid && reservationSet === accessVAddr
  // If is SC and SC will fail, write back the failing code 
  val sIDLE :: sWAIT_RD :: sWAIT_WR :: Nil = Enum(3)
  // IDLE -> read_req -> transfer to WAIT_RD 
  // WAIT_RD -> rvalid -> transfer to IDLE  / isAMO -> transfer to WAIT_WR
  // WVALID -> valid -> 
}

object MEM extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new MEM)
}
