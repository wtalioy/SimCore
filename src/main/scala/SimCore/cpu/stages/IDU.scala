package SimCore.cpu.stages

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.PCNextSelVals // Corrected import
import SimCore.cpu.utils.ALUOps        // Corrected import
import SimCore.cpu.utils.BranchTypes   // Corrected import
import SimCore.cpu.utils.IDUControlOutputs // Corrected import for bundle

// Assuming Uop is defined elsewhere or we define a simplified version here
// class Uop extends Bundle { /* ... */ }

// Define MIPS Opcodes and Funct codes (subset)
object MIPSOpcodes {
  // R-Type (opcode = 0)
  val OP_RTYPE = "b000000".U
  // I-Type
  val OP_ADDI  = "b001000".U
  val OP_ADDIU = "b001001".U
  val OP_SLTI  = "b001010".U
  val OP_SLTIU = "b001011".U
  val OP_ANDI  = "b001100".U
  val OP_ORI   = "b001101".U
  val OP_XORI  = "b001110".U
  val OP_LUI   = "b001111".U
  val OP_LW    = "b100011".U
  val OP_SW    = "b101011".U
  val OP_BEQ   = "b000100".U
  val OP_BNE   = "b000101".U
  // J-Type
  val OP_J     = "b000010".U
  val OP_JAL   = "b000011".U
}

object MIPSFuncts {
  // R-Type Funct codes
  val FN_ADD  = "b100000".U
  val FN_ADDU = "b100001".U
  val FN_SUB  = "b100010".U
  val FN_SUBU = "b100011".U
  val FN_AND  = "b100100".U
  val FN_OR   = "b100101".U
  val FN_XOR  = "b100110".U
  val FN_NOR  = "b100111".U
  val FN_SLT  = "b101010".U
  val FN_SLTU = "b101011".U
  val FN_SLL  = "b000000".U
  val FN_SRL  = "b000010".U
  val FN_SRA  = "b000011".U
  val FN_JR   = "b001000".U
  // SLL, SRL, SRA also need to check shamt or rs for 0 if it's NOP for SLL
}

class IDU extends Module {
  val io = IO(new Bundle {
    val valid_in = Input(Bool())
    val uop_instr_in = Input(UInt(32.W)) // Instruction from IFU
    val uop_pc_in = Input(UInt(32.W))    // PC from IFU, needed for JAL

    val valid_out = Output(Bool())
    // Decoded outputs - remaining direct outputs
    val uop_rs1_addr = Output(UInt(5.W))
    val uop_rs2_addr = Output(UInt(5.W))
    val uop_rd_addr = Output(UInt(5.W))
    val uop_imm = Output(UInt(32.W))
    val uop_pc_next_sel = Output(UInt(2.W)) // Control for PC update
    val uop_pc_out = Output(UInt(32.W))      // Pass through of the PC

    val ctrl = Output(new IDUControlOutputs()) // Grouped control signals
  })

  // Instruction field extraction
  val instr = io.uop_instr_in
  val opcode = instr(31, 26)
  val rs = instr(25, 21)
  val rt = instr(20, 16)
  val rd = instr(15, 11)
  val shamt = instr(10, 6)
  val funct = instr(5, 0)
  val imm16 = instr(15, 0)
  val imm16_signed = Wire(SInt(32.W))
  imm16_signed := imm16.asSInt // Sign-extended 16-bit immediate
  val imm16_unsigned = Wire(UInt(32.W))
  imm16_unsigned := imm16 // Zero-extended for logical ops
  val jump_addr = instr(25, 0)

  // Default control signal values (for those still direct IOs)
  io.uop_rs1_addr := 0.U
  io.uop_rs2_addr := 0.U
  io.uop_rd_addr := 0.U
  io.uop_imm := 0.U
  io.uop_pc_next_sel := PCNextSelVals.PC_PLUS_4 // Default PC + 4
  // Defaults for signals previously here but now in ctrl bundle are handled by IDUControlOutputs.NOP

  io.ctrl := IDUControlOutputs.NOP // Assign NOP to all control signals by default

