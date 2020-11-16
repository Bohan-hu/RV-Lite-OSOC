package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import chisel3.util._
import common.OpConstants._
import common.Instructions._
import common.{CSRRInstruction, RTypeInstruction}
class commit2Exe extends Bundle {
  val rdNum = UInt(5.W)
  val wen = Bool()
}

class WBIO extends Bundle {
  val instBundleIn = Input(new InstBundle)
  val exe2Commit = Input(new Exe2Commit)
  val instBundleOut = Output(new InstBundle)
  val regfileWrite = Output(new RegWrite)
  val commit2Exe = Output(new commit2Exe)
  val csrRw = Flipped(new commitCSR)
}

class WB extends Module {
  val io = IO(new WBIO)
  io.commit2Exe.rdNum := io.exe2Commit.RdNum
  io.commit2Exe.wen := io.exe2Commit.RFWen & io.instBundleIn.instValid
  io.csrRw.exceptionInfo := io.exe2Commit.exceInfo
  io.csrRw.instPC := io.instBundleIn.inst_pc
  BoringUtils.addSource(RegNext(io.instBundleIn.instValid), "difftestCommit")
  BoringUtils.addSource(RegNext(Mux(io.exe2Commit.exceInfo.valid & io.instBundleIn.instValid & io.exe2Commit.exceInfo.cause(63), io.exe2Commit.exceInfo.cause,0.U)),"difftestIntrNO")
  io.csrRw.csrWData := io.exe2Commit.arithResult
  io.csrRw.csrAddr := io.instBundleIn.inst.asTypeOf(new CSRRInstruction).csr
  io.csrRw.csrOp := io.exe2Commit.CSRCmd
  io.csrRw.instRd := io.regfileWrite.waddr
  io.csrRw.instValid := io.instBundleIn.instValid
  io.csrRw.instRs := io.instBundleIn.inst.asTypeOf(new CSRRInstruction).rs1
  io.csrRw.inst := io.instBundleIn.inst
  io.instBundleOut := io.instBundleIn
  io.regfileWrite.waddr := io.exe2Commit.RdNum
  io.regfileWrite.wen := io.exe2Commit.RFWen & io.instBundleIn.instValid & ~io.exe2Commit.exceInfo.valid
  io.regfileWrite.wdata := MuxLookup(io.exe2Commit.WBSel, 0.U,
    Array(
      WB_X -> io.exe2Commit.arithResult,
      WB_ALU -> io.exe2Commit.arithResult,
      WB_MEM -> io.exe2Commit.memResult,
      WB_PC4 -> (io.instBundleIn.inst_pc + 4.U),
      WB_CSR -> io.csrRw.csrRdata
    ))
}

object WB extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new WB)
}
