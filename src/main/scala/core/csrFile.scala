package core

import chisel3.util._
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils

object ExceptionNo {
  def instrAddrMisaligned = 0
  def instrAccessFault    = 1
  def illegalInstr        = 2
  def breakPoint          = 3
  def loadAddrMisaligned  = 4
  def loadAccessFault     = 5
  def storeAddrMisaligned = 6
  def storeAccessFault    = 7
  def ecallU              = 8
  def ecallS              = 9
  def ecallM              = 11
  def instrPageFault      = 12
  def loadPageFault       = 13
  def storePageFault      = 15

  val ExcePriority = Seq(
    breakPoint, // TODO: different BP has different priority
    instrPageFault,
    instrAccessFault,
    illegalInstr,
    instrAddrMisaligned,
    ecallM, ecallS, ecallU,
    storeAddrMisaligned,
    loadAddrMisaligned,
    storePageFault,
    loadPageFault,
    storeAccessFault,
    loadAccessFault
  )
}

object CSRAddr {
  // Machine Information Registers
  val mvendorid = 0xf11
  val marchid = 0xf12
  val mimpid = 0xf13
  val mhartid = 0xf14
  // Machine Trap Setup
  val mstatus = 0x300
  val misa = 0x301
  val medeleg = 0x302
  val mideleg = 0x303
  val mie = 0x304
  val mtvec = 0x305
  val mcounteren = 0x306
  // Machine Trap Handling
  val mscratch = 0x340
  val mepc = 0x341
  val mcause = 0x342
  val mtval = 0x343
  val mip = 0x344
  val mtinst = 0x34a
  val mtval2 = 0x34b
  // Machine Memory Protection
  val pmpcfg0 = 0x3a0
  val pmpcfg2 = 0x3a2
  val pmpcfg4 = 0x3a4
  val pmpcfg6 = 0x3a6
  val pmpcfg8 = 0x3a8
  val pmpcfg10 = 0x3aa
  val pmpcfg12 = 0x3ac
  val pmpcfg14 = 0x3ae
  // TODO:PMPADDRx


  // Machine Counter/Timers
  val mcycle = 0xb00
  val minstret = 0xb02
  val mhpmcounter3 = 0xb03
  val mhpmcounter4 = 0xb04
  val mhpmcounter5 = 0xb05
  val mhpmcounter6 = 0xb06
  val mhpmcounter7 = 0xb07
  val mhpmcounter8 = 0xb08
  val mhpmcounter9 = 0xb09
  val mhpmcounter10 = 0xb0a
  val mhpmcounter11 = 0xb0b
  val mhpmcounter12 = 0xb0c
  val mhpmcounter13 = 0xb0d
  val mhpmcounter14 = 0xb0e
  val mhpmcounter15 = 0xb0f
  val mhpmcounter16 = 0xb10
  val mhpmcounter17 = 0xb11
  val mhpmcounter18 = 0xb12
  val mhpmcounter19 = 0xb13
  val mhpmcounter20 = 0xb14
  val mhpmcounter21 = 0xb15
  val mhpmcounter22 = 0xb16
  val mhpmcounter23 = 0xb17
  val mhpmcounter24 = 0xb18
  val mhpmcounter25 = 0xb19
  val mhpmcounter26 = 0xb1a
  val mhpmcounter27 = 0xb1b
  val mhpmcounter28 = 0xb1c
  val mhpmcounter29 = 0xb1d
  val mhpmcounter30 = 0xb1e
  val mhpmcounter31 = 0xb1f
  // Machine Counter Setup
  val mcountinhibit = 0x320
  val mhpmevent3 = 0x323
  val mhpmevent4 = 0x324
  val mhpmevent5 = 0x325
  val mhpmevent6 = 0x326
  val mhpmevent7 = 0x327
  val mhpmevent8 = 0x328
  val mhpmevent9 = 0x329
  val mhpmevent10 = 0x32a
  val mhpmevent11 = 0x32b
  val mhpmevent12 = 0x32c
  val mhpmevent13 = 0x32d
  val mhpmevent14 = 0x32e
  val mhpmevent15 = 0x32f
  val mhpmevent16 = 0x330
  val mhpmevent17 = 0x331
  val mhpmevent18 = 0x332
  val mhpmevent19 = 0x333
  val mhpmevent20 = 0x334
  val mhpmevent21 = 0x335
  val mhpmevent22 = 0x336
  val mhpmevent23 = 0x337
  val mhpmevent24 = 0x338
  val mhpmevent25 = 0x339
  val mhpmevent26 = 0x33a
  val mhpmevent27 = 0x33b
  val mhpmevent28 = 0x33c
  val mhpmevent29 = 0x33d
  val mhpmevent30 = 0x33e
  val mhpmevent31 = 0x33f

