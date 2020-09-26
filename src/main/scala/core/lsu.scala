package core

import chisel3._
import chisel3.util.Decoupled
import defines._

class MemReq extends Bundle {
  val addr = UInt(64.W)
  val mask = UInt(8.W)
  val bytes = UInt(3.W)
  val isStore = Bool()
}

class LSUIO extends Bundle {
  val srcA = Input(UInt(64.W))
  val srcB = Input(UInt(64.W))
  val ext_Imm = Input(UInt(64.W))
  val addrOut = Input(UInt(64.W))
  val memReq = Decoupled(new MemReq)
  val memResp = Decoupled(UInt(64.W))
}

class LSU extends Module {
  val io = new LSUIO()

}
