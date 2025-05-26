package simcore.cpu.stages

import chisel3._
import chisel3.util._
import simcore.cpu.components.ALUUnit
import simcore.cpu.components.BrUnit
import simcore.cpu.utils.ALUOps
import simcore.cpu.utils.BranchTypes
import simcore.cpu.utils.ControlBundle
import simcore.cpu.Config
import simcore.cpu.utils.ForwardingSelects

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
      val ctrl = Input(new ControlBundle())
      val valid = Input(Bool())
      val branch_forward_rs1_sel = Input(UInt(2.W))
      val branch_forward_rs2_sel = Input(UInt(2.W))
      val forward_mem_result = Input(UInt(XLEN.W))
      val forward_wb_result = Input(UInt(XLEN.W))
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
  // Output connections
  // ==========================================================================

  // Set outputs
  io.out.result := aluUnit.io.result
  io.out.rd_addr := io.in.rd_addr
  io.out.valid := io.in.valid

  // Branch control signals
  io.branch_taken := branchJumpTaken
  io.branch_target := brUnit.io.branch_target
}
