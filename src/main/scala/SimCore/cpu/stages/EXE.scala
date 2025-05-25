package SimCore.cpu.stages

import chisel3._
import chisel3.util._
import SimCore.cpu.components.ALUUnit
import SimCore.cpu.components.BrUnit
import SimCore.cpu.utils.ALUOps
import SimCore.cpu.utils.BranchTypes
import SimCore.cpu.utils.ControlBundle
import SimCore.cpu.Config

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

  // Connect branch unit
  brUnit.io.rs1_data := io.in.rs1_data
  brUnit.io.rs2_data := io.in.rs2_data
  brUnit.io.branch_type := io.in.ctrl.branch_type
  brUnit.io.is_branch := io.in.ctrl.branch_type =/= BranchTypes.NONE
  brUnit.io.is_jump := io.in.ctrl.jump
  brUnit.io.imm := io.in.imm

  // Calculate branch/jump target
  val branchOffset = io.in.imm
  val branchTarget = io.in.pc + branchOffset

  // For jump instructions
  val jumpTarget = Mux(
    io.in.ctrl.alu_src1_sel === 0.U,
    io.in.rs1_data, // JR uses rs1
    io.in.imm // J/JAL uses immediate
  )

  // Final target selection
  val targetAddr = Mux(
    io.in.ctrl.jump,
    jumpTarget,
    branchTarget
  )

  // Branch/jump taken
  val branchJumpTaken =
    (brUnit.io.branch_taken || io.in.ctrl.jump) && io.in.valid

  // ==========================================================================
  // Output connections
  // ==========================================================================

  // Set outputs
  io.out.result := aluUnit.io.result
  io.out.rd_addr := io.in.rd_addr
  io.out.valid := io.in.valid

  // Branch control signals
  io.branch_taken := branchJumpTaken
  io.branch_target := targetAddr
}
