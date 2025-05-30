package simcore.cpu.stages

import chisel3._
import chisel3.util._
import simcore.cpu.utils.interfaces.MEMWBIO
import simcore.cpu.Config

/** WriteBack Stage Unit
  * Handles register writeback operations.
  * This completes the 5-stage pipeline modularization (IF-ID-EX-MEM-WB).
  */
class WB extends Module with Config {
  val io = IO(new Bundle {
    // Input from MEM stage
    val in = Input(new MEMWBIO(XLEN, GPR_LEN))
    
    // Register writeback interface
    val rd_addr = Output(UInt(GPR_LEN.W))
    val rd_data = Output(UInt(XLEN.W))
    val rd_write_en = Output(Bool())
    
    // For debug/monitoring
    val pc = Output(UInt(XLEN.W))
    val valid = Output(Bool())
  })
  
  // Default values
  io.rd_addr := 0.U
  io.rd_data := 0.U
  io.rd_write_en := false.B
  io.pc := 0.U
  io.valid := false.B
  
  // Simple pass-through logic
  when(io.in.valid) {
    // Register file write connections
    io.rd_addr := io.in.rd_addr
    io.rd_data := io.in.result
    io.rd_write_en := io.in.ctrl.reg_write && io.in.valid
    
    // Debug/monitoring
    io.pc := io.in.pc
    io.valid := io.in.valid
  }
} 