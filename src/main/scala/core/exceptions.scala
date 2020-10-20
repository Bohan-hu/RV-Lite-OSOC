package core
import Chisel.switch
import chisel3._


class exceptionIO extends Bundle {
  val excep2if = Output(new ExceptionRedir)
}
class exceptioInfo extends Bundle {
  val pageFault = Bool()

}
class ExceptionCSR extends Bundle {
  val redirIF = Output(new ExceptionRedir)
  val exceptionInfo = Output(new ExceptionInfo)
  val trapBaseAddr = Input(UInt(64.W))    // Trap Base Addr from *tvec from CSR
  val epc = Input(UInt(64.W))
  val exceptionBase = Input(UInt(64.W))
  val eret = Input(Bool())
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
