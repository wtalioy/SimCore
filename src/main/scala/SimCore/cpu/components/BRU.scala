package SimCore.cpu.components

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.BranchTypes

// Define Branch types if not globally defined (from IDU ideally)
// object BranchTypes { ... } // Removed this definition

class BrUnit(dataBits: Int) extends Module {
  val io = IO(new Bundle {
    // val pc = Input(UInt(32.W)) // Current PC for target calculation
    val rs1_data = Input(UInt(dataBits.W))
    val rs2_data = Input(UInt(dataBits.W))
    val imm = Input(UInt(dataBits.W)) // Branch offset or JAL/JALR immediate
    val is_branch = Input(Bool()) // Control signal from IDU/EXEU
    val is_jump = Input(Bool()) // Control signal for Jumps (JAL/JALR)
    val branch_type =
      Input(UInt(3.W)) // Specific branch condition (BEQ, BNE, etc.)

    val branch_taken = Output(Bool())
    val branch_target = Output(UInt(dataBits.W)) // Target address if branch is taken
  })

  // Default outputs
  io.branch_taken := false.B
  // Target calculation for branches is PC + imm. For JALR it is rs1_data + imm.
  // For JAL it is PC + imm. This needs the PC from an earlier stage.
  // For now, let's assume imm is the *absolute* target for simplicity or that PC is added elsewhere.
  // A more realistic approach would be: val current_pc = RegNext(io.pc_in_from_earlier_stage)
  // io.branch_target := current_pc + io.imm  // For PC-relative branches/JAL
  // For JALR: io.branch_target := io.rs1_data + io.imm (LSB cleared)
  io.branch_target := io.imm // Simplified: Target is directly from imm for now or calculated by ALU

  // Branch condition logic
  val rs1_eq_rs2 = io.rs1_data === io.rs2_data
  val rs1_lt_rs2_signed = io.rs1_data.asSInt < io.rs2_data.asSInt
  val rs1_lt_rs2_unsigned = io.rs1_data < io.rs2_data

  when(io.is_branch) {
    switch(io.branch_type) {
      is(BranchTypes.BEQ) { io.branch_taken := rs1_eq_rs2 }
      is(BranchTypes.BNE) { io.branch_taken := !rs1_eq_rs2 }
      is(BranchTypes.BLT) { io.branch_taken := rs1_lt_rs2_signed }
      is(BranchTypes.BGE) { io.branch_taken := !rs1_lt_rs2_signed }
      is(BranchTypes.BLTU) { io.branch_taken := rs1_lt_rs2_unsigned }
      is(BranchTypes.BGEU) { io.branch_taken := !rs1_lt_rs2_unsigned }
    }
  }
  // Jumps are always "taken" if is_jump is true.
  // The target for JAL/JALR is often calculated in ALU (rs1 + imm for JALR, PC + imm for JAL)
  // If this BrUnit is *only* for conditional branches, then is_jump might not be an input here.
  // If io.is_jump is true, then io.branch_taken should be true.
  when(io.is_jump) {
    io.branch_taken := true.B
    // Target for JALR is often (rs1+imm) & ~1. Target for JAL is PC + imm.
    // If ALU handles this, then branch_target here might only be for conditional branches.
    // The EXEU muxing for redirect_target_out will choose.
  }

}
