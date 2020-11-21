// build with
//  mill chiselModule.test.runMain sim.SimTop -td build --output-file Simtop.v 
package sim
import core._
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
class DiffTestIO extends Bundle {
  val r = Output(Vec(32, UInt(64.W)))
  val commit = Output(Bool())
  val isMultiCommit = Output(Bool())
  val thisPC = Output(UInt(64.W))
  val thisINST = Output(UInt(32.W))
  val isMMIO = Output(Bool())
  val isRVC = Output(Bool())
  val isRVC2 = Output(Bool())
  val intrNO = Output(UInt(64.W))

  val priviledgeMode = Output(UInt(2.W))
  val mstatus = Output(UInt(64.W))
  val sstatus = Output(UInt(64.W))
  val mepc = Output(UInt(64.W))
  val sepc = Output(UInt(64.W))
  val mcause = Output(UInt(64.W))
  val scause = Output(UInt(64.W))
}

class LogCtrlIO extends Bundle {
  val log_begin, log_end = Input(UInt(64.W))
  val log_level = Input(UInt(64.W)) // a cpp uint
}

class DiffTestCtrlIO extends Bundle {
  val enable = Output(Bool())
}

class SimTop extends Module {
  val io = IO(new Bundle() {
    val difftest = new DiffTestIO
    val logCtrl = new LogCtrlIO
    val difftestCtrl = new DiffTestCtrlIO
  })
  dontTouch(io.logCtrl)
  io.difftestCtrl.enable := true.B
  val top = Module(new Top)
  val difftest = WireInit(0.U.asTypeOf(new DiffTestIO))
  BoringUtils.addSink(difftest.commit, "difftestCommit")
  BoringUtils.addSink(difftest.isMultiCommit, "difftestMultiCommit")
  BoringUtils.addSink(difftest.thisPC, "difftestThisPC")
  BoringUtils.addSink(difftest.thisINST, "difftestThisINST")
  BoringUtils.addSink(difftest.isMMIO, "difftestIsMMIO")
  BoringUtils.addSink(difftest.isRVC, "difftestIsRVC")
  BoringUtils.addSink(difftest.isRVC2, "difftestIsRVC2")
  BoringUtils.addSink(difftest.intrNO, "difftestIntrNO")
  BoringUtils.addSink(difftest.r, "difftestRegs")
  BoringUtils.addSink(difftest.priviledgeMode, "difftestMode")
  BoringUtils.addSink(difftest.mstatus, "difftestMstatus")
  BoringUtils.addSink(difftest.sstatus, "difftestSstatus")
  BoringUtils.addSink(difftest.mepc, "difftestMepc")
  BoringUtils.addSink(difftest.sepc, "difftestSepc")
  BoringUtils.addSink(difftest.mcause, "difftestMcause")
  BoringUtils.addSink(difftest.scause, "difftestScause")

  io.difftest := difftest

}

object SimTop extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new SimTop)
}
