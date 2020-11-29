package bus
import core._
import core.NaiveBusM2S
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import _root_.core.MMIO
import chisel3.util.experimental.BoringUtils
/*
class NaiveBusM2S extends Bundle {
  val memRreq   = Output(Bool())
  val memAddr   = Output(UInt(64.W))
  val memRdata  = Input(UInt(64.W))
  val memRvalid = Input(Bool())
  val memWdata  = Output(UInt(64.W))
  val memWmask  = Output(UInt(64.W))
  val memWen    = Output(Bool())
  val memWrDone = Input(Bool())
}
*/
class AXIMaster extends Bundle {

  val arid     = Output(UInt(4.W))
  val araddr   = Output(UInt(32.W))
  val arlen    = Output(UInt(8.W))
  val arsize   = Output(UInt(3.W))
  val arburst  = Output(UInt(2.W))
  val arlock   = Output(Bool())
  val arcache  = Output(UInt(4.W))
  val aruser   = Output(Bool())
  val arprot   = Output(UInt(3.W))
  val arvalid  = Output(Bool())
  val arqos    = Output(UInt(4.W))
  val arready  = Input(Bool())

  val rid      = Input(UInt(1.W))
  val rdata    = Input(UInt(64.W))
  val rresp    = Input(UInt(2.W))
  val rlast    = Input(Bool())
  val ruser    = Input(Bool())
  val rvalid   = Input(Bool())
  val rready   = Output(Bool())

  val awid     = Output(UInt(4.W))
  val awaddr   = Output(UInt(32.W))
  val awlen    = Output(UInt(8.W))
  val awsize   = Output(UInt(3.W))
  val awburst  = Output(UInt(2.W))
  val awlock   = Output(Bool())
  val awcache  = Output(UInt(4.W))
  val awprot   = Output(UInt(3.W))
  val awvalid  = Output(Bool())
  val awqos    = Output(UInt(4.W))
  val awuser   = Output(Bool())
  val awready  = Input(Bool())

  val wid      = Output(UInt(1.W))
  val wdata    = Output(UInt(64.W))
  val wstrb    = Output(UInt(8.W))
  val wlast    = Output(Bool())
  val wvalid   = Output(Bool())
  val wready   = Input(Bool())

  val bid      = Input(UInt(4.W))
  val bresp    = Input(UInt(2.W))
  val buser    = Input(Bool())
  val bvalid   = Input(Bool())
  val bready   = Output(Bool())
}
class AXILiteMaster extends Bundle {

  val araddr   = Output(UInt(32.W))
  val arprot   = Output(UInt(3.W))
  val arvalid  = Output(Bool())
  val arready  = Input(Bool())

  val rdata    = Input(UInt(64.W))
  val rresp    = Input(UInt(2.W))
  val rvalid   = Input(Bool())
  val rready   = Output(Bool())

  val awaddr   = Output(UInt(32.W))
  val awprot   = Output(UInt(3.W))
  val awvalid  = Output(Bool())
  val awready  = Input(Bool())

  val wdata    = Output(UInt(64.W))
  val wstrb    = Output(UInt(8.W))
  val wvalid   = Output(Bool())
  val wready   = Input(Bool())

  val bresp    = Input(UInt(2.W))
  val bvalid   = Input(Bool())
  val bready   = Output(Bool())
}
class BridgeIO extends Bundle {
  val ifuPort = Flipped(new NaiveBusM2S)
  val lsuPort = Flipped(new NaiveBusM2S)
  val axiMaster = new AXIMaster
  val axiLiteMaster = new AXILiteMaster
}

class AXIBridge extends Module {
  val io = IO(new BridgeIO)
  val sIDLE :: sSEND_W_ADDR :: sSEND_R_ADDR :: sRECEIVE_DATA :: sSEND_DATA  :: sWAIT_WRESP :: Nil = Enum(6)
  val addr =  Mux(io.lsuPort.memRreq | io.lsuPort.memWen, io.lsuPort.memAddr, io.ifuPort.memAddr)
  BoringUtils.addSource(io.ifuPort.memAddr, "instReqAddr")
  BoringUtils.addSource(io.ifuPort.memRreq, "instReq")
  BoringUtils.addSource(io.ifuPort.memRvalid, "instResp")
  BoringUtils.addSource(io.lsuPort.memAddr, "memReqAddr")
  BoringUtils.addSource(io.lsuPort.memRvalid,"memRresp")
  BoringUtils.addSource(io.lsuPort.memRreq,"memRreq")
  BoringUtils.addSource(io.lsuPort.memWrDone,"memWrDone")
  
  // 8 Bytes in one transfer(Two instructions)
  val instReqArSz = WireInit("b011".U)
  dontTouch(io.axiMaster.awuser)
  dontTouch(io.axiMaster.awqos)
  dontTouch(io.axiMaster.aruser)
  // AR
  io.axiMaster.arid := Mux(io.lsuPort.memRreq, 1.U, 0.U) // Data requst has higher priority than inst request, data rid = 1
  io.axiMaster.araddr := addr 
  io.axiMaster.arlock := 0.U
  io.axiMaster.arcache := 0.U
  io.axiMaster.arprot := 0.U
  io.axiMaster.arburst := "b01".U 
  io.axiMaster.arlen := 0.U // No burst request now  
  io.axiMaster.arsize := Mux(io.lsuPort.memRreq, io.lsuPort.memSize, instReqArSz) 
  io.axiMaster.arvalid := false.B
  io.axiLiteMaster.arvalid := false.B // Initial value
  io.axiLiteMaster.araddr := addr 
  io.axiLiteMaster.arprot := 0.U
  io.axiLiteMaster.arvalid := false.B // Initial value
  io.axiMaster.arqos := 0.U

