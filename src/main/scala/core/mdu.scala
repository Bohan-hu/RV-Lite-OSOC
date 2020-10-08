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

class DivIO extends Bundle {
  val opA = Input(UInt(64.W))
  val opB = Input(UInt(64.W))
  val mduOp = Input(UInt(4.W))
  val opValid = Input(Bool())
  val divBusy = Output(Bool())
  val wbResult = Output(UInt(64.W))
}

class Divider extends Module {
  val io = IO(new DivIO)
  val fu = Module(new Divider_fu)
  val unsigned = io.mduOp === MDU_DIVU || io.mduOp === MDU_REMU || io.mduOp === MDU_DIVUW || io.mduOp === MDU_REMUW
  val isWord = io.mduOp === MDU_DIVW || io.mduOp === MDU_REMW || io.mduOp === MDU_DIVUW || io.mduOp === MDU_REMUW
  val isRem = io.mduOp === MDU_REMU || io.mduOp === MDU_REMUW || io.mduOp === MDU_REMW || io.mduOp === MDU_REM
  val op0_raw = Mux(isWord,
                      Mux(unsigned, Cat(Fill(32,0.U(1.W)), io.opA(31,0)), signExt64(io.opA(31,0))),
                    io.opA)
  val op1_raw = Mux(isWord,
    Mux(unsigned, Cat(Fill(32,0.U(1.W)), io.opB(31,0)), signExt64(io.opB(31,0))),
    io.opB)
  fu.io.in.bits(0) := op0_raw
  fu.io.in.bits(1) := op1_raw
  fu.io.out.ready := true.B
  fu.io.in.valid := io.opValid
  fu.io.sign := ~unsigned
  io.divBusy := fu.io.busy
  io.wbResult := Mux(isRem, fu.io.out.bits(127,64), fu.io.out.bits(63,0))
}


// Divider code borrowed from Nutshell
class DivIO_fu extends Bundle {
  val in = Flipped(DecoupledIO(Vec(2, Output(UInt(64.W)))))
  val sign = Input(Bool())
  val out = DecoupledIO(Output(UInt((64 * 2).W)))
  val busy = Output(Bool())
}
class Divider_fu extends Module {
  val io = IO(new DivIO_fu)

  def abs(a: UInt, sign: Bool): (Bool, UInt) = {
    val s = a(64 - 1) && sign
    (s, Mux(s, -a, a))
  }

  val s_idle :: s_log2 :: s_shift :: s_compute :: s_finish :: Nil = Enum(5)
  val state = RegInit(s_idle)
  val newReq = (state === s_idle) && io.in.fire()

  val (a, b) = (io.in.bits(0), io.in.bits(1))
  val divBy0 = b === 0.U(64.W)

  val shiftReg = Reg(UInt((1 + 64 * 2).W))
  val hi = shiftReg(64 * 2, 64)
  val lo = shiftReg(64 - 1, 0)

  val (aSign, aVal) = abs(a, io.sign)
  val (bSign, bVal) = abs(b, io.sign)
  val aSignReg = RegEnable(aSign, newReq)
  val qSignReg = RegEnable((aSign ^ bSign) && !divBy0, newReq)
  val bReg = RegEnable(bVal, newReq)
  val aValx2Reg = RegEnable(Cat(aVal, "b0".U), newReq)

  val cnt = Counter(64)
  when (newReq) {
    state := s_log2
  } .elsewhen (state === s_log2) {
    // `canSkipShift` is calculated as following:
    //   bEffectiveBit = Log2(bVal, XLEN) + 1.U
    //   aLeadingZero = 64.U - aEffectiveBit = 64.U - (Log2(aVal, XLEN) + 1.U)
    //   canSkipShift = aLeadingZero + bEffectiveBit
    //     = 64.U - (Log2(aVal, XLEN) + 1.U) + Log2(bVal, XLEN) + 1.U
    //     = 64.U + Log2(bVal, XLEN) - Log2(aVal, XLEN)
    //     = (64.U | Log2(bVal, XLEN)) - Log2(aVal, XLEN)  // since Log2(bVal, XLEN) < 64.U
    val canSkipShift = (64.U | Log2(bReg)) - Log2(aValx2Reg)
    // When divide by 0, the quotient should be all 1's.
    // Therefore we can not shift in 0s here.
    // We do not skip any shift to avoid this.
    cnt.value := Mux(divBy0, 0.U, Mux(canSkipShift >= (64-1).U, (64-1).U, canSkipShift))
    state := s_shift
  } .elsewhen (state === s_shift) {
    shiftReg := aValx2Reg << cnt.value
    state := s_compute
  } .elsewhen (state === s_compute) {
    val enough = hi.asUInt >= bReg.asUInt
    shiftReg := Cat(Mux(enough, hi - bReg, hi)(64 - 1, 0), lo, enough)
    cnt.inc()
    when (cnt.value === (64-1).U) { state := s_finish }
  } .elsewhen (state === s_finish) {
    state := s_idle
  }

  val r = hi(64, 1)
  val resQ = Mux(qSignReg, -lo, lo)
  val resR = Mux(aSignReg, -r, r)
  io.out.bits := Cat(resR, resQ)

  io.out.valid := (state === s_finish)
  io.busy := state =/= s_finish && state =/= s_idle || (state === s_idle && io.in.valid)
  io.in.ready := (state === s_idle)
}

object Multiplier extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Multiplier)
}

object Divider extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new Divider)
}


