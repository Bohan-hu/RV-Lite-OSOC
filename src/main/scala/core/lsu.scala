package core

import chisel3._
import chisel3.util.Decoupled
import defines._

class MemReq extends Bundle with ConfigParams {
  val addr = UInt(XLEN.W)
  val mask = UInt(8.W)
  val bytes = UInt(3.W)
  val isStore = Bool()
}

class LSUIO extends Bundle with ConfigParams {
  val srcA = Input(UInt(XLEN.W))
  val srcB = Input(UInt(XLEN.W))
  val ext_Imm = Input(UInt(XLEN.W))
  val addrOut = Input(UInt(XLEN.W))
  val memReq = Decoupled(new MemReq)
  val memResp = Decoupled(UInt(XLEN.W))
}

class LSU extends Module {
  val io = new LSUIO()

}
