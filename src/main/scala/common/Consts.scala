package common

import chisel3._
import chisel3.util._
import scala.math._

trait Consts {
  // 用以译码的一些常量
  val RD_MSB = 11
  val RD_LSB = 7
  val RS1_MSB = 19
  val RS1_LSB = 15
  val RS2_MSB = 24
  val RS2_LSB = 20

  val CSR_ADDR_MSB = 31
  val CSR_ADDR_LSB = 20

  //    val NOP = 0x0013.U(32.W)  // NOP 指令

}


