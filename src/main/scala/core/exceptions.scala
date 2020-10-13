package core
import Chisel.switch
import chisel3._

class ExceptionRedir extends Bundle {
  val excePC = UInt(64.W)
  val redir = Bool()
}
class exceptionIO extends Bundle {
  val excep2if = Output(new ExceptionRedir)
}
class exceptioInfo extends Bundle {
  val pageFault = Bool()

}
//class exceptions extends Module{
//  val io = new(exceptionIO)
//  val mip = UInt(32.W)
//  val mie = UInt(32.W)
//  val priv = UInt(2.W)
//  val mstatus = UInt(64.W).asTypeOf(new mstatus)
//  val interrupts_pending = mip & mie    // The pending interrupts
//  val interrupts_enabled = Wire(UInt(64.W))
//  switch(priv) {
//    is(M) {
//      interrupts_enabled := Mux(mstatus.MIE, ~mideleg, 0)
//    }
//    is(S) {
//      interrupts_enabled := Mux(sstatus.SIE, ~mideleg, 0)
//    }
//  }
//}
