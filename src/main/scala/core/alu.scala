package core

import chisel3._
import chisel3.util._

trait ALUConsts {
  val alu_ADD :: alu_SUB :: alu_AND :: alu_OR :: alu_XOR :: alu_SLT :: alu_SLL :: alu_SLTU :: alu_SRL :: alu_SRA :: alu_A :: alu_B :: Nil = Enum(12)
  val op_Arith :: op_Compare :: op_Shift :: op_Logic :: Nil = Enum(4)
}

class ALUIO(srcWidth: Int) extends Bundle with ConfigParams {
  val srcA = Input(UInt(srcWidth.W))
  val srcB = Input(UInt(srcWidth.W))
  val aluOP = Input(UInt(4.W))
  val aluType = Input(UInt(4.W))
  val out = Output(UInt(srcWidth.W))
}

class ALUIO_top(srcWidth: Int = 64) extends ALUIO(srcWidth) with ConfigParams {
  val isWordOp = Input(Bool())
}

class ALU(srcWidth: Int) extends Module with ALUConsts with ConfigParams {
  val io = IO(new ALUIO(srcWidth))

  //   ARITHMETIC
  val sum = io.srcA + Mux(io.aluOP === alu_ADD, io.srcB, -io.srcB)

  // COMPARE
  val cmp_mux = Array(
    alu_SLT -> (io.srcA.asSInt() < io.srcB.asSInt()),
    alu_SLTU -> (io.srcA < io.srcB))
  val cmp = MuxLookup(io.aluOP, io.srcB, cmp_mux)
  // LOGIC
  val logic_mux = Array(
    alu_OR -> (io.srcA | io.srcB),
    alu_AND -> (io.srcA & io.srcB),
    alu_XOR -> (io.srcA ^ io.srcB)
  )
  val logic = MuxLookup(io.aluOP, io.srcB, logic_mux)
  // SHIFT(TODO)
  val shamt = io.srcB(5, 0).asUInt()
  val shift = 0.U

  val out_mux = Array(
    op_Arith -> sum,
    op_Compare -> cmp,
    op_Logic -> logic,
    op_Shift -> shift
  )
  io.out := MuxLookup(io.aluType, io.srcB, out_mux)
}

class ALU_top extends Module with ALUConsts with ConfigParams {
  val io = IO(new ALUIO_top(XLEN))
  val alu32 = Module(new ALU(32))
  val alu64 = Module(new ALU(64))

  // Connection
  alu32.io.srcA := io.srcA
  alu32.io.srcB := io.srcB
  alu32.io.aluOP := io.aluOP
  alu32.io.aluType := io.aluType
  alu64.io.srcA := io.srcA
  alu64.io.srcB := io.srcB
  alu64.io.aluOP := io.aluOP
  alu64.io.aluType := io.aluType

  // Sign Extension
  val alu32_out = Wire(SInt(64.W))
  alu32_out := alu32.io.out.asSInt()

  // Output selection
  io.out := Mux(io.isWordOp, alu32_out.asUInt(), alu64.io.out)

}

object ALU extends App {
  val stage = new chisel3.stage.ChiselStage()
  stage.emitVerilog(new ALU_top)
}