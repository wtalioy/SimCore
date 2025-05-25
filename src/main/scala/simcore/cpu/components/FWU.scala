package simcore.cpu.components

import chisel3._
import chisel3.util._
import simcore.cpu.utils.ForwardingSelects

/**
 * Forwarding Unit
 * Handles data forwarding from later pipeline stages to earlier ones
 * to resolve data hazards
 */
class ForwardingUnit(addrBits: Int) extends Module {
  val io = IO(new Bundle {
    // Register addresses
    val rs1_addr = Input(UInt(addrBits.W))
    val rs2_addr = Input(UInt(addrBits.W))
    val ex_mem_rd_addr = Input(UInt(addrBits.W))
    val mem_wb_rd_addr = Input(UInt(addrBits.W))
    
    // Register usage flags
    val uses_rs1 = Input(Bool())
    val uses_rs2 = Input(Bool())
    
    // Control signals
    val ex_mem_reg_write = Input(Bool())
    val mem_wb_reg_write = Input(Bool())
    
    // Forwarding select signals
    val forward_rs1_sel = Output(UInt(2.W))
    val forward_rs2_sel = Output(UInt(2.W))
  })
  
  // Default to no forwarding
  io.forward_rs1_sel := ForwardingSelects.NO_FORWARD
  io.forward_rs2_sel := ForwardingSelects.NO_FORWARD
  
  // RS1 forwarding logic
  when(io.uses_rs1 && io.rs1_addr =/= 0.U) {
    // Check for forwarding from EX/MEM stage (higher priority)
    when(io.ex_mem_reg_write && (io.ex_mem_rd_addr === io.rs1_addr)) {
      io.forward_rs1_sel := ForwardingSelects.FORWARD_FROM_MEM
    }
    // Check for forwarding from MEM/WB stage
    .elsewhen(io.mem_wb_reg_write && (io.mem_wb_rd_addr === io.rs1_addr)) {
      io.forward_rs1_sel := ForwardingSelects.FORWARD_FROM_WB
    }
  }
  
  // RS2 forwarding logic
  when(io.uses_rs2 && io.rs2_addr =/= 0.U) {
    // Check for forwarding from EX/MEM stage (higher priority)
    when(io.ex_mem_reg_write && (io.ex_mem_rd_addr === io.rs2_addr)) {
      io.forward_rs2_sel := ForwardingSelects.FORWARD_FROM_MEM
    }
    // Check for forwarding from MEM/WB stage
    .elsewhen(io.mem_wb_reg_write && (io.mem_wb_rd_addr === io.rs2_addr)) {
      io.forward_rs2_sel := ForwardingSelects.FORWARD_FROM_WB
    }
  }
} 