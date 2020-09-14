package core
import defines._
// See README.md for license details.
import chisel3._
//class decode extends Module with defines.decodeConsts {
//  val io = IO(new Bundle {
//    val ifetch = new defines.IFetch
//    val decodeOps = Output(new defines.DecodeOps)
//  })
//  // 如果需要Chisel自动推断类型，那么也需要有推断的根据，不能两边都是空的
//  io.decodeOps.pc := io.ifetch.pc
//  io.decodeOps.nPcSel := pc_br
//  io.decodeOps.instValid := true.B
//}
//
//object decode extends App {
//  chisel3.Driver.execute(args, ()=>new decode)
//}
