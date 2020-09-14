// See README.md for license details.
package core.defines

import chisel3._
import chisel3.util._

trait decodeConsts {
  val pc_seq :: pc_br :: pc_j :: pc_jr :: pc_exc :: Nil = Enum(5) // Next PC Select
  val br_neq :: br_eq :: br_ge :: br_geu :: br_lt :: br_ltu :: br_j :: br_jr :: Nil = Enum(8) // Branch Type
  val op1_rs1 :: op1_imu :: op1_imz :: Nil = Enum(3) // RS1 Type
  val op2_rs2 :: op2_imi :: op2_ims :: op2_pc :: Nil = Enum(4) // RS2 Type
}


//class DecodeOps extends Bundle {
//  val instValid = Output(Bool())
//  val pc = Output(UInt())
//  val nPcSel = Output(UInt())
//  val brType = Output(UInt())
//  val isJ = Output(Bool())
//  val op1Src = Output(UInt())
//  val op2Src = Output(UInt())
//  val aluOp = Output(UInt())
//  val wbEn = Output(UInt())
//  val wbSel = Output(UInt())
//  val memOP = Output(UInt())
//  val memWen = Output(Bool())
//  val memMask = Output(UInt(4.W))
//  val csrOP = Output(UInt())
//}

class DecodeOps extends Bundle {
  val instValid = Bool()
  val pc = UInt()
  val nPcSel = UInt()
  //  val brType = Output(UInt())
  //  val isJ = Output(Bool())
  //  val op1Src = Output(UInt())
  //  val op2Src = Output(UInt())
  //  val aluOp = Output(UInt())
  //  val wbEn = Output(UInt())
  //  val wbSel = Output(UInt())
  //  val memOP = Output(UInt())
  //  val memWen = Output(Bool())
  //  val memMask = Output(UInt(4.W))
  //  val csrOP = Output(UInt())
}

class AXIIO extends Bundle {

}