package core

import chisel3._
import chisel3.util._
import common.OpConstants._
import mmu._
class Exe2Mem extends Bundle {
  val aluResult = UInt(64.W)
  val RdNum     = UInt(5.W)
  val R2val     = UInt(64.W)         // For L/S
  val WBSel     = UInt(2.W)
  val RFWen     = Bool()
  val isMemOp   = Bool()
  val MemOp     = UInt(2.W)
  val MemType   = UInt(3.W)
  val CSRCmd    = UInt(3.W)
  val isFence   = Bool()
  val exceInfo  = new ExceptionInfo
}

class Exe2Commit extends Bundle {
  // Excution Result
  val arithResult = UInt(64.W)
  val memResult = UInt(64.W)
  // Write back info
  val RdNum     = UInt(5.W)
  val WBSel     = UInt(2.W)
  val RFWen     = Bool()
  // CSR 
  val CSRCmd    = UInt(3.W)
  // Flush the pipeline and TLBs
  val isFence   = Bool()
  // Cause Exceptions or not?
  val exceInfo  = new ExceptionInfo
}

class BranchRedir extends Bundle {
  val TargetPC = UInt(64.W)
  val redir = Bool()
}

class EXUIO extends Bundle {
  val instBundleIn = Input(new InstBundle)
  val decode2Exe = Input(new Decode2Exe)
  val exe2IF = Output(new BranchRedir)
  val pauseReq = Output(Bool())
  val exe2Commit = Output(new Exe2Commit)
  val commit2Exe = Input(new commit2Exe)
  val instBundleOut = Output(new InstBundle)
  val mem2dmem = new NaiveBusM2S
  val csr2mmu = Flipped(new CSRMMU)
  val flush = Input(Bool())
}

class EXU extends Module {
  val io = IO(new EXUIO)
  // TODO: Deal with the bypass condition
  val op1 = io.decode2Exe.Op1
  val op2 = io.decode2Exe.Op2
  val rs2 = io.decode2Exe.R2val
//  /*
  val op1Hazard = io.commit2Exe.rdNum === io.decode2Exe.Raddr1 && io.decode2Exe.R1ren && io.commit2Exe.wen && io.instBundleIn.instValid
  val op2Hazard = io.commit2Exe.rdNum === io.decode2Exe.Raddr2 && io.decode2Exe.R2ren && io.commit2Exe.wen && io.instBundleIn.instValid
  val opHazard = op1Hazard | op2Hazard
//  */
//  val op1Hazard = false.B
//  val op2Hazard = false.B

  // Branch Unit
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
  io.exe2IF.redir := MuxLookup(io.decode2Exe.BrType, false.B, branchTakenCond) & io.instBundleIn.instValid & !opHazard
  io.exe2IF.TargetPC := Mux(io.decode2Exe.BrType === BR_JR, op1 + op2, io.instBundleIn.inst_pc + op2)

  // Arith Instruction
  val alu = Module(new ALU_top)
  alu.io.srcA := op1
  alu.io.srcB := op2
  alu.io.aluOP := io.decode2Exe.ALUOp
  alu.io.isWordOp := io.decode2Exe.isWordOp

  // Multiply instruction
  val mulu = Module(new Multiplier)
  mulu.io.opA := op1
  mulu.io.opB := op2
  mulu.io.mduOp := io.decode2Exe.ALUOp
  mulu.io.opValid := io.decode2Exe.FUType === FU_MUL && io.instBundleIn.instValid & !io.flush & !opHazard

  // Division instruction
  val divu = Module(new Divider)
  divu.io.opA := op1
  divu.io.opB := op2
  divu.io.mduOp := io.decode2Exe.ALUOp
  divu.io.opValid := io.decode2Exe.FUType === FU_DIV && io.instBundleIn.instValid  & !io.flush & !opHazard



  // Load / Store instruction
  // Can be exceptions
  val mem = Module(new MEM)
  val dmmu = Module(new MMU(isDMMU = true))
  // io.mem2dmem <> mem.io.mem2dmem
  dmmu.io.mem2mmu <> mem.io.mem2mmu
  dmmu.io.isStore := mem.io.MemOp === MEM_AMO || mem.io.MemOp === MEM_WRITE
  dmmu.io.flush := io.flush
  dmmu.io.csr2mmu <> io.csr2mmu

  mem.io.instPC           := io.instBundleIn.inst_pc
  mem.io.MemType          := io.decode2Exe.MemType
  mem.io.fuOp             := io.decode2Exe.ALUOp
  mem.io.isMemOp          := io.decode2Exe.isMemOp & io.instBundleIn.instValid
  mem.io.MemOp            := io.decode2Exe.MemOp
  mem.io.baseAddr         := op1
  mem.io.imm              := op2
  mem.io.R2Val            := io.decode2Exe.R2val
  mem.io.exceInfoIn       := io.decode2Exe.exceInfo
  io.exe2Commit.memResult := mem.io.memResult
  
  io.mem2dmem.memRreq       := mem.io.mem2dmem.memRreq | dmmu.io.dmemreq.memRreq
  io.mem2dmem.memWdata      := mem.io.mem2dmem.memWdata
  io.mem2dmem.memWen        := mem.io.mem2dmem.memWen
  io.mem2dmem.memWmask      := mem.io.mem2dmem.memWmask
  io.mem2dmem.memAddr       := Mux(dmmu.io.dmemreq.memRreq, dmmu.io.dmemreq.memAddr, mem.io.mem2dmem.memAddr)
  io.mem2dmem.memSize       := Mux(dmmu.io.dmemreq.memRreq, dmmu.io.dmemreq.memSize, mem.io.mem2dmem.memSize)
  dmmu.io.dmemreq.memRvalid := io.mem2dmem.memRvalid
  dmmu.io.dmemreq.memWrDone := io.flush
  dmmu.io.dmemreq.memRdata := io.mem2dmem.memRdata
  mem.io.mem2dmem.memWrDone := io.mem2dmem.memWrDone
  mem.io.mem2dmem.memRvalid := io.mem2dmem.memRvalid
  mem.io.mem2dmem.memRdata  := io.mem2dmem.memRdata


  io.pauseReq := divu.io.divBusy || mulu.io.mulBusy || mem.io.pauseReq || op1Hazard || op2Hazard

  // To next stage
  io.exe2Commit.exceInfo := RegNext(mem.io.exceInfoOut)
  io.exe2Commit.arithResult := RegNext(Mux(io.decode2Exe.FUType === FU_ALU, alu.io.out,
    Mux(io.decode2Exe.FUType === FU_MUL, mulu.io.wbResult, divu.io.wbResult)))
  io.exe2Commit.memResult := RegNext(mem.io.memResult)
  // Pass through
  io.exe2Commit.RdNum := RegNext(io.decode2Exe.RdNum)
  io.exe2Commit.WBSel := RegNext(io.decode2Exe.WBSel)
  io.exe2Commit.RFWen := RegNext(io.decode2Exe.RFWen)
  io.exe2Commit.CSRCmd := RegNext(io.decode2Exe.CSRCmd)
  io.exe2Commit.isFence := RegNext(io.decode2Exe.isFence)

  io.instBundleOut := RegNext(io.instBundleIn)
  io.instBundleOut.instValid := RegNext(Mux(io.flush, false.B, !io.pauseReq & io.instBundleIn.instValid))
  //   io.inst_out.instValid := RegNext(Mux(io.pause, io.inst_out.instValid, Mux(io.branchRedir.redir || io.exceptionRedir.redir, false.B, thisInstValid)))

}
object EXU extends App {
  chisel3.Driver.execute(args, () => { new EXU })
}