  // Individual signal defaults that are part of io.ctrl for clarity during decode:
  val ctrl_alu_op            = WireDefault(ALUOps.ADD)
  val ctrl_alu_src2_is_imm   = WireDefault(false.B)
  val ctrl_is_branch         = WireDefault(false.B)
  val ctrl_branch_type       = WireDefault(BranchTypes.BEQ)
  val ctrl_is_jump           = WireDefault(false.B)
  val ctrl_is_jal            = WireDefault(false.B)
  val ctrl_jump_target_from_idu = WireDefault(0.U(32.W))
  val ctrl_is_load           = WireDefault(false.B)
  val ctrl_is_store          = WireDefault(false.B)
  val ctrl_reg_write_en      = WireDefault(false.B)
  val ctrl_mem_to_reg        = WireDefault(false.B)
  val ctrl_reads_rs          = WireDefault(false.B)
  val ctrl_reads_rt          = WireDefault(false.B)

  // Decoding logic
  when(io.valid_in) {
    switch(opcode) {
      is(MIPSOpcodes.OP_RTYPE) { // R-type instructions
        io.uop_rs1_addr := rs
        io.uop_rs2_addr := rt
        io.uop_rd_addr := rd
        ctrl_reads_rs := true.B
        ctrl_reads_rt := true.B
        ctrl_reg_write_en := true.B

        switch(funct) {
          is(MIPSFuncts.FN_ADD)  { ctrl_alu_op := ALUOps.ADD; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_ADDU) { ctrl_alu_op := ALUOps.ADD; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_SUB)  { ctrl_alu_op := ALUOps.SUB; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_SUBU) { ctrl_alu_op := ALUOps.SUB; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_AND)  { ctrl_alu_op := ALUOps.AND; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_OR)   { ctrl_alu_op := ALUOps.OR;  ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_XOR)  { ctrl_alu_op := ALUOps.XOR; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_NOR)  { ctrl_alu_op := ALUOps.NOR; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_SLT)  { ctrl_alu_op := ALUOps.SLT; ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_SLTU) { ctrl_alu_op := ALUOps.SLTU;ctrl_alu_src2_is_imm := false.B }
          is(MIPSFuncts.FN_SLL)  { ctrl_alu_op := ALUOps.SLL;  io.uop_rs1_addr := rt; io.uop_imm := shamt; ctrl_alu_src2_is_imm := true.B; ctrl_reads_rs := false.B; ctrl_reads_rt := true.B}
          is(MIPSFuncts.FN_SRL)  { ctrl_alu_op := ALUOps.SRL;  io.uop_rs1_addr := rt; io.uop_imm := shamt; ctrl_alu_src2_is_imm := true.B; ctrl_reads_rs := false.B; ctrl_reads_rt := true.B}
          is(MIPSFuncts.FN_SRA)  { ctrl_alu_op := ALUOps.SRA;  io.uop_rs1_addr := rt; io.uop_imm := shamt; ctrl_alu_src2_is_imm := true.B; ctrl_reads_rs := false.B; ctrl_reads_rt := true.B}
          is(MIPSFuncts.FN_JR)   {
            ctrl_is_jump := true.B
            io.uop_rs1_addr := rs 
            ctrl_reads_rs := true.B 
            ctrl_reads_rt := false.B
            ctrl_reg_write_en := false.B
            ctrl_alu_op := ALUOps.COPY_A 
            ctrl_alu_src2_is_imm := false.B 
            io.uop_pc_next_sel := PCNextSelVals.JUMP_JR // Set for JR
          }
          // Default for R-type if funct not recognized (can be NOP if SLL with rd=0,rt=0,shamt=0)
          // or treat as illegal instruction
        }
        // SLL rd, rt, shamt : rs is 0. ALU operand1 is rt, operand2 is shamt.
        // For SLL, SRL, SRA, rs is not used, rt is source, rd is dest. shamt is shift amount.
        // The current ALU takes rs1_data and rs2_data. So for shifts, rs1_addr should be rt, and rs2_addr... needs to be shamt.
        // This implies shamt needs to be passed or handled.
        // For simplicity, if it's a shift, we are currently setting uop_rs1_addr = rt, and uop_rs2_addr = shamt.
        // The RRU will fetch regs(rt) and regs(shamt). This is incorrect for shamt.
        // The immediate field in ALU is a better fit for shamt.
        // Let's redefine that: ALU operand2 will be shamt for shifts, taken from uop_imm.
        when(funct === MIPSFuncts.FN_SLL || funct === MIPSFuncts.FN_SRL || funct === MIPSFuncts.FN_SRA) {
            io.uop_rs1_addr := rt // Operand 1 for shift is from register rt
            io.uop_rs2_addr := 0.U // Operand 2 (shamt) comes from immediate field, rs2_addr not used for ALU data
            io.uop_imm := shamt
            ctrl_alu_src2_is_imm := true.B
            ctrl_reads_rs := false.B // Shift doesn't use rs field as source for data
            ctrl_reads_rt := true.B  // rt is the source register for data to be shifted
        }.otherwise {
            // For other R-Types, rs2_addr (rt) is used for ALU op2
            ctrl_alu_src2_is_imm := false.B
            // Reads for rs and rt already true by default for R-type
        }

      }
      is(MIPSOpcodes.OP_ADDI, MIPSOpcodes.OP_ADDIU) {
        ctrl_alu_op := ALUOps.ADD
        io.uop_rs1_addr := rs
        io.uop_rd_addr := rt // ADDI rt, rs, imm
        io.uop_imm := imm16_signed.asUInt
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B
        ctrl_reads_rs := true.B
        ctrl_reads_rt := false.B
      }
      is(MIPSOpcodes.OP_SLTI) {
        ctrl_alu_op := ALUOps.SLT
        io.uop_rs1_addr := rs
        io.uop_rd_addr := rt
        io.uop_imm := imm16_signed.asUInt
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B
        ctrl_reads_rs := true.B
        ctrl_reads_rt := false.B // Compares rs with immediate
      }
      is(MIPSOpcodes.OP_SLTIU) {
        ctrl_alu_op := ALUOps.SLTU
        io.uop_rs1_addr := rs
        io.uop_rd_addr := rt
        io.uop_imm := imm16_signed.asUInt // SLTIU uses signed immediate for comparison but result is 0 or 1.
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B
        ctrl_reads_rs := true.B
        ctrl_reads_rt := false.B // Compares rs with immediate
      }
      is(MIPSOpcodes.OP_ANDI) {
        ctrl_alu_op := ALUOps.AND
        io.uop_rs1_addr := rs
        io.uop_rd_addr := rt
        io.uop_imm := imm16_unsigned // ANDI uses zero-extended immediate
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B
        ctrl_reads_rs := true.B
        ctrl_reads_rt := false.B
      }
      is(MIPSOpcodes.OP_ORI) {
        ctrl_alu_op := ALUOps.OR
        io.uop_rs1_addr := rs
        io.uop_rd_addr := rt
        io.uop_imm := imm16_unsigned // ORI uses zero-extended immediate
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B
        ctrl_reads_rs := true.B
        ctrl_reads_rt := false.B
      }
      is(MIPSOpcodes.OP_XORI) {
        ctrl_alu_op := ALUOps.XOR
        io.uop_rs1_addr := rs
        io.uop_rd_addr := rt
        io.uop_imm := imm16_unsigned // XORI uses zero-extended immediate
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B
        ctrl_reads_rs := true.B
        ctrl_reads_rt := false.B
      }
      is(MIPSOpcodes.OP_LUI) { // LUI rt, imm
        ctrl_alu_op := ALUOps.COPY_B // ALU to pass immediate shifted
        // The immediate is loaded into the upper 16 bits of rt. Lower 16 bits are 0.
        io.uop_imm := Cat(imm16, Fill(16, 0.U))
        io.uop_rd_addr := rt
        io.uop_rs1_addr := 0.U // rs not used for LUI value
        ctrl_reg_write_en := true.B
        ctrl_alu_src2_is_imm := true.B // ALU uses imm directly
        ctrl_reads_rs := false.B
        ctrl_reads_rt := false.B
      }
      is(MIPSOpcodes.OP_LW) { // LW rt, offset(rs)
        ctrl_alu_op := ALUOps.ADD // For address calculation: rs + offset
        io.uop_rs1_addr := rs
        io.uop_imm := imm16_signed.asUInt
        io.uop_rd_addr := rt // Destination is rt
        ctrl_is_load := true.B
        ctrl_reg_write_en := true.B
        ctrl_mem_to_reg := true.B
        ctrl_alu_src2_is_imm := true.B // Address calculation uses immediate
        ctrl_reads_rs := true.B // Reads rs for base address
        ctrl_reads_rt := false.B // rt is destination
      }
      is(MIPSOpcodes.OP_SW) { // SW rt, offset(rs)
        ctrl_alu_op := ALUOps.ADD // For address calculation: rs + offset
        io.uop_rs1_addr := rs
        io.uop_rs2_addr := rt // rt is the source data for store, not used by ALU for addr calc
        io.uop_imm := imm16_signed.asUInt
        ctrl_is_store := true.B
        ctrl_reg_write_en := false.B // SW does not write to register file
        ctrl_alu_src2_is_imm := true.B // Address calculation uses immediate
        ctrl_reads_rs := true.B // Reads rs for base address
        ctrl_reads_rt := true.B // Reads rt for data to store
      }
      is(MIPSOpcodes.OP_BEQ) {
        ctrl_is_branch := true.B
        ctrl_branch_type := BranchTypes.BEQ
        io.uop_rs1_addr := rs
        io.uop_rs2_addr := rt
        io.uop_imm := imm16_signed.asUInt // PC-relative branch offset
        ctrl_reg_write_en := false.B
        io.uop_pc_next_sel := PCNextSelVals.BRANCH // Branch select
        ctrl_reads_rs := true.B
        ctrl_reads_rt := true.B
      }
      is(MIPSOpcodes.OP_BNE) {
        ctrl_is_branch := true.B
        ctrl_branch_type := BranchTypes.BNE
        io.uop_rs1_addr := rs
        io.uop_rs2_addr := rt
        io.uop_imm := imm16_signed.asUInt // PC-relative branch offset
        ctrl_reg_write_en := false.B
        io.uop_pc_next_sel := PCNextSelVals.BRANCH // Branch select
        ctrl_reads_rs := true.B
        ctrl_reads_rt := true.B
      }
      is(MIPSOpcodes.OP_J) {
        ctrl_is_jump := true.B
        ctrl_reg_write_en := false.B
        // Target: (PC+4)[31:28] ## jump_addr ## "00"
        val current_pc_plus_4_upper_4_bits = (io.uop_pc_in + 4.U)(31,28)
        ctrl_jump_target_from_idu := Cat(current_pc_plus_4_upper_4_bits, jump_addr, "b00".U(2.W))
        io.uop_pc_next_sel := PCNextSelVals.JUMP_J_JAL // J/JAL select
        ctrl_reads_rs := false.B
        ctrl_reads_rt := false.B
      }
      is(MIPSOpcodes.OP_JAL) {
        ctrl_is_jump := true.B
        ctrl_is_jal := true.B
        ctrl_reg_write_en := true.B
        io.uop_rd_addr := 31.U // $ra
        // Target: (PC+4)[31:28] ## jump_addr ## "00"
        val current_pc_plus_4_upper_4_bits = (io.uop_pc_in + 4.U)(31,28)
        ctrl_jump_target_from_idu := Cat(current_pc_plus_4_upper_4_bits, jump_addr, "b00".U(2.W))
        // Data to write to $ra is PC+4 (or PC+8 depending on pipeline, typically address of instruction AFTER delay slot)
        // For this design, let's assume PC+4 is captured by EXEU for JAL.
        // The IDU doesn't directly calculate the value for $ra; it signals EXEU to do so.
        io.uop_pc_next_sel := PCNextSelVals.JUMP_J_JAL // J/JAL select
        ctrl_reads_rs := false.B
        ctrl_reads_rt := false.B
      }
      // Default case for unrecognized opcodes (could be an illegal instruction)
    }
  }