  // =========================== USER MODE BEGIN =================================

  // User Trap Setup
  val ustatus = 0x000
  val uie = 0x004
  val utvec = 0x005
  // User Trap Handling
  val uscratch = 0x040
  val uepc = 0x041
  val ucause = 0x042
  val utval = 0x043
  val uip = 0x044
  // User Counter/Timers
  val cycle = 0xc00
  val time = 0xc01
  val instret = 0xc02
  val hpmcounter3 = 0xc03
  val hpmcounter4 = 0xc04
  val hpmcounter5 = 0xc05
  val hpmcounter6 = 0xc06
  val hpmcounter7 = 0xc07
  val hpmcounter8 = 0xc08
  val hpmcounter9 = 0xc09
  val hpmcounter10 = 0xc0a
  val hpmcounter11 = 0xc0b
  val hpmcounter12 = 0xc0c
  val hpmcounter13 = 0xc0d
  val hpmcounter14 = 0xc0e
  val hpmcounter15 = 0xc0f
  val hpmcounter16 = 0xc10
  val hpmcounter17 = 0xc11
  val hpmcounter18 = 0xc12
  val hpmcounter19 = 0xc13
  val hpmcounter20 = 0xc14
  val hpmcounter21 = 0xc15
  val hpmcounter22 = 0xc16
  val hpmcounter23 = 0xc17
  val hpmcounter24 = 0xc18
  val hpmcounter25 = 0xc19
  val hpmcounter26 = 0xc1a
  val hpmcounter27 = 0xc1b
  val hpmcounter28 = 0xc1c
  val hpmcounter29 = 0xc1d
  val hpmcounter30 = 0xc1e
  val hpmcounter31 = 0xc1f

  // ============================= USER MODE END ==========================

  // ============================== SUPERVISOR MODE BEGIN =====================
  // Supervisor Trap Setup
  val sstatus = 0x100
  val sedeleg = 0x102
  val sideleg = 0x103
  val sie = 0x104
  val stvec = 0x105
  val scounteren = 0x106
  // Supervisor Trap Handling
  val sscratch = 0x140
  val sepc = 0x141
  val scause = 0x142
  val stval = 0x143
  val sip = 0x144
  // Supervisor Protection and Translation
  val satp = 0x180
  // ======================= SUPERVISOR MODE END =======================

}

class mstatus extends Bundle {
  val SD = Bool()
  val reserved = UInt((64 - 39).W)
  val MBE = Bool()
  val SBE = Bool()
  val SXL = UInt(2.W)
  val UXL = UInt(2.W)
  val reserved2 = UInt((32 - 23).W)
  val TSR = Bool()
  val TW = Bool()
  val TVM = Bool()
  val MXR = Bool()
  val SUM = Bool()
  val MPRV = Bool()
  val XS = UInt(2.W)
  val FS = UInt(2.W)
  val MPP = UInt(2.W)
  val HPP = UInt(2.W)
  val SPP = Bool()
  val MPIE = Bool()
  val UBE = Bool()
  val SPIE = Bool()
  val reserved4 = Bool()
  val MIE = Bool()
  val reserved5 = Bool()
  val SIE = Bool()
  val reserved6 = Bool()
}

class mtvec_t extends Bundle {
  val BASE = UInt(62.W)
  val MODE = UInt(2.W)
}



class CSRFile extends Module {
  val io = IO(new Bundle() {
    val csrWrAddr = Input(UInt(10.W))
    val csrWData = Input(UInt(64.W))
    val csrRdAddr = Input(UInt(10.W))
  })
  // Hardwired Registers
  val misa_extension = "I"
  val extension_val = misa_extension.map(e => 1 << (e - 'A')).reduce(_ | _).asUInt()
  val misa = WireInit(UInt(64.W), Cat(2.U(2.W), 0.U(60.W)) | extension_val)
  val mvendorid = WireInit(UInt(32.W), 0.U)
  val marchid = WireInit(UInt(64.W), 0.U)
  val mimpid = WireInit(UInt(64.W), 0.U)
  val mhartid = WireInit(UInt(64.W), 0.U)
  // Configurable Registers

  // mtvec
  val reset_mtvec = WireInit(0.U.asTypeOf(new mtvec_t))   // todo: determine the reset value
  reset_mtvec.BASE := 0.U
  reset_mtvec.MODE := 0.U
  val mtvec = RegInit(reset_mtvec.asUInt())

