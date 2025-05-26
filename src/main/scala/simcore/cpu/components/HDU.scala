package simcore.cpu.components

import chisel3._
import chisel3.util._

/**
 * Enhanced Hazard Detection Unit
 * Detects multiple types of hazards and generates appropriate stall signals
 */
class HazardDetectionUnit(addrBits: Int) extends Module {
  val io = IO(new Bundle {
    // ID stage register addresses and usage flags
    val id_rs1_addr = Input(UInt(addrBits.W))
    val id_rs2_addr = Input(UInt(addrBits.W))
    val id_uses_rs1 = Input(Bool())
    val id_uses_rs2 = Input(Bool())
    val id_is_branch = Input(Bool())
    val id_is_store = Input(Bool())
    val id_is_load = Input(Bool())
    
    // EX stage information
    val ex_rd_addr = Input(UInt(addrBits.W))
    val ex_is_load = Input(Bool())
    val ex_is_mul_div = Input(Bool())  // For long-latency operations
    
    // MEM stage information
    val mem_is_load = Input(Bool())
    val mem_rd_addr = Input(UInt(addrBits.W))
    
    // Structural hazard information
    val alu_busy = Input(Bool())       // ALU is busy with multi-cycle operation
    val mem_busy = Input(Bool())       // Memory unit is busy
    
    // Pipeline control outputs
    val load_use_stall = Output(Bool())
    val structural_stall = Output(Bool())
    val branch_stall = Output(Bool())
    val memory_order_stall = Output(Bool())
    
    // Combined stall signal for convenience
    val pipeline_stall = Output(Bool())
  })

  // 1. Load-Use Hazard: When a load instruction is in EX stage and the next instruction uses its destination register
  val rs1_load_use_hazard = io.id_uses_rs1 && io.ex_is_load && (io.ex_rd_addr === io.id_rs1_addr) && (io.ex_rd_addr =/= 0.U)
  val rs2_load_use_hazard = io.id_uses_rs2 && io.ex_is_load && (io.ex_rd_addr === io.id_rs2_addr) && (io.ex_rd_addr =/= 0.U)
  val load_use_hazard = rs1_load_use_hazard || rs2_load_use_hazard
  
  // 2. Load-Branch Hazard: Special case of load-use where the instruction after a load is a branch
  val load_branch_hazard = io.id_is_branch && io.ex_is_load && 
                          ((io.id_uses_rs1 && (io.ex_rd_addr === io.id_rs1_addr) && (io.ex_rd_addr =/= 0.U)) || 
                           (io.id_uses_rs2 && (io.ex_rd_addr === io.id_rs2_addr) && (io.ex_rd_addr =/= 0.U)))
  
  // 3. Memory Order Hazard: Load after Store to same address needs to be handled
  // This is a simplification - actual implementation would check addresses
  val memory_order_hazard = (io.id_is_load && io.ex_is_load) || (io.id_is_store && io.mem_is_load)
  
  // 4. Structural Hazards: Resource conflicts
  val alu_structural_hazard = io.ex_is_mul_div && io.alu_busy
  val mem_structural_hazard = (io.id_is_load || io.id_is_store) && io.mem_busy
  val structural_hazard = alu_structural_hazard || mem_structural_hazard
  
  // Output assignments
  io.load_use_stall := load_use_hazard
  io.branch_stall := load_branch_hazard
  io.memory_order_stall := memory_order_hazard
  io.structural_stall := structural_hazard
  
  // Combined stall signal
  io.pipeline_stall := load_use_hazard || load_branch_hazard || memory_order_hazard || structural_hazard
} 