  // Assign collected control signals to the output bundle
  io.ctrl.alu_op := ctrl_alu_op
  io.ctrl.alu_src2_is_imm := ctrl_alu_src2_is_imm
  io.ctrl.is_branch := ctrl_is_branch
  io.ctrl.branch_type := ctrl_branch_type
  io.ctrl.is_jump := ctrl_is_jump
  io.ctrl.is_jal := ctrl_is_jal
  io.ctrl.jump_target_from_idu := ctrl_jump_target_from_idu
  io.ctrl.is_load := ctrl_is_load
  io.ctrl.is_store := ctrl_is_store
  io.ctrl.reg_write_en := ctrl_reg_write_en
  io.ctrl.mem_to_reg := ctrl_mem_to_reg
  io.ctrl.reads_rs := ctrl_reads_rs
  io.ctrl.reads_rt := ctrl_reads_rt

  // Pass through valid signal - in a pipelined IDU, this would be registered.
  io.valid_out := io.valid_in && (instr =/= 0.U) // Consider instruction 0 (NOP SLL r0,r0,0) as valid or filter as needed
  io.uop_pc_out := io.uop_pc_in // Pass through PC

  // Special handling for NOP (SLL r0, r0, 0) which is all zeros
  when(instr === 0.U) {
    io.uop_rs1_addr := 0.U
    io.uop_rs2_addr := 0.U
    io.uop_rd_addr := 0.U
    io.uop_imm := 0.U
    io.uop_pc_next_sel := PCNextSelVals.PC_PLUS_4
    io.ctrl := IDUControlOutputs.NOP // This will set all ctrl signals to NOP defaults
  }
} 