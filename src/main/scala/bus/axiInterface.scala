package bus
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
class CPUInstReq extends Bundle {
  val req = Bool()
  val addr = UInt(64.W)
}

class CPUInstResp extends Bundle {
  val inst = UInt(32.W)
  val valid = Bool()
}

class CPUDataReq extends Bundle {
  val req = Bool()
  val isWrite = Bool()
  val addr = UInt(64.W)
  val size = UInt(3.W)
  val wData = UInt(64.W)
  val wStrb = UInt(8.W)  // 8 Bytes
  val wLast = Bool()
}

class CPUDataResp extends Bundle {
  val valid = Bool()
  val write_done = Bool()
  val rdata = UInt(64.W)
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

class BridgeIO extends Bundle {
  val instReq = Input(new CPUInstReq)
  val instResp = Output(new CPUInstResp)
  val dataReq = Input(new CPUDataReq)
  val dataResp = Output(new CPUDataResp)
  val axiMaster = new AXIMaster
}

class AXIBridge extends Module {
  val io = IO(new BridgeIO)
  val sIDLE :: sSEND_RADDR_IREQ :: sWAIT_RDATA_IREQ :: sSEND_RADDR_DREQ :: sWAIT_RDATA_DREQ  :: sSEND_WADDR_DREQ :: sSEND_WDATA_DREQ :: sWAIT_WRESP_DREQ :: Nil = Enum(8)
  io.axiMaster.arvalid := io.instReq.req | io.dataReq.req
  io.axiMaster.arid := Mux(io.dataReq.req, 1.U, 0.U) // Data requst has higher priority than inst request
  io.axiMaster.araddr := Mux(io.dataReq.req, io.dataReq.addr, io.instReq.addr)
  
  // Fixed value
  // Bust Mode: Incr
  io.axiMaster.arburst := "b01".U
  io.axiMaster.awburst := "b01".U
  io.axiMaster.arlock := 0.U
  io.axiMaster.arcache := 0.U
  io.axiMaster.arprot := 0.U
  io.axiMaster.awcache := 0.U
  io.axiMaster.awid := 0.U
  io.axiMaster.awlock := 0.U
  io.axiMaster.awprot := 0.U
  io.axiMaster.wid := 0.U

  // 8 Bytes in one transfer(Two instructions)
  val instReqArSz = WireInit("b011".U)
  // Data transfer should depend on the Mem Op
  val state = RegInit(sIDLE)
  // State Transfer Logic
  io.instResp.valid := io.axiMaster.rvalid && io.axiMaster.rlast && state === sWAIT_RDATA_IREQ
  io.instResp.inst := Mux(io.instReq.addr(2), io.axiMaster.rdata(63, 32), io.axiMaster.rdata(31, 0))
  io.dataResp.valid := io.axiMaster.rvalid && io.axiMaster.rlast && state === sWAIT_RDATA_DREQ
  io.dataResp.rdata := io.axiMaster.rdata
  io.dataResp.write_done := io.axiMaster.bvalid && state === sWAIT_WRESP_DREQ
  switch(state) {
    is(sIDLE) {
      when(io.dataReq.req && io.dataReq.isWrite) {
        state := sSEND_WADDR_DREQ
      }.elsewhen(io.dataReq.req && ~io.dataReq.isWrite) {
        state := sSEND_RADDR_DREQ
      }.elsewhen(io.instReq.req) {
        state := sSEND_RADDR_IREQ
      }
    }
    is(sSEND_RADDR_IREQ) {
      when(io.axiMaster.arready) {
        state := sWAIT_RDATA_IREQ
      }
    }
    is(sWAIT_RDATA_IREQ) {
      when(io.axiMaster.rvalid && io.axiMaster.rlast) {
        state := sIDLE
      }
    }
    is(sSEND_WADDR_DREQ) {
      when(io.axiMaster.awready) {
        state := sSEND_WDATA_DREQ
      }
    }
    is(sSEND_WDATA_DREQ) {
      when(io.axiMaster.wready) {
        state := sWAIT_WRESP_DREQ
      }
    }
    is(sWAIT_WRESP_DREQ) {
      when(io.axiMaster.bvalid) {
        state := sIDLE
      }
    }
    is(sSEND_RADDR_DREQ) {
      when(io.axiMaster.arready) {
        state := sWAIT_RDATA_DREQ
      }
    }
    is(sWAIT_RDATA_DREQ) {
      when(io.axiMaster.rvalid && io.axiMaster.rlast){
        state := sIDLE
      }
    }
  }
  // Assign outputs
  io.axiMaster.awaddr := io.dataReq.addr  // Only data has write request
  io.axiMaster.awsize := io.dataReq.size
  io.axiMaster.wdata := io.dataReq.wData
  io.axiMaster.wstrb := io.dataReq.wStrb
  io.axiMaster.arvalid := false.B
  io.axiMaster.awvalid := false.B
  io.axiMaster.arsize := "b011".U
  io.axiMaster.wvalid := false.B
  io.axiMaster.bready := true.B
  io.axiMaster.rready := false.B
  io.axiMaster.wlast := false.B
  io.axiMaster.awlen := 0.U     // Do not Burst
  io.axiMaster.arlen := 0.U     // Do not Burst

    switch(state) {
    is(sIDLE) {
      when(io.dataReq.req && io.dataReq.isWrite) {
        io.axiMaster.awaddr := io.dataReq.addr
        io.axiMaster.awsize := io.dataReq.size
        io.axiMaster.awvalid := true.B
      }.elsewhen(io.dataReq.req && ~io.dataReq.isWrite) {
        io.axiMaster.araddr := io.dataReq.addr
        io.axiMaster.arvalid := true.B
        io.axiMaster.arsize := io.dataReq.size
      }.elsewhen(io.instReq.req) {
        io.axiMaster.araddr := io.instReq.addr
        io.axiMaster.arvalid := true.B
        io.axiMaster.arsize := "b011".U
      }
    }
    is(sSEND_RADDR_IREQ) {
      io.axiMaster.araddr := io.instReq.addr
      io.axiMaster.arvalid := true.B
      io.axiMaster.arsize := "b011".U
    }
    is(sWAIT_RDATA_IREQ) {
      io.axiMaster.rready := true.B
    }
    is(sSEND_WADDR_DREQ) {
      io.axiMaster.awaddr := io.dataReq.addr
      io.axiMaster.awsize := io.dataReq.size
      io.axiMaster.awvalid := true.B
    }
    is(sSEND_WDATA_DREQ) {
      io.axiMaster.wvalid := true.B
      io.axiMaster.wlast := true.B
      io.axiMaster.wid := 0.U
    }
    is(sWAIT_WRESP_DREQ) {
      
    }
    is(sSEND_RADDR_DREQ) {
      io.axiMaster.araddr := io.dataReq.addr
      io.axiMaster.arvalid := true.B
    }
    is(sWAIT_RDATA_DREQ) {
      io.axiMaster.rready := true.B
    }
  }
}

object AXIBridge extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new AXIBridge)
}