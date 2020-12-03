// build with
//  mill chiselModule.test.runMain sim.SimTop -td build --output-file Simtop.v 
package sim
import core._
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import bus.AXIMaster
import bus.AXILiteMaster
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

    val meip = Input(Bool())
  })
  val Core = Module(new Top)
  val RAM = Module(new AXI_ram)
  val mmio = Module(new AXILiteMMIO)
  // top.io.axiLiteMaster <> io.mmio
  RAM.io.clock := clock.asBool()
  RAM.io.reset := reset
  Core.io.axiLiteMaster <> mmio.io
  // top.io.axiMaster <> ram.io
    RAM.io.awid := Core.io.axiMaster.awid
    RAM.io.awaddr := Core.io.axiMaster.awaddr - 0x80000000L.U
    RAM.io.awlen := Core.io.axiMaster.awlen
    RAM.io.awsize := Core.io.axiMaster.awsize
    RAM.io.awburst := Core.io.axiMaster.awburst
    RAM.io.awlock := Core.io.axiMaster.awlock
    RAM.io.awcache := Core.io.axiMaster.awcache
    RAM.io.awprot := Core.io.axiMaster.awprot
    RAM.io.awvalid := Core.io.axiMaster.awvalid
    Core.io.axiMaster.awready := RAM.io.awready
    RAM.io.wdata := Core.io.axiMaster.wdata
    RAM.io.wstrb := Core.io.axiMaster.wstrb
    RAM.io.wlast := Core.io.axiMaster.wlast
    RAM.io.wvalid := Core.io.axiMaster.wvalid
    Core.io.axiMaster.wready := RAM.io.wready
    Core.io.axiMaster.bid := RAM.io.bid
    Core.io.axiMaster.bresp := RAM.io.bresp
    Core.io.axiMaster.bvalid := RAM.io.bvalid
    RAM.io.bready := Core.io.axiMaster.bready
    RAM.io.arid := Core.io.axiMaster.arid
    RAM.io.araddr := Core.io.axiMaster.araddr  - 0x80000000L.U
    RAM.io.arlen := Core.io.axiMaster.arlen
    RAM.io.arsize := Core.io.axiMaster.arsize
    RAM.io.arburst := Core.io.axiMaster.arburst
    RAM.io.arlock := Core.io.axiMaster.arlock
    RAM.io.arcache := Core.io.axiMaster.arcache
    RAM.io.arprot := Core.io.axiMaster.arcache
    RAM.io.arvalid := Core.io.axiMaster.arvalid
    Core.io.axiMaster.arready := RAM.io.arready
    Core.io.axiMaster.rid := RAM.io.rid
    Core.io.axiMaster.rdata := RAM.io.rdata
    Core.io.axiMaster.rresp := RAM.io.rresp
    Core.io.axiMaster.rlast := RAM.io.rlast
    Core.io.axiMaster.rvalid := RAM.io.rvalid
    Core.io.axiMaster.ruser := 0.U
    Core.io.axiMaster.buser := 0.U

    RAM.io.rready := Core.io.axiMaster.rready
  // ram.io.araddr := top.io.axiMaster.araddr - 0x80000000L.U
  // ram.io.awaddr := top.io.axiMaster.awaddr - 0x80000000L.U
  Core.io.meip := io.meip
  val difftest = WireInit(0.U.asTypeOf(new DiffTestIO))
  dontTouch(io.logCtrl)
  io.difftestCtrl.enable := true.B
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
  val debug_signal = Wire(new debug_signal)
  debug_signal := 0.U.asTypeOf(new debug_signal)
  BoringUtils.addSink(debug_signal.instReqAddr, "instReqAddr")
  BoringUtils.addSink(debug_signal.instReq, "instReq")
  BoringUtils.addSink(debug_signal.instResp, "instResp")
  BoringUtils.addSink(debug_signal.memReqAddr, "memReqAddr")
  BoringUtils.addSink(debug_signal.memResp,"memRresp")
  BoringUtils.addSink(debug_signal.memRreq,"memRreq")
  BoringUtils.addSink(debug_signal.memWrDone,"memWrDone")
  BoringUtils.addSink(debug_signal.memWen,"menWen")

}

object SimTop extends App {
  val stage = new ChiselStage
//  stage.execute(args, Seq(
//    ChiselGeneratorAnnotation(() => new ysyx_hbh),
////    RunFirrtlTransformAnnotation(new AddModulePrefix()),
////    ModulePrefixAnnotation("hbh_")
//  ))
  stage.emitVerilog(new SimTop)
}
