package core

import mmu._
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

class IFUTopIO extends Bundle {
  // IFU
  val inst_out = Output(new InstBundle)
  val branchRedir = Input(new BranchRedir)
  val exceptionRedir = Input(new ExceptionRedir)
  val exceInfoOut = Output(new ExceptionInfo)
  val pause = Input(Bool())
  val intCtrl = Input(new INTCtrl)
  // MMU
  val flush = Input(Bool())
  val csr2mmu = Flipped(new CSRMMU)
  // routed signal
  val IFUMemReq = new NaiveBusM2S
}

class IFUTop extends Module {
    val io = IO(new IFUTopIO)
    val ifu = Module(new IFU)
    val immu = Module(new MMU(isDMMU = false))
    // val ifuIO = Flipped(new IFUIO)
    io.inst_out := ifu.io.inst_out
    ifu.io.branchRedir := io.branchRedir
    ifu.io.exceptionRedir := io.exceptionRedir
    ifu.io.pause := io.pause
    io.exceInfoOut := ifu.io.exceInfoOut
    ifu.io.ifu2mmu <> immu.io.mem2mmu
    immu.io.csr2mmu := io.csr2mmu
    immu.io.isStore := false.B
    immu.io.flush := io.flush
    
    when(immu.io.dmemreq.memRreq) {
        io.IFUMemReq <> immu.io.dmemreq
    }.otherwise {
        io.IFUMemReq <> ifu.io.ifu2dmem
    }
  immu.io.dmemreq.memRvalid := io.IFUMemReq.memRvalid
  immu.io.dmemreq.memWrDone := false.B
  immu.io.dmemreq.memRdata  := io.IFUMemReq.memRdata
  ifu.io.ifu2dmem.memWrDone := false.B
  ifu.io.ifu2dmem.memRvalid := io.IFUMemReq.memRvalid
  ifu.io.ifu2dmem.memRdata  := io.IFUMemReq.memRdata
  ifu.io.intCtrl := io.intCtrl
}

object IFUTop extends App {
    val stage = new ChiselStage
    stage.emitVerilog(new IFUTop)
}