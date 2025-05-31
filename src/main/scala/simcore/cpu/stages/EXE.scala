package simcore.cpu.stages

import chisel3._
import chisel3.util._
import simcore.cpu.components.{ALUUnit, BrUnit}
import simcore.cpu.utils.constants.{ALUOps, BPTypes, BranchTypes, ForwardingSelects}
import simcore.cpu.utils.interfaces.ControlIO
import simcore.cpu.Config

/** Execution Unit Handles ALU operations, memory access, and branch/jump
  * decisions
  */
class EXE extends Module with Config {
  val io = IO(new Bundle {
    // Input from ID stage
    val in = new Bundle {
      val pc = Input(UInt(XLEN.W))
      val rs1_data = Input(UInt(XLEN.W))
      val rs2_data = Input(UInt(XLEN.W))
      val rd_addr = Input(UInt(GPR_LEN.W))
      val imm = Input(UInt(XLEN.W))
      val ctrl = Input(new ControlIO())
      val valid = Input(Bool())
      val branch_forward_rs1_sel = Input(UInt(2.W))
      val branch_forward_rs2_sel = Input(UInt(2.W))
      val forward_mem_result = Input(UInt(XLEN.W))
      val forward_wb_result = Input(UInt(XLEN.W))
      val predicted_taken = Input(Bool())
      val predicted_target = Input(UInt(XLEN.W))
    }

    // Output to MEM stage
    val out = new Bundle {
      val result = Output(UInt(XLEN.W))
      val rd_addr = Output(UInt(GPR_LEN.W))
      val valid = Output(Bool())
    }

    // Branch control signals for fetch redirect
    val branch_taken = Output(Bool())
    val branch_target = Output(UInt(XLEN.W))
    
    // Branch prediction resolution signals
    val branch_resolved = Output(Bool())
    val branch_hit = Output(Bool())
    val branch_index = Output(UInt(log2Ceil(BTB_ENTRY_NUM).W))
    val branch_type = Output(UInt(2.W))
  })

  // Functional units
  val aluUnit = Module(new ALUUnit(XLEN))
  val brUnit = Module(new BrUnit(XLEN))

  // ==========================================================================
  // ALU Logic
  // ==========================================================================

  // ALU Source 1 selection
  val aluSrc1 = MuxLookup(io.in.ctrl.alu_src1_sel, io.in.rs1_data)(
    Seq(
      0.U -> io.in.rs1_data, // Register value
      1.U -> 0.U, // Zero
      2.U -> io.in.pc, // PC (for JAL)
      3.U -> io.in.pc // Reserved
    )
  )

  // ALU Source 2 selection
  val aluSrc2 = MuxLookup(io.in.ctrl.alu_src2_sel, io.in.rs2_data)(
    Seq(
      0.U -> io.in.rs2_data, // Register value
      1.U -> io.in.imm, // Immediate
      2.U -> 4.U, // Constant 4 (for JAL)
      3.U -> 0.U // Reserved
    )
  )

  // Connect ALU
  aluUnit.io.alu_op := io.in.ctrl.alu_op
  aluUnit.io.operand1 := aluSrc1
  aluUnit.io.operand2 := aluSrc2

  // ==========================================================================
  // Branch Logic
  // ==========================================================================

  // Detect JALR instruction (typically when io.in.ctrl.jump is true and alu_src1_sel is 0)
  val is_jalr = io.in.ctrl.jump && (io.in.ctrl.alu_src1_sel === 0.U)

  // Connect branch unit
  brUnit.io.pc := io.in.pc
  
  // Apply branch-specific forwarding if available
  val branchRs1Data = MuxLookup(io.in.branch_forward_rs1_sel, io.in.rs1_data)(
    Seq(
      ForwardingSelects.FORWARD_FROM_MEM -> io.in.forward_mem_result,
      ForwardingSelects.FORWARD_FROM_WB -> io.in.forward_wb_result
    )
  )
  
  val branchRs2Data = MuxLookup(io.in.branch_forward_rs2_sel, io.in.rs2_data)(
    Seq(
      ForwardingSelects.FORWARD_FROM_MEM -> io.in.forward_mem_result,
      ForwardingSelects.FORWARD_FROM_WB -> io.in.forward_wb_result
    )
  )
  
  brUnit.io.rs1_data := branchRs1Data
  brUnit.io.rs2_data := branchRs2Data
  brUnit.io.branch_type := io.in.ctrl.branch_type
  brUnit.io.is_branch := io.in.ctrl.branch_type =/= BranchTypes.NONE
  brUnit.io.is_jump := io.in.ctrl.jump
  brUnit.io.is_jalr := is_jalr
  brUnit.io.imm := io.in.imm

  // Branch/jump taken logic
  val branchJumpTaken = brUnit.io.branch_taken && io.in.valid
  
  // ==========================================================================
  // Branch Prediction Resolution
  // ==========================================================================
  
  // Determine branch type for branch predictor
  val bp_type = Wire(UInt(2.W))
  bp_type := Mux(io.in.ctrl.jump, BPTypes.jump, 
             Mux(io.in.ctrl.branch_type =/= BranchTypes.NONE, BPTypes.cond, 
                 BPTypes.dontcare))
  
  // Branch resolution logic
  val is_branch_or_jump = io.in.ctrl.branch_type =/= BranchTypes.NONE || io.in.ctrl.jump
  
  // Branch hit detection - for now we'll just use a simple dummy value
  // In a real implementation, this would be more sophisticated and track BTB hit information
  val branch_hit = io.in.predicted_taken || io.in.predicted_target === brUnit.io.branch_target
  
  // Branch index - for now we'll just use a dummy value
  // In a real implementation, this would track the BTB entry index that matched
  val branch_index = 0.U(log2Ceil(BTB_ENTRY_NUM).W)
  
  // ==========================================================================
  // Output connections
  // ==========================================================================

  // Set outputs
  io.out.result := aluUnit.io.result
  io.out.rd_addr := io.in.rd_addr
  io.out.valid := io.in.valid

  // Branch control signals
  io.branch_taken := branchJumpTaken
  io.branch_target := brUnit.io.branch_target
  
  // Branch prediction resolution signals
  io.branch_resolved := is_branch_or_jump && io.in.valid
  io.branch_hit := branch_hit
  io.branch_index := branch_index
  io.branch_type := bp_type
}