  // R
  // io.axiMaster.rready := (*Depnedent on the state) 
  io.axiMaster.rready := false.B 
  io.axiLiteMaster.rready := false.B 
  // R
  io.axiMaster.rready := false.B 
  io.axiLiteMaster.rready := false.B 
  // AW
  // Bust Mode: Incr
  io.axiMaster.awburst := "b01".U
  io.axiMaster.awaddr := io.lsuPort.memAddr
  io.axiMaster.awcache := 0.U
  io.axiMaster.awid := 1.U
  io.axiMaster.awlock := 0.U
  io.axiMaster.awprot := 0.U
  io.axiMaster.awlen := 0.U      // No burst request now  
  io.axiMaster.awsize := io.lsuPort.memSize 
  io.axiMaster.awvalid := false.B 
  io.axiLiteMaster.awvalid := false.B 
  io.axiLiteMaster.awaddr := io.lsuPort.memAddr
  io.axiLiteMaster.awprot := 0.U
  // W
  io.axiMaster.wid := 1.U
  io.axiMaster.wdata    := io.lsuPort.memWdata
  io.axiMaster.wstrb    := io.lsuPort.memWmask
  io.axiMaster.wlast    := true.B
  io.axiMaster.wvalid   := false.B
  io.axiLiteMaster.wdata    := io.lsuPort.memWdata
  io.axiLiteMaster.wstrb    := io.lsuPort.memWmask
  io.axiLiteMaster.wvalid   := false.B
  // B
  io.axiMaster.bready := true.B
  io.axiLiteMaster.bready := true.B
  val isMMIO = MMIO.inMMIORange(addr)
  // Data transfer should depend on the Mem Op
  val state = RegInit(sIDLE)
  // Regs to remeber address and id
  val addrReg = Reg(UInt(64.W))
  val idReg = Reg(UInt(4.W))
  val isMMIOReg = Reg(Bool())
  // State Transfer Logic
  val rValid = Mux(isMMIOReg, io.axiLiteMaster.rvalid, io.axiMaster.rvalid && io.axiMaster.rlast)
  val rData = Mux(isMMIOReg, io.axiLiteMaster.rdata, io.axiMaster.rdata)
  val bValid = Mux(isMMIOReg, io.axiLiteMaster.bvalid, io.axiMaster.bvalid)
  io.ifuPort.memRvalid := rValid && idReg === 0.U
  io.ifuPort.memRdata := Mux(io.ifuPort.memAddr(2), rData(63, 32), rData(31, 0))
  io.ifuPort.memWrDone := false.B
  io.lsuPort.memRvalid := rValid && idReg === 1.U 
  io.lsuPort.memRdata := rData 
  io.lsuPort.memWrDone := bValid && idReg === 1.U
  switch(state) {
    is(sIDLE) {
      when(io.lsuPort.memWen) {
        state := sSEND_W_ADDR
        addrReg := io.lsuPort.memAddr
        idReg := io.axiMaster.awid
        isMMIOReg := isMMIO
      }.elsewhen(io.ifuPort.memRreq | io.lsuPort.memRreq) {
        state := sSEND_R_ADDR
        idReg := io.axiMaster.arid
        addrReg := addr
        isMMIOReg := isMMIO
      }
    }
    is(sSEND_R_ADDR) {
      when(isMMIOReg) {
        io.axiLiteMaster.arvalid := true.B
        when(io.axiLiteMaster.arready) {
          state := sRECEIVE_DATA
        }
      }.otherwise{
        io.axiMaster.arvalid := true.B
        when(io.axiMaster.arready) {
          state := sRECEIVE_DATA
        }
      }
    }
    is(sRECEIVE_DATA) {
      when(isMMIOReg) {
        io.axiLiteMaster.rready := true.B
        when(io.axiLiteMaster.rvalid) {
          state := sIDLE
        }
      }.otherwise {
        io.axiMaster.rready := true.B
        when(io.axiMaster.rvalid && io.axiMaster.rlast) {
          state := sIDLE
        }
      }
    }
    is(sSEND_W_ADDR) {
      when(isMMIOReg) {
        io.axiLiteMaster.awvalid := true.B
        when(io.axiLiteMaster.awready) {
          state := sSEND_DATA
        } 
      }.otherwise{
        io.axiMaster.awvalid := true.B
        when(io.axiMaster.awready) {
          state := sSEND_DATA
        }
      }
    }
    is(sSEND_DATA) {
      when(isMMIOReg) {
        io.axiLiteMaster.wvalid := true.B
        when(io.axiLiteMaster.wready) {
          state := sWAIT_WRESP
        }
      }.otherwise{
        io.axiMaster.wvalid := true.B
        io.axiMaster.wlast := true.B
        when(io.axiMaster.wready) {
          state := sWAIT_WRESP
        }
      }
    }
    is(sWAIT_WRESP) {
      when( (io.axiMaster.bvalid && !isMMIOReg) | (io.axiLiteMaster.bvalid && isMMIOReg) ) {
        state := sIDLE
      }
    }
  }
  io.axiMaster.awuser := 0.U
  io.axiMaster.awqos := 0.U
  io.axiMaster.aruser := 0.U
}
object AXIBridge extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new AXIBridge)
}
