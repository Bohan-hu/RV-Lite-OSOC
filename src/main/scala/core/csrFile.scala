package core

import chisel3.util._
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.experimental.BoringUtils
import common.OpConstants._

object IntNo {
  val MEI = 11
  val SEI = 9
  val MTI = 7
  val STI = 5
  val MSI = 3
  val SSI = 1
}

object ExceptionNo {
  def instrAddrMisaligned = 0

  def instrAccessFault = 1

  def illegalInstr = 2

  def breakPoint = 3

  def loadAddrMisaligned = 4

  def loadAccessFault = 5

  def storeAddrMisaligned = 6

  def storeAccessFault = 7

  def ecallU = 8

  def ecallS = 9

  def ecallM = 11

  def instrPageFault = 12

  def loadPageFault = 13

  def storePageFault = 15

  val ExcePriority = Seq(
    breakPoint,
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
  // Machine Information Registers.U
  val mvendorid = 0xf11.U
  val marchid = 0xf12.U
  val mimpid = 0xf13.U
  val mhartid = 0xf14.U
  // Machine Trap Setup.U
  val mstatus = 0x300.U
  val misa = 0x301.U
  val medeleg = 0x302.U
  val mideleg = 0x303.U
  val mie = 0x304.U
  val mtvec = 0x305.U
  val mcounteren = 0x306.U
  // Machine Trap Handling.U
  val mscratch = 0x340.U
  val mepc = 0x341.U
  val mcause = 0x342.U
  val mtval = 0x343.U
  val mip = 0x344.U
  val mtinst = 0x34a.U
  val mtval2 = 0x34b.U
  // Machine Memory Protection.U
  val pmpcfg0 = 0x3a0.U  
  val pmpcfg1 = 0x3a1.U
  val pmpcfg2 = 0x3a2.U
  val pmpcfg3 = 0x3a3.U
  val pmpaddr0 = 0x3B0.U
  val pmpaddr1 = 0x3B1.U
  val pmpaddr2 = 0x3B2.U
  val pmpaddr3 = 0x3B3.U

  // TODO:PMPADD.UR.Ux


  // Machine Counter/Timers.U
  val mcycle = 0xb00.U
  val minstret = 0xb02.U
  val mhpmcounter3 = 0xb03.U
  val mhpmcounter4 = 0xb04.U
  val mhpmcounter5 = 0xb05.U
  val mhpmcounter6 = 0xb06.U
  val mhpmcounter7 = 0xb07.U
  val mhpmcounter8 = 0xb08.U
  val mhpmcounter9 = 0xb09.U
  val mhpmcounter10 = 0xb0a.U
  val mhpmcounter11 = 0xb0b.U
  val mhpmcounter12 = 0xb0c.U
  val mhpmcounter13 = 0xb0d.U
  val mhpmcounter14 = 0xb0e.U
  val mhpmcounter15 = 0xb0f.U
  val mhpmcounter16 = 0xb10.U
  val mhpmcounter17 = 0xb11.U
  val mhpmcounter18 = 0xb12.U
  val mhpmcounter19 = 0xb13.U
  val mhpmcounter20 = 0xb14.U
  val mhpmcounter21 = 0xb15.U
  val mhpmcounter22 = 0xb16.U
  val mhpmcounter23 = 0xb17.U
  val mhpmcounter24 = 0xb18.U
  val mhpmcounter25 = 0xb19.U
  val mhpmcounter26 = 0xb1a.U
  val mhpmcounter27 = 0xb1b.U
  val mhpmcounter28 = 0xb1c.U
  val mhpmcounter29 = 0xb1d.U
  val mhpmcounter30 = 0xb1e.U
  val mhpmcounter31 = 0xb1f.U
  // Machine Counter Setup.U
  val mcountinhibit = 0x320.U
  val mhpmevent3 = 0x323.U
  val mhpmevent4 = 0x324.U
  val mhpmevent5 = 0x325.U
  val mhpmevent6 = 0x326.U
  val mhpmevent7 = 0x327.U
  val mhpmevent8 = 0x328.U
  val mhpmevent9 = 0x329.U
  val mhpmevent10 = 0x32a.U
  val mhpmevent11 = 0x32b.U
  val mhpmevent12 = 0x32c.U
  val mhpmevent13 = 0x32d.U
  val mhpmevent14 = 0x32e.U
  val mhpmevent15 = 0x32f.U
  val mhpmevent16 = 0x330.U
  val mhpmevent17 = 0x331.U
  val mhpmevent18 = 0x332.U
  val mhpmevent19 = 0x333.U
  val mhpmevent20 = 0x334.U
  val mhpmevent21 = 0x335.U
  val mhpmevent22 = 0x336.U
  val mhpmevent23 = 0x337.U
  val mhpmevent24 = 0x338.U
  val mhpmevent25 = 0x339.U
  val mhpmevent26 = 0x33a.U
  val mhpmevent27 = 0x33b.U
  val mhpmevent28 = 0x33c.U
  val mhpmevent29 = 0x33d.U
  val mhpmevent30 = 0x33e.U
  val mhpmevent31 = 0x33f.U

  // =========================== USER MODE BEGIN ===============================.U==

  // User Counter/Timers.U
  val cycle = 0xc00.U
  val time = 0xc01.U
  val instret = 0xc02.U
  val hpmcounter3 = 0xc03.U
  val hpmcounter4 = 0xc04.U
  val hpmcounter5 = 0xc05.U
  val hpmcounter6 = 0xc06.U
  val hpmcounter7 = 0xc07.U
  val hpmcounter8 = 0xc08.U
  val hpmcounter9 = 0xc09.U
  val hpmcounter10 = 0xc0a.U
  val hpmcounter11 = 0xc0b.U
  val hpmcounter12 = 0xc0c.U
  val hpmcounter13 = 0xc0d.U
  val hpmcounter14 = 0xc0e.U
  val hpmcounter15 = 0xc0f.U
  val hpmcounter16 = 0xc10.U
  val hpmcounter17 = 0xc11.U
  val hpmcounter18 = 0xc12.U
  val hpmcounter19 = 0xc13.U
  val hpmcounter20 = 0xc14.U
  val hpmcounter21 = 0xc15.U
  val hpmcounter22 = 0xc16.U
  val hpmcounter23 = 0xc17.U
  val hpmcounter24 = 0xc18.U
  val hpmcounter25 = 0xc19.U
  val hpmcounter26 = 0xc1a.U
  val hpmcounter27 = 0xc1b.U
  val hpmcounter28 = 0xc1c.U
  val hpmcounter29 = 0xc1d.U
  val hpmcounter30 = 0xc1e.U
  val hpmcounter31 = 0xc1f.U

  // ============================= USER MODE END =========================

  // ============================== SUPERVISOR MODE BEGIN =====================
  // Supervisor Trap Setup.U
  val sstatus = 0x100.U
  val sie = 0x104.U
  val stvec = 0x105.U
  val scounteren = 0x106.U
  // Supervisor Trap Handling.U
  val sscratch = 0x140.U
  val sepc = 0x141.U
  val scause = 0x142.U
  val stval = 0x143.U
  val sip = 0x144.U
  // Supervisor Protection and Translation
  val satp = 0x180.U
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

class ExceptionInfo extends Bundle {
  val cause = UInt(64.W)
  val tval = UInt(64.W)
  val epc = UInt(64.W)
  val valid = Bool()
}

class PLICCSR extends Bundle {
  val intrVec = UInt(3.W)       // TODO: Should be defined by parameter
  val meip = Bool()
}

class CLINTCSR extends Bundle {
  val mtip = Bool()
  val msip = Bool()
}

class INTCtrl extends Bundle {
  val privMode = UInt(2.W)
  val mie = UInt(64.W)
  val mip = UInt(64.W)
  val mideleg = UInt(64.W)
  val sie = Bool()
  val intGlobalEnable = Bool()
}

class CSRMMU extends Bundle {
  val satpPPN = Output(UInt(44.W))
  val asid = Output(UInt(16.W))
  val sum = Output(Bool())
  val enableSv39 = Output(Bool())
  val enableLSVM = Output(Bool())
  val mxr = Output(Bool())
  val tvm = Output(Bool())
  val tw = Output(Bool())
  val tsr = Output(Bool())
}

class commitCSR extends Bundle {
  val instValid = Input(Bool())
  val inst = Input(UInt(32.W))
  val instPC = Input(UInt(64.W))
  val csrWData = Input(UInt(64.W))
  val csrAddr = Input(UInt(12.W))
  val csrOp = Input(UInt(3.W))
  val instRd = Input(UInt(5.W)) // Destination Register
  val instRs = Input(UInt(5.W))
  val csrRdata = Output(UInt(64.W))
  val exceptionInfo = Input(new ExceptionInfo)
}

class ExceptionRedir extends Bundle {
  val redirPC = UInt(64.W)
  val redir = Bool()
}

class DecodePrivCheck extends Bundle {
  val csrAddr = Input(UInt(12.W))
  val csrOp = Input(UInt(3.W))
  val instRd = Input(UInt(5.W))
  val instRs = Input(UInt(5.W))
  val instImm = Input(UInt(32.W))
  val illegalInst = Output(Bool())
}

class CSRIO extends Bundle {
  val commitCSR = new commitCSR
  val illegalInst = Output(Bool())
  // To IF, if is eret, go to epc, else goto handlerentry
  val ifRedir = Output(new ExceptionRedir)
  // To MMU
  val csrMMU = new CSRMMU

  // To decoder, let decoder handle the interrupt
  val intCtrl = Output(new INTCtrl)
  val clintIn = Input(new CLINTCSR)

  val decodePrivCheck = new DecodePrivCheck
  val meip = Input(Bool())
}

class CSRFile extends Module {
  val io = IO(new CSRIO)
  val M = "b11".U
  val S = "b01".U
  val U = "b00".U

  val accessCSRPriv = io.commitCSR.csrAddr(9, 8)
  //             IF the instruction is CSRRW / CSRRWI               else
  val csrRen = io.commitCSR.instValid && ((io.commitCSR.csrOp === CSR_W && io.commitCSR.instRd =/= 0.U) || (io.commitCSR.csrOp =/= CSR_X && io.commitCSR.csrOp =/= CSR_W && io.commitCSR.csrOp =/= CSR_I)) && ~io.commitCSR.exceptionInfo.valid
  val csrWen = io.commitCSR.instValid && !(io.commitCSR.csrOp === CSR_X || io.commitCSR.csrOp === CSR_I || 
    ((io.commitCSR.csrOp === CSR_S || io.commitCSR.csrOp === CSR_C) && io.commitCSR.instRs === 0.U) ||
    ((io.commitCSR.csrOp === CSR_SI || io.commitCSR.csrOp === CSR_CI) && io.commitCSR.csrWData === 0.U)) && ~io.commitCSR.exceptionInfo.valid


  def maskedWrite(oldValue: UInt, writeValue: UInt, mask: UInt) = {
    (oldValue & (~mask).asUInt()) | (writeValue & mask)
  }

  val sstatus_read_mask = WireInit(0.U(64.W)).asTypeOf(new mstatus)
  sstatus_read_mask.SIE := true.B
  sstatus_read_mask.SPIE := true.B
  sstatus_read_mask.SPP := true.B
  sstatus_read_mask.FS := "b11".U
  sstatus_read_mask.XS := "b11".U
  sstatus_read_mask.SUM := true.B
  sstatus_read_mask.MXR := true.B
  sstatus_read_mask.SPIE := true.B
  sstatus_read_mask.UXL := "b11".U
  sstatus_read_mask.SD := true.B
  val sstatus_write_mask = WireInit(0.U(64.W)).asTypeOf(new mstatus)
  sstatus_write_mask.SIE := true.B
  sstatus_write_mask.SPIE := true.B
  sstatus_write_mask.SPP := true.B
  sstatus_write_mask.FS := "b11".U
  sstatus_write_mask.SUM := true.B
  sstatus_write_mask.MXR := true.B
  val mstatus_write_mask = WireInit(0.U(64.W))
  mstatus_write_mask := "hffffffffffffffff".U
  mstatus_write_mask.asTypeOf(new mstatus).FS := 0.U


  val privMode = RegInit(M)
  BoringUtils.addSource(privMode, "difftestMode")

  // Hardwired Registers
  val misa_extension = "IMASUC" // Add C to cheat the Difftest
  val extension_val = misa_extension.map(e => 1 << (e - 'A')).reduce(_ | _).asUInt()
  val misa = WireInit(UInt(64.W), Cat(2.U(2.W), 0.U(62.W)) | extension_val)
  val mvendorid = WireInit(UInt(32.W), 0.U)
  val marchid = WireInit(UInt(64.W), 0.U)
  val mimpid = WireInit(UInt(64.W), 0.U)
  val mhartid = WireInit(UInt(64.W), 0.U)
  // Configurable Registers

  // mtvec
  val reset_mtvec = WireInit(0.U.asTypeOf(new mtvec_t))
  reset_mtvec.BASE := 0.U
  reset_mtvec.MODE := 0.U
  val mtvec = RegInit(reset_mtvec.asUInt())

  // mdeleg and mideleg
  val medeleg = RegInit(UInt(64.W), 0.U) // Machine Exception Delegation Register
  val mideleg = RegInit(UInt(64.W), 0.U) // Machine Interrupt Delegation Register
  // val medelgAndMask = 1.U << 11 // medelg[11] is hardwired to zero
  val midelegMask = WireInit(UInt(64.W),
    ((1.U << IntNo.STI) | (1.U << IntNo.SEI) | (1.U << IntNo.SSI))
  )
  val sieMask = WireInit(UInt(64.W),
    ((1.U << IntNo.STI) | (1.U << IntNo.SEI) | (1.U << IntNo.SSI)) & mideleg
  )
  // Spec P77, SEIP , STIP is read-only, and SSIP is writable
  // val sipMask = WireInit(UInt(64.W),
  //   (1.U << IntNo.SSI) | mideleg
  // )
  val sipMask = WireInit(UInt(64.W),0.U)  // SIP is unwritable

  val mcounteren = RegInit(UInt(64.W), 0.U)
  val mcause = RegInit(UInt(64.W), 0.U)
  BoringUtils.addSource(mcause, "difftestMcause")
  val mtval = RegInit(UInt(64.W), 0.U)
  val mepc = RegInit(UInt(64.W), 0.U)
  BoringUtils.addSource(mepc, "difftestMepc")
  val mie = RegInit(UInt(64.W), 0.U)
  val mip = RegInit(UInt(64.W), 0.U)
  val mstatus = RegInit(UInt(64.W), 0x1800.U)
  BoringUtils.addSource(mstatus, "difftestMstatus")
  val mscratch = RegInit(UInt(64.W), 0.U)

  val pmpcfg0 = RegInit(UInt(64.W), 0.U)
  val pmpcfg1 = RegInit(UInt(64.W), 0.U)
  val pmpcfg2 = RegInit(UInt(64.W), 0.U)
  val pmpcfg3 = RegInit(UInt(64.W), 0.U)
  val pmpaddr0 = RegInit(UInt(64.W), 0.U)
  val pmpaddr1 = RegInit(UInt(64.W), 0.U)
  val pmpaddr2 = RegInit(UInt(64.W), 0.U)
  val pmpaddr3 = RegInit(UInt(64.W), 0.U)
  val stvec = RegInit(UInt(64.W), 0.U)
  val scounteren = RegInit(UInt(64.W), 0.U)
  val csrRdAddr = Wire(UInt(12.W))
  val sscratch = RegInit(UInt(64.W), 0.U)
  val sepc = RegInit(UInt(64.W), 0.U)
  BoringUtils.addSource(sepc, "difftestSepc")
  val scause = RegInit(UInt(64.W), 0.U)
  BoringUtils.addSource(scause, "difftestScause")
  val stval = RegInit(UInt(64.W), 0.U)
  val satp = RegInit(UInt(64.W), 0.U)

  csrRdAddr := io.commitCSR.csrAddr
  val csrMapping = Array(
    CSRAddr.mvendorid -> mvendorid,
    //    // Machine Information Registers
    CSRAddr.marchid -> marchid,
    CSRAddr.mimpid -> mimpid,
    CSRAddr.mhartid -> mhartid,
    //    // Machine Trap Setup
    CSRAddr.mstatus -> mstatus,
    CSRAddr.misa -> misa,
    CSRAddr.medeleg -> medeleg,
    CSRAddr.mideleg -> mideleg,
    CSRAddr.mie -> mie,
    CSRAddr.mtvec -> mtvec,
    CSRAddr.mcounteren -> mcounteren,
    //Machine Trap Handling
    CSRAddr.mscratch -> mscratch,
    CSRAddr.mepc -> mepc,
    CSRAddr.mcause -> mcause,
    CSRAddr.mtval -> mtval,
    CSRAddr.mip -> mip,
    //CSRAddr.mtinst      ->   mtinst     ,
//    CSRAddr.mtval -> mtval,
    // Supervisor
    CSRAddr.sstatus -> mstatus, // To be handled soon
    CSRAddr.sie -> mie, // To be handled soon
    CSRAddr.sip -> mip, // To be handled soon
    CSRAddr.stvec -> stvec,
    CSRAddr.scounteren -> scounteren,
    CSRAddr.sscratch -> sscratch,
    CSRAddr.sepc -> sepc,
    CSRAddr.scause -> scause,
    CSRAddr.stval -> stval,
    CSRAddr.satp -> satp,
    // PMP
    CSRAddr.pmpcfg0 -> pmpcfg0,
    CSRAddr.pmpcfg1 -> pmpcfg1,
    CSRAddr.pmpcfg2 -> pmpcfg2,
    CSRAddr.pmpcfg3 -> pmpcfg3,
    CSRAddr.pmpaddr0 -> pmpaddr0,
    CSRAddr.pmpaddr1 -> pmpaddr1,
    CSRAddr.pmpaddr2 -> pmpaddr2,
    CSRAddr.pmpaddr3 -> pmpaddr3
  )
  // dontTouch(mip_o)
  // CSR Read Data
  val csrRdata = MuxLookup(io.commitCSR.csrAddr, 0.U, csrMapping)
  io.commitCSR.csrRdata := csrRdata
  // Some S mode registers read is masked
  when(csrRdAddr === CSRAddr.sstatus) {
    io.commitCSR.csrRdata := csrRdata & sstatus_read_mask.asUInt()
  }.elsewhen(csrRdAddr === CSRAddr.sie) {
    io.commitCSR.csrRdata := csrRdata & mideleg
  }.elsewhen(csrRdAddr === CSRAddr.sip) {
    io.commitCSR.csrRdata := csrRdata & mideleg
  }
  BoringUtils.addSource(mstatus & sstatus_read_mask.asUInt(), "difftestSstatus")

  val WrMaskedCSR = Map(
    CSRAddr.mstatus -> mstatus_write_mask,
    CSRAddr.mideleg -> midelegMask, // SSIP, SEIP, STIP
    CSRAddr.mie -> ((1.U << 1) | (1.U << 3) | (1.U << 5) | (1.U << 7) | (1.U << 9) | (1.U << 11)),
    CSRAddr.mip -> "h77f".U,
    CSRAddr.medeleg -> 0xbbff.U,
    CSRAddr.sstatus -> sstatus_write_mask.asUInt(),
    CSRAddr.sie -> sieMask,
    CSRAddr.sip -> sipMask
    // CSRAddr.stvec -> (~(1.U(64.W) << 1)).asUInt(),
    // CSRAddr.sepc -> (~1.U(64.W)).asUInt(),

  )
  val sideEffectCSR = Map( // Address: Int -> (Initial Value: UInt, Write Value: UInt) => Return Value: UInt
    CSRAddr.mstatus -> { oldValue: UInt => Cat(oldValue.asTypeOf(new mstatus).FS === "b11".U, oldValue(62, 0)) },
    CSRAddr.sstatus -> { oldValue: UInt => Cat(oldValue.asTypeOf(new mstatus).FS === "b11".U, oldValue(62, 0)) },
    CSRAddr.satp -> { oldValue: UInt => Mux(oldValue(63, 60) === 8.U, oldValue, Cat(0.U(4.W), oldValue(59, 0))) } // We only support SV39 and Bare
  )
  // Generate CSR Write Enable Signals for EXISTING & WRITABLE CSRs
  val isCsr_S = io.commitCSR.csrOp === CSR_S || io.commitCSR.csrOp === CSR_SI
  val isCsr_C = io.commitCSR.csrOp === CSR_C || io.commitCSR.csrOp === CSR_CI
  val isCsr_W = io.commitCSR.csrOp === CSR_W
  // If write to CSR, should consider whether the address is legal
  // Writing to a read-only CSR will cause an illegal instruction exception, or writing to an unimplemented CSR
  val CSRExists = csrMapping.map(kv => io.commitCSR.csrAddr === kv._1).reduce(_ | _).asBool()
  val ReadOnlyCSR = io.commitCSR.csrAddr(11, 10) === "b11".U
  val CSRFalsePriv = accessCSRPriv > privMode
  // Legal stands for CSR exists and the priv is right
  val CSRAddrLegal = CSRExists & !CSRFalsePriv
  //                      Address is legal  |                           is in S-Mode but TVM is enabled
  val writeIllegalCSR = csrWen & (!CSRAddrLegal |
    (mstatus.asTypeOf(new mstatus).TVM && privMode === S && io.commitCSR.csrAddr === CSRAddr.satp) |
    ReadOnlyCSR)
  val readIllegalCSR = csrRen & (!CSRAddrLegal |
    (mstatus.asTypeOf(new mstatus).TVM && privMode === S && io.commitCSR.csrAddr === CSRAddr.satp))
  csrMapping.map(kv =>
    when(io.commitCSR.csrAddr === kv._1 && csrWen && CSRAddrLegal) { // We have no need to consider whether the address is legal
      // Since we only generate the logic for legal writing
      val newValMasked = WireInit(io.commitCSR.csrWData)
      val updateVal = (Mux(isCsr_S | isCsr_C, csrRdata, 0.U) | io.commitCSR.csrWData) & (~Mux(isCsr_C, io.commitCSR.csrWData, 0.U)).asUInt()
      if (WrMaskedCSR.contains(kv._1)) { // CSR Write is Masked ???
        newValMasked := maskedWrite(kv._2, updateVal, WrMaskedCSR(kv._1))
      }
      val newValWithSideEffect = WireInit(newValMasked)
      if (sideEffectCSR.contains(kv._1)) { // Have Side Effect ?
        newValWithSideEffect := sideEffectCSR(kv._1)(newValMasked)
      }
      kv._2 := newValWithSideEffect
    }
  )
  // Handle the MIP & SIP Case
  val updateValMip = (Mux(isCsr_S | isCsr_C, csrRdata, 0.U) | io.commitCSR.csrWData) & (~Mux(isCsr_C, io.commitCSR.csrWData, 0.U)).asUInt()
  when( csrWen && io.commitCSR.csrAddr === CSRAddr.mip ) {
    mip := maskedWrite(mip, updateValMip, WrMaskedCSR(CSRAddr.mip)) & ~(1.U << IntNo.MSI | 1.U << IntNo.MTI | 1.U << IntNo.MEI ) | ((io.clintIn.msip << IntNo.MSI) | (io.clintIn.mtip << IntNo.MTI) | (io.meip << IntNo.MEI))
  }.otherwise {
    mip := mip & ~(1.U << IntNo.MSI | 1.U << IntNo.MTI | 1.U << IntNo.MEI) | ((io.clintIn.msip << IntNo.MSI) | (io.clintIn.mtip << IntNo.MTI) | (io.meip << IntNo.MEI) )
  }
  // Illegal Instruction
  val raiseIllegalInstructionException = writeIllegalCSR | readIllegalCSR
  io.illegalInst := raiseIllegalInstructionException

  // ================== Exception Handler Begins ===================
  // Trap to which mode?
  val nextPrivLevel = WireInit(M)
  // if delegated, trap to S mode
  when(io.commitCSR.exceptionInfo.cause(63) && mideleg(io.commitCSR.exceptionInfo.cause) // is interrupt
    || !io.commitCSR.exceptionInfo.cause(63) && medeleg(io.commitCSR.exceptionInfo.cause)) { // is exception
    nextPrivLevel := Mux(privMode === M, M, S) // if is in S or U mode, will not trap to M mode
  }
  // Update the registers
  val mstatus_new = WireInit(0.U(64.W)).asTypeOf(new mstatus)
  mstatus_new := mstatus.asTypeOf(new mstatus)
  when(io.commitCSR.exceptionInfo.valid && io.commitCSR.instValid) {
    privMode := nextPrivLevel // Update the priv mode
    when(nextPrivLevel === M) { // Will trap to M Mode
      mstatus_new.MIE := false.B
      mstatus_new.MPIE := mstatus.asTypeOf(new mstatus).MIE
      mstatus_new.MPP := privMode
      mstatus := mstatus_new.asUInt()
      mcause := io.commitCSR.exceptionInfo.cause
      mepc := io.commitCSR.exceptionInfo.epc
      mtval := Mux(io.commitCSR.exceptionInfo.cause(63) ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.illegalInstr.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.breakPoint.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.ecallM.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.ecallS.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.ecallU.U, 0.U, io.commitCSR.exceptionInfo.tval)
    }.elsewhen(nextPrivLevel === S) { // Will trap to S Mode
      mstatus_new.SIE := false.B
      mstatus_new.SPIE := mstatus.asTypeOf(new mstatus).SIE
      mstatus_new.SPP := privMode(0)
      mstatus := mstatus_new.asUInt()
      scause := io.commitCSR.exceptionInfo.cause
      sepc := io.commitCSR.exceptionInfo.epc
      stval := Mux(io.commitCSR.exceptionInfo.cause(63) ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.illegalInstr.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.breakPoint.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.ecallM.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.ecallS.U ||
        io.commitCSR.exceptionInfo.cause === ExceptionNo.ecallU.U, 0.U, io.commitCSR.exceptionInfo.tval)
    }
  }
  // ================== Exception Handler Ends ===================

  val isMret = io.commitCSR.inst === "b00110000001000000000000001110011".U & io.commitCSR.instValid & ~io.commitCSR.exceptionInfo.valid
  val isSret = io.commitCSR.inst === "b00010000001000000000000001110011".U & io.commitCSR.instValid & ~io.commitCSR.exceptionInfo.valid
  val isSFence = io.commitCSR.inst === BitPat("b0001001??????????000000001110011") & io.commitCSR.instValid & ~io.commitCSR.exceptionInfo.valid
  val isEret = isMret | isSret
  // ================== ERET Handler Begins ===================
  /*
  When executing an xRET instruction,
  supposing xPP holds the value y,
  xIE is set to xPIE;
  the privilege mode is changed to y;
  xPIE is set to 1; and xPP is set to U
   */
  val epc = WireInit(0.U)
  when(isMret) {
    mstatus_new.MIE := mstatus.asTypeOf(new mstatus).MPIE
    mstatus_new.MPP := U
    mstatus_new.MPIE := true.B
    mstatus := mstatus_new.asUInt()
    privMode := mstatus.asTypeOf(new mstatus).MPP
    epc := mepc
  }.elsewhen(isSret) {
    mstatus_new.SIE := mstatus.asTypeOf(new mstatus).SPIE
    mstatus_new.SPP := 0.U
    mstatus_new.SPIE := true.B
    mstatus := mstatus_new.asUInt()
    privMode := Cat(0.U(1.W), mstatus.asTypeOf(new mstatus).SPP)
    epc := sepc
  }
  // ================== ERET Handler Ends ===================

  // ================== Exception Handler Entry Begins ===================
  val handlerBase = Wire(UInt(64.W))
  handlerBase := Cat(mtvec(63, 2), 0.U(2.W))
  when(nextPrivLevel === S) {
    handlerBase := Cat(stvec(63, 2), 0.U(2.W))
  }
  val handlerEntry = WireInit(handlerBase)
  when((mtvec(0) || stvec(0)) && io.commitCSR.exceptionInfo.cause(63)) { // Async exceptions are vectorized
    handlerEntry := handlerBase + (io.commitCSR.exceptionInfo.cause(62, 0) << 2)
  }

  // ================== Exception Handler Entry Ends ===================

  // Decide whether to enable the Sv39
  io.csrMMU.enableSv39 := (privMode =/= M && satp(63, 60) === 8.U)
  io.csrMMU.asid := satp(59,44)
  io.csrMMU.mxr := mstatus.asTypeOf(new mstatus).MXR
  io.csrMMU.sum := mstatus.asTypeOf(new mstatus).SUM
  io.csrMMU.tw := mstatus.asTypeOf(new mstatus).TW
  io.csrMMU.tsr := mstatus.asTypeOf(new mstatus).TSR
  io.csrMMU.tvm := mstatus.asTypeOf(new mstatus).TVM
  io.csrMMU.satpPPN := satp(43,0)

  // Apply MPRV Rules
  when(mstatus.asTypeOf(new mstatus).MPRV && satp(63, 60) === 8.U && (mstatus.asTypeOf(new mstatus).MPP =/= M)) {
    io.csrMMU.enableLSVM := true.B
  }.otherwise {
    io.csrMMU.enableLSVM := io.csrMMU.enableSv39
  }

  // To Decoder
  io.intCtrl.mie := mie
  io.intCtrl.mip := mip
  io.intCtrl.sie := mstatus.asTypeOf(new mstatus).SIE
  io.intCtrl.mideleg := mideleg
  io.intCtrl.privMode := privMode
  //  The Machine mode interrupt is globally enabled iff 1. In M mode, see Mstatus.MIE | 2. Not in M mode, enabled
  // If we are in M mode, we should should consider the MIE, but if we are not in M mode, interrupts are considered to
  // be globally enabled
  io.intCtrl.intGlobalEnable := (mstatus.asTypeOf(new mstatus).MIE & privMode === M) | (privMode =/= M)

  // Redir
  val needFlush = io.commitCSR.instValid && (isSFence || csrWen)
  io.ifRedir.redir := false.B
  io.ifRedir.redirPC := epc
  when(isEret && io.commitCSR.instValid) {
    io.ifRedir.redir := true.B
    io.ifRedir.redirPC := epc
  }.elsewhen(io.commitCSR.exceptionInfo.valid  && io.commitCSR.instValid ) {
    io.ifRedir.redir := true.B
    io.ifRedir.redirPC := handlerEntry
  }.elsewhen( needFlush ) {
    io.ifRedir.redir := true.B
    io.ifRedir.redirPC := io.commitCSR.instPC + 4.U
  }

  // Check priv in decode stage
  val decodeAccessCSRPriv = io.decodePrivCheck.csrAddr(9, 8)
  //             IF the instruction is CSRRW / CSRRWI               else
  val decodeCSRRen = ((io.decodePrivCheck.csrOp === CSR_W && io.decodePrivCheck.instRd =/= 0.U) || (io.decodePrivCheck.csrOp =/= CSR_X && io.decodePrivCheck.csrOp =/= CSR_W && io.decodePrivCheck.csrOp =/= CSR_I))
  val decodeCSRWen = !(io.decodePrivCheck.csrOp === CSR_X || io.decodePrivCheck.csrOp === CSR_I || 
    ((io.decodePrivCheck.csrOp === CSR_S ||  io.decodePrivCheck.csrOp === CSR_C) &&  io.decodePrivCheck.instRs === 0.U) ||
    ((io.decodePrivCheck.csrOp === CSR_SI || io.decodePrivCheck.csrOp === CSR_CI) && io.decodePrivCheck.instImm === 0.U))
  val decodeCSRExists = csrMapping.map(kv => io.decodePrivCheck.csrAddr === kv._1).reduce(_ | _).asBool()
  val decodeReadOnlyCSR = io.decodePrivCheck.csrAddr(11, 10) === "b11".U
  val decodeCSRFalsePriv = decodeAccessCSRPriv > privMode
  // Legal stands for CSR exists and the priv is right
  val decodeCSRAddrLegal = decodeCSRExists & !decodeCSRFalsePriv
  val decodeWriteIllegalCSR = decodeCSRWen & (!decodeCSRAddrLegal |
    (mstatus.asTypeOf(new mstatus).TVM && privMode === S && io.decodePrivCheck.csrAddr === CSRAddr.satp) |
    decodeReadOnlyCSR)
  val decodeReadIllegalCSR = decodeCSRRen & (!decodeCSRAddrLegal |
    (mstatus.asTypeOf(new mstatus).TVM && privMode === S && io.decodePrivCheck.csrAddr === CSRAddr.satp))

  io.decodePrivCheck.illegalInst := (decodeWriteIllegalCSR | decodeReadIllegalCSR)

}

object CSRFile extends App {
  val stage = new ChiselStage
  stage.emitVerilog(new CSRFile)
}
