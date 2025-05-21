package SimCore.cpu.utils

import chisel3._
import chisel3.util._

// This is a placeholder. The Arbiter's specifics depend heavily on what it's arbitrating.
// If it's for multiple execution units writing back to the register file:

class WriteArbiter extends Module {
  val io = IO(new Bundle {
    // Example: Arbitrating between two execution results for reg file write
    val in1_valid = Input(Bool())
    val in1_rd_addr = Input(UInt(5.W))
    val in1_rd_data = Input(UInt(32.W))

    val in2_valid = Input(Bool())
    val in2_rd_addr = Input(UInt(5.W))
    val in2_rd_data = Input(UInt(32.W))

    // To RegFile Write Port
    val out_wen = Output(Bool())
    val out_rd_addr = Output(UInt(5.W))
    val out_rd_data = Output(UInt(32.W))
  })

  // Simple priority arbiter (in1 has priority)
  when(io.in1_valid) {
    io.out_wen := true.B
    io.out_rd_addr := io.in1_rd_addr
    io.out_rd_data := io.in1_rd_data
  }.elsewhen(io.in2_valid) {
    io.out_wen := true.B
    io.out_rd_addr := io.in2_rd_addr
    io.out_rd_data := io.in2_rd_data
  }.otherwise {
    io.out_wen := false.B
    io.out_rd_addr := 0.U // Default, shouldn't matter if wen is false
    io.out_rd_data := 0.U
  }
}

// If it's for arbitrating dbus access between IFU and MemUnit:
// class BusArbiter extends Module { /* ... */ } 