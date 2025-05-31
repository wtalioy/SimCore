package simcore.cpu.components

import chisel3._
import chisel3.util._
import simcore.cpu.utils.constants.BranchTypes

class BrUnit(dataBits: Int) extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(dataBits.W)) // Current PC for target calculation
    val rs1_data = Input(UInt(dataBits.W))
    val rs2_data = Input(UInt(dataBits.W))
    val imm = Input(UInt(dataBits.W)) // Branch offset or JAL/JALR immediate
    val is_branch = Input(Bool()) // Control signal from IDU/EXEU
    val is_jump = Input(Bool()) // Control signal for Jumps (JAL/JALR)
    val is_jalr = Input(Bool()) // Specific signal for JALR (to handle its special address calculation)
    val branch_type = Input(UInt(4.W)) // Expanded to 4 bits to accommodate more branch types

    val branch_taken = Output(Bool())
    val branch_target = Output(UInt(dataBits.W)) // Target address if branch is taken
  })

  // Default outputs
  io.branch_taken := false.B

  // Branch/Jump target calculation
  // For branches and JAL: PC + imm
  // For JALR: (rs1_data + imm) & ~1 (clear LSB)
  val pcRelativeTarget = io.pc + io.imm
  val jalrTarget = (io.rs1_data + io.imm) & (~(1.U(dataBits.W)))
  
  io.branch_target := Mux(io.is_jalr, jalrTarget, pcRelativeTarget)

  // Branch condition logic - calculate all conditions once
  // Two-operand comparisons
  val rs1_eq_rs2 = io.rs1_data === io.rs2_data
  val rs1_lt_rs2_signed = io.rs1_data.asSInt < io.rs2_data.asSInt
  val rs1_lt_rs2_unsigned = io.rs1_data < io.rs2_data
  
  // Single-operand comparisons (with zero)
  val rs1_eq_zero = io.rs1_data === 0.U
  val rs1_lt_zero = io.rs1_data.asSInt < 0.S
  val rs1_gt_zero = io.rs1_data.asSInt > 0.S

  // Branch logic using lookup table for cleaner code
  val branchCondition = MuxLookup(io.branch_type, false.B)(
    Seq(
      // Two-operand branches
      BranchTypes.BEQ  -> rs1_eq_rs2,
      BranchTypes.BNE  -> !rs1_eq_rs2,
      BranchTypes.BLT  -> rs1_lt_rs2_signed,
      BranchTypes.BGE  -> !rs1_lt_rs2_signed,
      BranchTypes.BLTU -> rs1_lt_rs2_unsigned,
      BranchTypes.BGEU -> !rs1_lt_rs2_unsigned,
      
      // Single-operand branches (comparison with zero)
      BranchTypes.BGEZ -> !rs1_lt_zero,        // rs1 >= 0
      BranchTypes.BGTZ -> rs1_gt_zero,         // rs1 > 0
      BranchTypes.BLEZ -> (rs1_lt_zero || rs1_eq_zero), // rs1 <= 0
      BranchTypes.BLTZ -> rs1_lt_zero          // rs1 < 0
    )
  )

  // Set branch_taken output
  when(io.is_branch) {
    io.branch_taken := branchCondition
  }.elsewhen(io.is_jump) {
    io.branch_taken := true.B
  }
}
