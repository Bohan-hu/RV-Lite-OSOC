package common
import chisel3._
object util {
  def signExt64(src: UInt) = {
    WireInit(SInt(64.W), src.asSInt()).asUInt()
  }
}
