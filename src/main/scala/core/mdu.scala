package core

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import common.Instructions._
import common.OpConstants._
import common.util.signExt64
class MulIO extends Bundle {
  val opA = Input(UInt(64.W))
  val opB = Input(UInt(64.W))
  val mduOp = Input(UInt(4.W))
  val opValid = Input(Bool())
  val mulBusy = Output(Bool())
  val wbResult = Output(UInt(64.W))
}



class Multiplier extends Module {
  val io = IO(new MulIO)
  val unsignedA = io.mduOp === MDU_MULHU
  val unsignedB = io.mduOp === MDU_MULHU || io.mduOp === MDU_MULHSU
  val upper = io.mduOp === MDU_MULH || io.mduOp === MDU_MULHU || io.mduOp === MDU_MULHSU
  val isWordOp = io.mduOp === MDU_MULW
  val opA_raw = Mux(isWordOp, Cat(Fill(32,io.opA(31)),io.opA(31,0)), io.opA)
  val opB_raw = Mux(isWordOp, Cat(Fill(32,io.opB(31)),io.opB(31,0)), io.opB)
  val opA = WireInit(Cat(Fill(2,Mux(unsignedA, 0.U, opA_raw(63))), opA_raw))
  val opB = WireInit(Cat(Fill(2,Mux(unsignedB, 0.U, opB_raw(63))), opB_raw, 0.U(1.W)))
  val sIDLE :: sMUL :: sDONE :: Nil = Enum(3)
  val state = RegInit(sIDLE)
  val opAreg = RegInit(opA)
  val opBreg = RegInit(opB)
  val opA_ext = Cat(opAreg(65),opAreg)
  val mulDone = Wire(Bool())
  val cnt = Reg(UInt(6.W))
  mulDone := cnt === 32.U
  val resReg = Reg(UInt((66*2+1).W))
  io.mulBusy := false.B
  when(state === sIDLE) {
    when(io.opValid) {
      opAreg := opA
      opBreg := opB
      state := sMUL
      resReg := 0.U
      io.mulBusy := true.B
      cnt := 0.U
    }
  }.elsewhen(state === sMUL) {
    cnt := cnt + 1.U
    io.mulBusy := true.B
    when(mulDone) {
      state := sDONE
    }
  }.elsewhen(state === sDONE) {
    io.mulBusy := false.B
    state := sIDLE
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
  val outRes = resReg(129,2)
  io.wbResult := Mux(isWordOp, signExt64(outRes(31,0)),
                              Mux(upper, outRes(127,64), outRes(63,0)))
}

object Multiplier extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Multiplier)
}


