package core
import chisel3._

class PipelineRegCtrl extends Bundle {
    val flush = Bool()
    val hold = Bool()
}

class PipelineCtrlReq extends Bundle {
    val flush = Bool()
    val hold = Bool()
}

class PipelineCtrlIO extends Bundle {
    val toIFID = Output(new PipelineRegCtrl)
    val toIDEX = Output(new PipelineRegCtrl)
    val toEXWB = Output(new PipelineRegCtrl)
    val fromEX = Input(new PipelineCtrlReq)
    val fromWB = Input(new PipelineCtrlReq)
}

// IFU's hold and flush be handled by redir from FU
// EXU's flush be handled by its own fsm

class PipelineCtrl extends Module {
    val io = IO(new PipelineCtrlIO)
    // Handle the flush request from WB, EX
    when(io.fromEX.flush) {
        io.toIFID.flush := true.B
        io.toIDEX.flush := true.B
    }
    when(io.fromWB.flush) {
        io.toIFID.flush := true.B
        io.toIDEX.flush := true.B
        // We need to handle the flush to FU: MDU, LSU, when flush comes, don't start the fsm
        io.toEXWB.flush := true.B
    }

    when(io.fromEX.hold) {
        io.toIFID.hold := true.B
        io.toIDEX.hold := true.B
    }
    when(io.fromWB.flush) {
        io.toIFID.flush := true.B
        io.toIDEX.flush := true.B
    }
}