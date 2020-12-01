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
  val memWrDone = Input(Bool())
}

class MEM2MMU extends Bundle {
  val reqVAddr      = Output(UInt(64.W))
  val reqReady      = Output(Bool())
  val respPAddr     = Input(UInt(64.W))
  val respValid     = Input(Bool())
  val respPageFault = Input(Bool())
}

class MEMIO extends Bundle {
  val instPC      = Input(UInt(64.W))
  val isMemOp     = Input(Bool())
  val MemOp       = Input(UInt(2.W))
  val fuOp        = Input(UInt(4.W))
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
  val mem2mmu = new MEM2MMU
  val toclint  = Flipped(new MEMCLINT)
}

object MMIO {
  val MMIORange = List(
    (0x40600000L, 0x1000L), // uart
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
  val accessVAddr = io.baseAddr + io.imm

  // TODO:
  val readClint = io.mem2dmem.memAddr >= 0x38000000L.U && io.mem2dmem.memAddr <= 0x00010000L.U + 0x38000000L.U
  io.exceInfoOut := io.exceInfoIn
  io.toclint.data := io.R2Val
  io.toclint.addr := io.mem2dmem.memAddr
  // TODO Ends
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
  val signExt = io.MemType === SZ_B || io.MemType === SZ_H || io.MemType === SZ_W
  val dataFromMem = WireInit(io.mem2dmem.memRdata)
  val memRdataRaw = MuxLookup(dataSize, dataFromMem, // Including Word Select
    Array( // Byte, Addressed by addr[2:0]
      1.U -> dataFromMem.asTypeOf(DataTypesUtils.Bytes)(accessVAddr(2, 0)),
      2.U -> dataFromMem.asTypeOf(DataTypesUtils.HalfWords)(accessVAddr(2, 1)),
      4.U -> dataFromMem.asTypeOf(DataTypesUtils.Words)(accessVAddr(2)),
      8.U -> dataFromMem
    )
  )
  val memRdataRawExt = MuxLookup(dataSize, dataFromMem, // Including Word Select
    Array( // Byte, Addressed by addr[2:0]
      1.U -> dataFromMem.asTypeOf(DataTypesUtils.Bytes)(accessVAddr(2, 0)),
      2.U -> dataFromMem.asTypeOf(DataTypesUtils.HalfWords)(accessVAddr(2, 1)),
      4.U -> dataFromMem.asTypeOf(DataTypesUtils.Words)(accessVAddr(2)),
      8.U -> dataFromMem
    ).map( kw => { kw._1 -> signExt64(kw._2) }
  )
  )
  // Misaligned addr
  val addrMisaligned =  (accessVAddr(0) =/= 0.U && (io.MemType === SZ_H || io.MemType === SZ_HU)) ||        // Half
                        (accessVAddr(1,0) =/= 0.U && (io.MemType === SZ_W || io.MemType === SZ_WU)) ||      // Word
                        (accessVAddr(2,0) =/= 0.U && (io.MemType === SZ_D))                                 // Double
  // LR/SC Handler

  val reservationSet = Reg(UInt(64.W))
  val reservationValid = RegInit(false.B)

  val sIDLE :: sWAIT_PADDR :: sWAIT_RD :: sWAIT_WR :: Nil = Enum(4)
  // Signals to make state transfer
  val state            = RegInit(sIDLE)
  val translatedPAddr  = Reg(UInt(64.W))
  val isLoad           = ( io.isMemOp & io.fuOp === LSU_LOAD & !io.exceInfoIn.valid )
  val isStore          = ( io.isMemOp & io.fuOp === LSU_STORE & !io.exceInfoIn.valid )
  val isLR             = ( io.isMemOp & io.fuOp === LSU_LR & !io.exceInfoIn.valid )
  val isSC             = ( io.isMemOp & io.fuOp === LSU_SC & !io.exceInfoIn.valid )
  val isAMO            = ( io.isMemOp & io.MemOp === MEM_AMO & !io.exceInfoIn.valid )
  val scWillSuccess    = ( reservationValid && reservationSet === accessVAddr )
  val scResult         = isSC & !scWillSuccess
  val scSuccessReg     = RegInit(1.U(64.W))
  // When the instruction does not cause exception, is valid, and will happen, send the request to MMU
  val canFireMemReq = ( isLoad | isStore | isLR | (isSC & scWillSuccess) | ( isAMO & ~isSC) ) & ~addrMisaligned
  io.mem2mmu.reqReady := false.B
  io.mem2mmu.reqVAddr := accessVAddr
  val rDataReg = Reg(UInt(64.W))
  val amoSrc1 = Mux(dataSize === 4.U, io.R2Val(31,0), io.R2Val)
  val amoSrc2 = Mux(dataSize === 4.U, Mux(accessVAddr(2),rDataReg(63,32) ,rDataReg(31,0)), rDataReg)
  val amoWData = MuxLookup(io.fuOp, amoSrc2, 
    Array(
      LSU_ASWAP -> amoSrc1,
      LSU_AADD -> (amoSrc1 + amoSrc2),
      LSU_AAND -> (amoSrc1 & amoSrc2),
      LSU_AOR ->  (amoSrc1 | amoSrc2),
      LSU_AXOR -> (amoSrc1 ^ amoSrc2),
      LSU_AMAX -> (Mux(amoSrc1.asSInt() > amoSrc2.asSInt(), amoSrc1, amoSrc2)),
      LSU_AMAXU -> (Mux(amoSrc1 > amoSrc2, amoSrc1, amoSrc2)),
      LSU_AMIN -> (Mux(amoSrc1.asSInt() < amoSrc2.asSInt(), amoSrc1, amoSrc2)),
      LSU_AMINU -> (Mux(amoSrc1 < amoSrc2, amoSrc1, amoSrc2))
    )
  )
  io.mem2dmem.memAddr := translatedPAddr
  io.mem2dmem.memWdata := DataTypesUtils.WDataGen(dataSize, accessVAddr, Mux(isAMO && !isSC, amoWData, io.R2Val))
  io.mem2dmem.memWmask := DataTypesUtils.Byte2BitMask(DataTypesUtils.ByteMaskGen(dataSize, accessVAddr))
  io.mem2dmem.memWen := false.B
  io.mem2dmem.memRreq := false.B
  io.memResult := Mux(signExt, memRdataRawExt, memRdataRaw)
  val isMMIO = Mux(state === sIDLE, MMIO.inMMIORange(accessVAddr) ,MMIO.inMMIORange(io.mem2dmem.memAddr))   // Patch
  when(isSC & scSuccessReg === 0.U) {
    io.memResult := 0.U
  }.elsewhen(isSC & scSuccessReg === 1.U) {
    io.memResult := 1.U
  }
  when(readClint) {
    io.memResult := io.toclint.rdata
  }
  io.pauseReq := false.B
  io.toclint.wen := io.mem2dmem.memAddr >= 0x38000000L.U && io.mem2dmem.memAddr <= 0x00010000L.U + 0x38000000L.U && io.isMemOp && io.MemOp === MEM_WRITE && state === sWAIT_WR // TODO: Notice: It's an ugly patch!!!!
  switch(state) {
    is(sIDLE) {
      scSuccessReg := 1.U
      when(addrMisaligned & io.isMemOp & !io.exceInfoIn.valid) {
        io.exceInfoOut.valid := true.B
        io.exceInfoOut.cause := Mux(isLoad, ExceptionNo.loadAddrMisaligned.U, ExceptionNo.storeAddrMisaligned.U)
        io.exceInfoOut.tval := accessVAddr
        io.exceInfoOut.epc := io.instPC
      }
      when(isSC && scWillSuccess) {
        reservationValid := false.B
        scSuccessReg := 0.U
      }
      when( canFireMemReq & !isMMIO ) {
        io.pauseReq := true.B
        io.mem2mmu.reqReady := true.B
        state := sWAIT_PADDR
      }.elsewhen( canFireMemReq & isMMIO ) {
        io.pauseReq := true.B
        translatedPAddr := accessVAddr
        when( isLoad | isLR | isAMO ) {
          when(isLR) {
            reservationSet := accessVAddr
            reservationValid := true.B
          }
          state := sWAIT_RD
        }.elsewhen( isStore | isSC ) {
          state := sWAIT_WR
        }
      }
    }
    is(sWAIT_PADDR) {
      io.pauseReq := true.B
      io.mem2mmu.reqReady := true.B
      when( io.mem2mmu.respValid & !io.mem2mmu.respPageFault ) {
        translatedPAddr := io.mem2mmu.respPAddr
        when( isLoad | isLR | isAMO ) {
          when(isLR) {
            reservationSet := accessVAddr
            reservationValid := true.B
          }
          state := sWAIT_RD
        }.elsewhen( isStore | isSC ) {
          when(isSC) {
            reservationValid := false.B
          }
          state := sWAIT_WR
        }
      }.elsewhen(io.mem2mmu.respValid & io.mem2mmu.respPageFault) {
        state := sIDLE
        io.pauseReq := false.B
        io.exceInfoOut.valid := true.B
        io.exceInfoOut.cause := Mux(isLoad, ExceptionNo.loadPageFault.U, ExceptionNo.storePageFault.U)
        io.exceInfoOut.tval := accessVAddr
        io.exceInfoOut.epc := io.instPC
      }
    }
    is(sWAIT_RD) {
      io.mem2dmem.memRreq := true.B
      io.pauseReq := true.B
      when(io.mem2dmem.memRvalid & !(isAMO & ~isLR)) {
        io.pauseReq := false.B
        state := sIDLE
      }.elsewhen(io.mem2dmem.memRvalid & (isAMO & ~isLR)) {
        state := sWAIT_WR
        rDataReg := dataFromMem
      }
    }
    is(sWAIT_WR) {
        io.mem2dmem.memWen := !isMMIO
        io.pauseReq := true.B
        when(io.mem2dmem.memWrDone) {
          io.pauseReq := false.B
          state := sIDLE
        }
    }
  }
  val isUART = 0x40600000L.U <= io.mem2dmem.memAddr & (0x40600000L+10L).U >= io.mem2dmem.memAddr
  when( isStore && isUART ) {
    printf("%c", io.R2Val(7,0))
  }

  // MMIO Flag
  BoringUtils.addSource(RegNext(RegNext(io.isMemOp & isMMIO)), "difftestIsMMIO")
  when(accessVAddr === 0x807FF000L.U & isStore) {
    // printf("Writing to &SATP: %x\n", io.R2Val)
  }
  // LSU 
    // IDLE -> ReqPADDR -> OP -> IDLE
  // If is SC and SC will fail, write back the failing code 
  // IDLE -> read_req -> transfer to WAIT_RD 
  // WAIT_RD -> rvalid -> transfer to IDLE  / isAMO -> transfer to WAIT_WR
  // WVALID -> valid -> 
}

object MEM extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new MEM)
}
