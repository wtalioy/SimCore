package simcore.cpu.components

import chisel3._
import chisel3.util._

/**
 * Pipeline Control Unit
 * Centralizes pipeline control signals like stalls and flushes
 */
class PipelineControlUnit extends Module {
  val io = IO(new Bundle {
    // Stall inputs
    val mem_stall = Input(Bool())
    val load_use_stall = Input(Bool())
    
    // Flush inputs
    val branch_taken = Input(Bool())
    
    // Control outputs
    val pipeline_stall = Output(Bool())
    val if_id_flush = Output(Bool())
    val id_ex_flush = Output(Bool())
    val ex_mem_flush = Output(Bool())
    val mem_wb_flush = Output(Bool())
  })
  
  // Compute pipeline stall signal - any stage stalling will stall the entire pipeline
  io.pipeline_stall := io.mem_stall || io.load_use_stall
  
  // Compute flush signals
  // When a branch is taken, we need to flush the pipeline stages that contain
  // instructions from the wrong path
  io.if_id_flush := io.branch_taken
  io.id_ex_flush := io.branch_taken
  
  // EX/MEM and MEM/WB typically not flushed on branches
  // Their contents are already executed or being executed
  io.ex_mem_flush := false.B
  io.mem_wb_flush := false.B
} 