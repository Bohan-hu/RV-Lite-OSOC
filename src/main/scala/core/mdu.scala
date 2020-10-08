package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
class MulIO extends Bundle {
  val opA = Input(UInt(64.W))
  val opB = Input(UInt(64.W))
  val opValid = Input(Bool())
  val unsignedA = Input(Bool())
  val unsignedB = Input(Bool())
//  val isWordOp = Input(Bool())
//  val isHi = Input(Bool())
  val outRes = Output(UInt(128.W))
//  val dWordResult = Output(UInt(64.W))
//  val wordResult = Output(UInt(64.W))
  val MulDone = Output(Bool())
}



class Multiplier extends Module {
  val io = IO(new MulIO)
  val opA = WireInit(Cat(Fill(2,Mux(io.unsignedA, 0.U, io.opA(63))), io.opA))
  val opB = WireInit(Cat(Fill(2,Mux(io.unsignedB, 0.U, io.opB(63))), io.opB, 0.U(1.W)))
  val sIDLE :: sMUL :: sDONE :: Nil = Enum(3)
  val state = RegInit(sIDLE)
  val opAreg = RegInit(opA)
  val opBreg = RegInit(opB)
  val opA_ext = Cat(opAreg(65),opAreg)
//  val opB_ext = Cat(opBreg(65),opBreg)
  val mulDone = Wire(Bool())
  val cnt = Reg(UInt(6.W))
  mulDone := cnt === 32.U
  io.MulDone := false.B
  val resReg = Reg(UInt((66*2+1).W))
  when(state === sIDLE) {
    when(io.opValid) {
      opAreg := opA
      opBreg := opB
      state := sMUL
      resReg := 0.U
      cnt := 0.U
    }
  }.elsewhen(state === sMUL) {
    cnt := cnt + 1.U
    when(mulDone) {
      state := sDONE
    }
  }.elsewhen(state === sDONE) {
    state := sIDLE
    io.MulDone := true.B
  }

  val pp = Wire(UInt(67.W))
  val BoothTable = Array(
    "b000".U -> 0.U,
    "b001".U -> opA_ext,
    "b010".U -> opA_ext,
    "b011".U -> (opA_ext << 1),
    "b100".U -> ((~opA_ext+1.U) << 1),
    "b101".U -> (~opA_ext+1.U),
    "b110".U -> (~opA_ext+1.U),
    "b111".U -> 0.U
  )
  // Mul
  pp := 0.U
  when(state === sMUL) {
    pp := MuxLookup(opBreg(2,0), 0.U, BoothTable)
    resReg := Cat(pp, 0.U(66.W)) + Cat(Fill(2, resReg(132)), resReg(132,2))
    opBreg := opBreg >> 2
    cnt := cnt + 1.U
  }
  io.outRes := resReg(129,2)
}

object Multiplier extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Multiplier)
}


