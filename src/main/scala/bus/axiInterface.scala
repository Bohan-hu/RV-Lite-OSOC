package bus
import chisel3._

class CPUInstReq extends Bundle {
  val req = Bool()
  val addr = UInt(64.W)
  val len = UInt(4.W)
}

class CPUInstResp extends Bundle {
  val inst = UInt(64.W)
  val last = Bool()
  val valid = Bool()
}

class CPUDataReq extends Bundle {
  val req = Bool()
  val isWrite = Bool()
  val addr = UInt(64.W)
  val len = UInt(4.W)
  val size = UInt(3.W)
  val wData = UInt(64.W)
  val mask = UInt(8.W)  // 8 Bytes
  val wLast = Bool()
}

class AXIMaster extends Bundle {

  val arid     = Output(UInt(4.W))
  val araddr   = Output(UInt(64.W))
  val arlen    = Output(UInt(4.W))
  val arsize   = Output(UInt(3.W))
  val arburst  = Output(UInt(2.W))
  val arlock   = Output(UInt(2.W))
  val arcache  = Output(UInt(2.W))
  val arprot   = Output(UInt(3.W))
  val arvalid  = Output(Bool())
  val arready  = Input(Bool())

  val rid      = Input(UInt(3.W))
  val rdata    = Input(UInt(64.W))
  val rresp    = Input(UInt(2.W))
  val rlast    = Input(Bool())
  val rvalid   = Input(Bool())
  val rready   = Output(Bool())

  val awid     = Output(UInt(4.W))
  val awaddr   = Output(UInt(64.W))
  val awlen    = Output(UInt(4.W))
  val awsize   = Output(UInt(3.W))
  val awburst  = Output(UInt(2.W))
  val awlock   = Output(UInt(2.W))
  val awcache  = Output(UInt(4.W))
  val awprot   = Output(UInt(3.W))
  val awvalid  = Output(Bool())
  val awready  = Input(Bool())

  val wid      = Output(UInt(4.W))
  val wdata    = Output(UInt(64.W))
  val wstrb    = Output(UInt(4.W))
  val wlast    = Output(Bool())
  val wvalid   = Output(Bool())
  val wready   = Input(Bool())

  val bid      = Input(UInt(4.W))
  val bresp    = Input(UInt(2.W))
  val bvalid   = Input(Bool())
  val bready   = Output(Bool())
}

class AXIBridge extends Bundle {

  val respRid = Output(UInt(4.W))
}

class axiInterface extends Module {
  val io = IO(new AXIBridge)
}
