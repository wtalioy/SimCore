package SimCore.cpu.components

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.DBusBundle // Import DBusBundle

// Define DBus request and response bundles if not globally defined
// class DBusReq extends Bundle { /* val addr, wdata, wen, etc. */ }
// class DBusResp extends Bundle { /* val rdata, error, etc. */ }

class MemUnit extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W)) // Address from ALU
    val wdata = Input(UInt(32.W)) // Data to write for stores
    val is_load = Input(Bool())
    val is_store = Input(Bool())
    // val load_type = Input(UInt(2.W)) // e.g. LB, LH, LW
    // val store_type= Input(UInt(2.W)) // e.g. SB, SH, SW

    val rdata = Output(UInt(32.W)) // Data read from memory

    // Simplified DBus interface (outputs from MemUnit, inputs to MemUnit)
    val dbus_req_ready = Output(Bool())
    val dbus_req_addr = Output(UInt(32.W))
    val dbus_req_wdata = Output(UInt(32.W))
    val dbus_req_wen = Output(Bool()) // Write enable for store

    val dbus_resp_valid =
      Input(Bool()) // Memory system signals data is ready/ack
    val dbus_resp_rdata = Input(UInt(32.W))

    // val stall_out = Output(Bool()) // Stall if DMem is not ready
  })

  // Default assignments
  io.dbus_req_ready := false.B
  io.dbus_req_addr := io.addr
  io.dbus_req_wdata := io.wdata
  io.dbus_req_wen := false.B
  io.rdata := io.dbus_resp_rdata // Pass through response data

  // io.stall_out := (io.is_load || io.is_store) && !io.dbus_resp_valid // Basic stall condition

  when(io.is_load) {
    io.dbus_req_ready := true.B
    io.dbus_req_wen := false.B
    // rdata is assigned from dbus_resp_rdata
    // Add logic for sign/zero extension based on load_type (LB, LH, LBU, LHU) if implementing
  }.elsewhen(io.is_store) {
    io.dbus_req_ready := true.B
    io.dbus_req_wen := true.B
    // Add logic for byte/half-word enables if implementing SB, SH
  }

  // For now, assume memory operations complete in one cycle if dbus_resp_valid is high
  // A more complex memory system might require a state machine here.
}
