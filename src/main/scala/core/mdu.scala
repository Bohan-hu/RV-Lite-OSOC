package core

import chisel3._

class MulIO extends Bundle {
  val opA = Input(UInt(64.W))
  val opB = Input(UInt(64.W))
  val unsigned = Input(Bool())
  val isWordOp = Input(Bool())
  val isHi = Input(Bool())
  val dWordResult = Output(UInt(64.W))
  val wordResult = Output(UInt(64.W))
}



class Multiplier extends Module {
  val io = IO(new MulIO)
  val mulRes = io.opA * io.opB
}