  // mdeleg and mideleg
  val medeleg = RegInit(UInt(64.W), 0.U)         // Machine Exception Delegation Register
  val mideleg = RegInit(UInt(64.W), 0.U)        // Machine Interrupt Delegation Register
  val medelgAndMask = 1.U << 11                 // medelg[11] is hardwired to zero

  val mcounteren = RegInit(UInt(64.W),0.U)
  val mcause = RegInit(UInt(64.W),0.U)
  BoringUtils.addSource(RegNext(mcause), "difftestMcause")
  val mtval = RegInit(UInt(64.W),0.U)
  val mepc = RegInit(UInt(64.W),0.U)
  BoringUtils.addSource(RegNext(mepc), "difftestMepc")
  val mie = RegInit(UInt(64.W),0.U)
  val mip = RegInit(UInt(64.W),0.U)

  //  val mip
  val mstatus = RegInit(UInt(64.W),0x1800.U)
  BoringUtils.addSource(RegNext(mstatus), "difftestMstatus")
  val mscratch = RegInit(UInt(64.W), 0.U)

  val pmpcfg0 = RegInit(UInt(64.W), 0.U)
  val pmpcfg1 = RegInit(UInt(64.W), 0.U)
  val pmpcfg2 = RegInit(UInt(64.W), 0.U)
  val pmpcfg3 = RegInit(UInt(64.W), 0.U)
  val pmpaddr0 = RegInit(UInt(64.W), 0.U)
  val pmpaddr1 = RegInit(UInt(64.W), 0.U)
  val pmpaddr2 = RegInit(UInt(64.W), 0.U)
  val pmpaddr3 = RegInit(UInt(64.W), 0.U)

  val csrRdAddr = Wire(UInt(8.W))
  csrRdAddr := DontCare
  val csrMapping = Array(
    CSRAddr.mvendorid   ->    mvendorid,
//    // Machine Information Registers
    CSRAddr.marchid     ->    marchid,
    CSRAddr.mimpid      ->    mimpid,
    CSRAddr.mhartid     ->    mhartid,
//    // Machine Trap Setup
    CSRAddr.mstatus     ->    mstatus   ,
    CSRAddr.misa        ->    misa,
    CSRAddr.medeleg     ->    medeleg   ,
    CSRAddr.mideleg     ->    mideleg   ,
    CSRAddr.mie         ->    mie       ,
    CSRAddr.mtvec       ->    mtvec     ,
    CSRAddr.mcounteren  ->    mcounteren,
//    // Machine Trap Handling
    CSRAddr.mscratch    ->   mscratch   ,
    CSRAddr.mepc        ->   mepc       ,
    CSRAddr.mcause      ->   mcause     ,
    CSRAddr.mtval       ->   mtval      ,
    CSRAddr.mip         ->   mip        ,
//    CSRAddr.mtinst      ->   mtinst     ,
    CSRAddr.mtval       ->   mtval      ,
  )
  val readOnlyCSR = List(
    CSRAddr.mvendorid,
    CSRAddr.marchid,
    CSRAddr.mimpid,
    CSRAddr.mhartid
  )
  val WrMaskedCSR = Map(
    CSRAddr.mstatus     ->  "b001100100111".U,
  )
  val sideEffectCSR = Map(    // Address: Int -> (Initial Value: UInt, Write Value: UInt) => Return Value: UInt
    CSRAddr.mstatus     ->  "b001100100111".U,
  )
  val csrWen = Wire(Bool()) // TODO
  csrWen := true.B
  // CSRWr
  csrMapping.map( kv => if (!readOnlyCSR.contains(kv._1)) { // CSR is Not READ Only
      when(io.csrWrAddr === kv._1.U && csrWen){
        if(WrMaskedCSR.contains(kv._1)) { // CSR Write is Masked
          kv._2 := io.csrWData & WrMaskedCSR(kv._1)
        } else {
          kv._2 := io.csrWData
        }
      }
  }
  )
  // Writing to a read-only CSR will cause an illegal instruction exception, or writing to an unimplemented CSR
  val writeCSRExists = csrMapping.map(kv => io.csrWrAddr === kv._1.U).reduce(_|_).asBool()
  val writeReadOnlyCSR = readOnlyCSR.map(io.csrWrAddr === _.U).reduce(_|_).asBool()
  val writeillegalCSR = ( (~writeCSRExists) || writeReadOnlyCSR ) & csrWen
  dontTouch(writeillegalCSR)

}

object CSRFile extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new CSRFile)
}
