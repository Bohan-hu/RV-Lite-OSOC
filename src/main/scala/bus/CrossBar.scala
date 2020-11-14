// package bus
// import chisel3._
// import _root_.core.NaiveBusM2S
// class CrossBarMasterDeviceIO (id: Int) extends Bundle {
//   val masterPort = Flipped(new NaiveBusM2S)
//   val deviceID = id.U(2.W)
// }

// class CrossBarIO extends Bundle {
//   val imemMaster = Flipped(new NaiveBusM2S)
//   val dmemMaster = Flipped(new NaiveBusM2S)
//   val clintSlave = new NaiveBusM2S
//   val uartSlave = new NaiveBusM2S
//   val axiRamSlave = new AXIMaster     // Maybe route to the AXI Bridge
// }

// class CrossBar extends Module {
//   val io = IO(new CrossBarIO)
//   val imemReq = io.imemMaster
//   val dmemReq = io.dmemMaster
//   val imemRreq = io.imemMaster.memRreq
//   val dmemRreq = io.dmemMaster.memRreq
//   val dmemWen = io.dmemMaster.memWen
//   // ID: Instruction request: 0, Data request: 1
//   // To provide a unified interface, the slave should handle the transaction ID
//   // AXI just pass the ID to the top
//   val transactionID = Mux(dmemRreq | dmemWen, 1.U, 0.U)
//   // Determine route the response to which master
//   val respIsDmem = io.axiRamSlave.rid === 1.U || io.axiRamSlave.bid === 1.U || io.axiRamSlave.w
// //  when(respIsImem) {
// //
// //  }
//   val masterList = List()
//   val priority = List()

// }
