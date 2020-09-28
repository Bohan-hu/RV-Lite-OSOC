package core

import chisel3._
import chisel3.util._
import common.OpConstants._

class Exe2Mem extends Bundle {
  val aluResult = UInt(64.W)
  val RdNum = UInt(5.W)
  val R2val = UInt(64.W)  // For L/S
  val WBSel = UInt(2.W)
  val RFWen = Bool()
  val isMemOp = Bool()
  val MemOp = UInt(2.W)
  val MemType = UInt(3.W)
  val CSRCmd = UInt(3.W)
  val isFence = Bool()
}
class BranchRedir extends Bundle {
  val TargetPC = Output(UInt(64.W))
  val redir = Bool()
}
class EXUIO extends Bundle {
  val instValid = Input(Bool())
  val inst = Input(UInt(32.W))
  val pc = Input(UInt(64.W))
  //  val wbInfoMem = Input()
  //  val wbInfowb = Input()
  val decode2Exe = Input(new Decode2Exe)
  val exe2Mem = Output(new Exe2Mem)
  val exe2IF = Output(new BranchRedir)
}

class EXU extends Module {
  val io = IO(new EXUIO)
  val alu = Module(new ALU_top)
  // Todo: Deal with the bypass condition
  val op1 = io.decode2Exe.Op1
  val op2 = io.decode2Exe.Op2
  val rs2 = io.decode2Exe.R2val

  alu.io.srcA := io.decode2Exe.Op1
  alu.io.srcB := io.decode2Exe.Op2
  alu.io.aluOP := io.decode2Exe.ALUOp
  alu.io.isWordOp := false.B  // todo
  // Pass through
  io.exe2Mem.aluResult := alu.io.out
  io.exe2Mem.RdNum     := io.decode2Exe.RdNum
  io.exe2Mem.R2val     := io.decode2Exe.R2val
  io.exe2Mem.WBSel     := io.decode2Exe.WBSel
  io.exe2Mem.RFWen     := io.decode2Exe.RFWen
  io.exe2Mem.isMemOp   := io.decode2Exe.isMemOp
  io.exe2Mem.MemOp     := io.decode2Exe.MemOp
  io.exe2Mem.MemType   := io.decode2Exe.MemType
  io.exe2Mem.CSRCmd    := io.decode2Exe.CSRCmd
  io.exe2Mem.isFence   := io.decode2Exe.isFence
  val branchTakenCond = Array(
    BR_N -> false.B,
    BR_J -> true.B,
    BR_JR -> true.B,
    BR_EQ -> (op1 === rs2),
    BR_NE -> (op1 =/= rs2),
    BR_GE -> (op1.asSInt >= rs2.asSInt),
    BR_GEU -> (op1 >= rs2),
    BR_LT -> (op1.asSInt < rs2.asSInt),
    BR_LTU -> (op1 < rs2)
  )
  io.exe2IF.redir := MuxLookup(io.decode2Exe.BrType, false.B, branchTakenCond)
  io.exe2IF.TargetPC := Mux(io.decode2Exe.BrType === BR_JR, alu.io.out, io.pc + io.decode2Exe.Op2)
}

object EXU extends App {
  chisel3.Driver.execute(args, () => { new EXU })
}