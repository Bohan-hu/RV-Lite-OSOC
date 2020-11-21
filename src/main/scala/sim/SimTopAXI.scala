// build with
//  mill chiselModule.test.runMain sim.SimTop -td build --output-file Simtop.v 
package sim
import core._
import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util.experimental.BoringUtils
import bus.AXIMaster
import bus.AXILiteMaster
import firrtl.stage.RunFirrtlTransformAnnotation

class ysyx_hbh extends Module {
  val io = IO(new Bundle() {
    // val difftest = new DiffTestIO
    // val logCtrl = new LogCtrlIO
    // val difftestCtrl = new DiffTestCtrlIO
    val mem = new AXIMaster
    val mmio = new AXILiteMaster 
    val interrupt = Input(Bool())
  })
  val top = Module(new Top)
  top.io.axiLiteMaster <> io.mmio
  top.io.axiMaster <> io.mem
  top.io.meip := io.interrupt
  val difftest = WireInit(0.U.asTypeOf(new DiffTestIO))
  // dontTouch(io.logCtrl)
  // io.difftestCtrl.enable := true.B
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

}

object ysyx_hbh extends App {
  val stage = new ChiselStage
//  stage.execute(args, Seq(
//    ChiselGeneratorAnnotation(() => new ysyx_hbh),
////    RunFirrtlTransformAnnotation(new AddModulePrefix()),
////    ModulePrefixAnnotation("hbh_")
//  ))
  stage.emitVerilog(new ysyx_hbh)
}