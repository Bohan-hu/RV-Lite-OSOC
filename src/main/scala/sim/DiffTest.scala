package sim
import chisel3._
import chisel3.util.experimental.BoringUtils

class DiffTestIO extends Bundle {
  val r = Output(Vec(32, UInt(64.W)))
  val commit = Output(Bool())
  val isMultiCommit = 0.B
  val thisPC = Output(UInt(64.W))
  val thisINST = Output(UInt(32.W))
  val isMMIO = Output(Bool())
  val isRVC = 0.B
  val isRVC2 = 0.B
  val intrNO = Output(UInt(64.W))

  val priviledgeMode = Output(UInt(2.W))
  val mstatus = Output(UInt(64.W))
  val sstatus = Output(UInt(64.W))
  val mepc = Output(UInt(64.W))
  val sepc = Output(UInt(64.W))
  val mcause = Output(UInt(64.W))
  val scause = Output(UInt(64.W))
}

