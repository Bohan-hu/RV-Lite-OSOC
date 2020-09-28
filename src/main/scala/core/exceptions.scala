package core
import chisel3._

class ExceptionRedir extends Bundle {
  val excePC = Output(UInt(64.W))
  val redir = Bool()
}
class exceptionIO extends Bundle {
  val excep2if = Output(new ExceptionRedir)
}
class exceptions extends Module{
  val io = new(exceptionIO)
}
