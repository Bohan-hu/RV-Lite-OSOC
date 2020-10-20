package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import chisel3.util._
import common.OpConstants._
import common.Instructions._
import common.{CSRRInstruction, RTypeInstruction}
class WBIO extends Bundle {
  val instBundleIn = Input(new InstBundle)
  val mem2Wb = Input(new Mem2Wb)
  val instBundleOut = Output(new InstBundle)
  val regfileWrite = Output(new RegWrite)
  val csrRw = Flipped(new commitCSR)
  // todo: interact with csr
}

class WB extends Module {
  val io = IO(new WBIO)
  io.csrRw.exceptionInfo := io.mem2Wb.exceInfo
  BoringUtils.addSource(RegNext(io.instBundleIn.instValid), "difftestCommit")
  io.csrRw.csrWData := io.mem2Wb.aluResult
  io.csrRw.csrAddr := io.instBundleIn.inst.asTypeOf(new CSRRInstruction).csr
  io.csrRw.csrOp := io.mem2Wb.CSRCmd
  io.csrRw.instRd := io.regfileWrite.waddr
  io.csrRw.instValid := io.instBundleIn.instValid
  io.csrRw.instRs := io.instBundleIn.inst.asTypeOf(new CSRRInstruction).rs1

  io.instBundleOut := io.instBundleIn
  io.regfileWrite.waddr := io.mem2Wb.RdNum
  io.regfileWrite.wen := io.mem2Wb.RFWen & io.instBundleIn.instValid
  io.regfileWrite.wdata := MuxLookup(io.mem2Wb.WBSel, 0.U,
    Array(
      WB_X -> io.mem2Wb.aluResult,
      WB_ALU -> io.mem2Wb.aluResult,
      WB_MEM -> io.mem2Wb.memResult,
      WB_PC4 -> (io.instBundleIn.inst_pc + 4.U),
      WB_CSR -> io.csrRw.csrRdata
    ))
}

object WB extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new WB)
}
