package simcore.cpu.components

import chisel3._
import chisel3.util._
import simcore.cpu.utils.constants.ForwardingSelects

/**
 * Enhanced Forwarding Unit
 * Handles data forwarding from later pipeline stages to earlier ones
 * to resolve data hazards, including more sophisticated forwarding paths
 */
class ForwardingUnit(addrBits: Int) extends Module {
  val io = IO(new Bundle {
    // Register addresses
    val rs1_addr = Input(UInt(addrBits.W))
    val rs2_addr = Input(UInt(addrBits.W))
    val ex_rd_addr = Input(UInt(addrBits.W))          // Current EX instruction rd
    val ex_mem_rd_addr = Input(UInt(addrBits.W))      // EX/MEM pipeline register rd
    val mem_wb_rd_addr = Input(UInt(addrBits.W))      // MEM/WB pipeline register rd
    
    // Register usage flags
    val uses_rs1 = Input(Bool())
    val uses_rs2 = Input(Bool())
    val is_branch = Input(Bool())                     // Current instruction is a branch
    
    // Control signals
    val ex_reg_write = Input(Bool())                  // Current EX instruction will write to register
    val ex_mem_reg_write = Input(Bool())              // EX/MEM instruction will write to register
    val mem_wb_reg_write = Input(Bool())              // MEM/WB instruction will write to register
    val ex_valid = Input(Bool())                      // Current EX instruction is valid
    
    // Forwarding select signals - now 3 bits wide to accommodate more options
    val forward_rs1_sel = Output(UInt(3.W))
    val forward_rs2_sel = Output(UInt(3.W))
    
    // Branch-specific forwarding signals
    val forward_branch_rs1_sel = Output(UInt(3.W))
    val forward_branch_rs2_sel = Output(UInt(3.W))
  })
  
  // Default to no forwarding
  io.forward_rs1_sel := ForwardingSelects.NO_FORWARD
  io.forward_rs2_sel := ForwardingSelects.NO_FORWARD
  io.forward_branch_rs1_sel := ForwardingSelects.NO_FORWARD
  io.forward_branch_rs2_sel := ForwardingSelects.NO_FORWARD
  
  // RS1 forwarding logic for main execution path (ALU)
  when(io.uses_rs1 && io.rs1_addr =/= 0.U) {
    // Check for forwarding from EX stage (highest priority) - allows for intra-stage forwarding
    when(io.ex_valid && io.ex_reg_write && (io.ex_rd_addr === io.rs1_addr)) {
      io.forward_rs1_sel := ForwardingSelects.FORWARD_FROM_EX
    }
    // Check for forwarding from EX/MEM stage
    .elsewhen(io.ex_mem_reg_write && (io.ex_mem_rd_addr === io.rs1_addr)) {
      io.forward_rs1_sel := ForwardingSelects.FORWARD_FROM_MEM
    }
    // Check for forwarding from MEM/WB stage
    .elsewhen(io.mem_wb_reg_write && (io.mem_wb_rd_addr === io.rs1_addr)) {
      io.forward_rs1_sel := ForwardingSelects.FORWARD_FROM_WB
    }
  }
  
  // RS2 forwarding logic for main execution path (ALU)
  when(io.uses_rs2 && io.rs2_addr =/= 0.U) {
    // Check for forwarding from EX stage (highest priority) - allows for intra-stage forwarding
    when(io.ex_valid && io.ex_reg_write && (io.ex_rd_addr === io.rs2_addr)) {
      io.forward_rs2_sel := ForwardingSelects.FORWARD_FROM_EX
    }
    // Check for forwarding from EX/MEM stage
    .elsewhen(io.ex_mem_reg_write && (io.ex_mem_rd_addr === io.rs2_addr)) {
      io.forward_rs2_sel := ForwardingSelects.FORWARD_FROM_MEM
    }
    // Check for forwarding from MEM/WB stage
    .elsewhen(io.mem_wb_reg_write && (io.mem_wb_rd_addr === io.rs2_addr)) {
      io.forward_rs2_sel := ForwardingSelects.FORWARD_FROM_WB
    }
  }
  
  // Specialized forwarding for branch instructions
  when(io.is_branch) {
    // RS1 forwarding for branch comparisons
    when(io.rs1_addr =/= 0.U) {
      when(io.ex_mem_reg_write && (io.ex_mem_rd_addr === io.rs1_addr)) {
        io.forward_branch_rs1_sel := ForwardingSelects.FORWARD_FROM_MEM
      }.elsewhen(io.mem_wb_reg_write && (io.mem_wb_rd_addr === io.rs1_addr)) {
        io.forward_branch_rs1_sel := ForwardingSelects.FORWARD_FROM_WB
      }
    }
    
    // RS2 forwarding for branch comparisons
    when(io.rs2_addr =/= 0.U) {
      when(io.ex_mem_reg_write && (io.ex_mem_rd_addr === io.rs2_addr)) {
        io.forward_branch_rs2_sel := ForwardingSelects.FORWARD_FROM_MEM
      }.elsewhen(io.mem_wb_reg_write && (io.mem_wb_rd_addr === io.rs2_addr)) {
        io.forward_branch_rs2_sel := ForwardingSelects.FORWARD_FROM_WB
      }
    }
  }
} 