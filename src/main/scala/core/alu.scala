package core

import chisel3._
import common.util.signExt64
import chisel3.util._
import common.OpConstants._


class ALUIO(srcWidth: Int) extends Bundle {
  val srcA = Input(UInt(srcWidth.W))
  val srcB = Input(UInt(srcWidth.W))
  val aluOP = Input(UInt(4.W))
  val out = Output(UInt(srcWidth.W))
}

class ALUIO_top(srcWidth: Int = 64) extends ALUIO(srcWidth)  {
  val isWordOp = Input(Bool())
}

class ALU(srcWidth: Int) extends Module  {
  val io = IO(new ALUIO(srcWidth))
  val shamt = if (srcWidth == 64) io.srcB(5, 0).asUInt() else io.srcB(4, 0).asUInt()
  val pc_4 = io.srcA + 4.U
  val res_mux = Array(
    ALU_ADD -> (io.srcA + io.srcB),
    ALU_SUB -> (io.srcA - io.srcB),
    ALU_SLT -> (io.srcA.asSInt() < io.srcB.asSInt()),
    ALU_SLTU -> (io.srcA < io.srcB),
    ALU_OR -> (io.srcA | io.srcB),
    ALU_AND -> (io.srcA & io.srcB),
    ALU_XOR -> (io.srcA ^ io.srcB),
    ALU_SLL -> (io.srcA << shamt),
    ALU_SRL -> (io.srcA >> shamt),
    ALU_SRA -> ((Cat(io.srcA(srcWidth - 1), io.srcA).asSInt() >> shamt) (srcWidth - 1, 0)).asUInt(),
    ALU_COPY_1 -> io.srcA,
    ALU_COPY_2 -> io.srcB
  )
  io.out := MuxLookup(io.aluOP, io.srcB, res_mux)

}

class ALU_top extends Module  {
  val io = IO(new ALUIO_top(64))
  val alu32 = Module(new ALU(32))
  val alu64 = Module(new ALU(64))

  // Connection
  alu32.io.srcA := io.srcA
  alu32.io.srcB := io.srcB
  alu32.io.aluOP := io.aluOP
  alu64.io.srcA := io.srcA
  alu64.io.srcB := io.srcB
  alu64.io.aluOP := io.aluOP

  // Output selection
  io.out := Mux(io.isWordOp, signExt64(alu32.io.out), alu64.io.out)

}

object ALU extends App {
  val stage = new chisel3.stage.ChiselStage()
  stage.emitVerilog(new ALU_top)
}