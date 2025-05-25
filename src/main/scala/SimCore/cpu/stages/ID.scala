package SimCore.cpu.stages

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.ALUOps
import SimCore.cpu.utils.BranchTypes
import SimCore.cpu.utils.ControlBundle
import SimCore.cpu.utils.IFID_Bundle

// Define MIPS Opcodes and Funct codes (subset)
object MIPSOpcodes {
  // R-Type (opcode = 0)
  val OP_RTYPE = "b000000".U
  // I-Type
  val OP_ADDI = "b001000".U
  val OP_ADDIU = "b001001".U
  val OP_SLTI = "b001010".U
  val OP_SLTIU = "b001011".U
  val OP_ANDI = "b001100".U
  val OP_ORI = "b001101".U
  val OP_XORI = "b001110".U
  val OP_LUI = "b001111".U
  val OP_LW = "b100011".U
  val OP_SW = "b101011".U
  val OP_BEQ = "b000100".U
  val OP_BNE = "b000101".U
  // J-Type
  val OP_J = "b000010".U
  val OP_JAL = "b000011".U
}

object MIPSFuncts {
  // R-Type Funct codes
  val FN_ADD = "b100000".U
  val FN_ADDU = "b100001".U
  val FN_SUB = "b100010".U
  val FN_SUBU = "b100011".U
  val FN_AND = "b100100".U
  val FN_OR = "b100101".U
  val FN_XOR = "b100110".U
  val FN_NOR = "b100111".U
  val FN_SLT = "b101010".U
  val FN_SLTU = "b101011".U
  val FN_SLL = "b000000".U
  val FN_SRL = "b000010".U
  val FN_SRA = "b000011".U
  val FN_JR = "b001000".U
  // SLL, SRL, SRA also need to check shamt or rs for 0 if it's NOP for SLL
}

/** Instruction Decode Unit Decodes instructions and generates control signals
  */
class ID extends Module {
  val io = IO(new Bundle {
    // Input from IF stage
    val in = Input(new IFID_Bundle())

    // Output signals for ID stage
    val pc = Output(UInt(32.W))
    val rs1_addr = Output(UInt(5.W))
    val rs2_addr = Output(UInt(5.W))
    val rd_addr = Output(UInt(5.W))
    val imm = Output(UInt(32.W))
    val ctrl = Output(new ControlBundle())
    val valid = Output(Bool())

    // Additional outputs for hazard detection
    val uses_rs1 = Output(Bool())
    val uses_rs2 = Output(Bool())
  })

  // Extract instruction fields
  val instr = io.in.instr
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

  // Default output values
  io.pc := io.in.pc
  io.rs1_addr := 0.U
  io.rs2_addr := 0.U
  io.rd_addr := 0.U
  io.imm := 0.U
  io.ctrl := ControlBundle.NOP
  io.valid := io.in.valid
  io.uses_rs1 := false.B
  io.uses_rs2 := false.B

  // Default control values (will be set during decode)
  val ctrl_alu_op = WireDefault(0.U(4.W))
  val ctrl_alu_src1_sel = WireDefault(0.U(2.W))
  val ctrl_alu_src2_sel = WireDefault(0.U(2.W))
  val ctrl_branch_type = WireDefault(0.U(3.W))
  val ctrl_jump = WireDefault(false.B)
  val ctrl_mem_read = WireDefault(false.B)
  val ctrl_mem_write = WireDefault(false.B)
  val ctrl_mem_to_reg = WireDefault(false.B)
  val ctrl_reg_write = WireDefault(false.B)
  val ctrl_uses_rs1 = WireDefault(false.B)
  val ctrl_uses_rs2 = WireDefault(false.B)

  // Instruction decode logic
  when(io.in.valid) {
    switch(opcode) {
      is(MIPSOpcodes.OP_RTYPE) { // R-type instructions
        io.rs1_addr := rs
        io.rs2_addr := rt
        io.rd_addr := rd
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := true.B
        ctrl_reg_write := true.B

        switch(funct) {
          is(MIPSFuncts.FN_ADD) {
            ctrl_alu_op := ALUOps.ADD; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_ADDU) {
            ctrl_alu_op := ALUOps.ADD; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_SUB) {
            ctrl_alu_op := ALUOps.SUB; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_SUBU) {
            ctrl_alu_op := ALUOps.SUB; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_AND) {
            ctrl_alu_op := ALUOps.AND; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_OR) {
            ctrl_alu_op := ALUOps.OR; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_XOR) {
            ctrl_alu_op := ALUOps.XOR; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_NOR) {
            ctrl_alu_op := ALUOps.NOR; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_SLT) {
            ctrl_alu_op := ALUOps.SLT; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_SLTU) {
            ctrl_alu_op := ALUOps.SLTU; ctrl_alu_src2_sel := 0.U
          }
          is(MIPSFuncts.FN_SLL) {
            ctrl_alu_op := ALUOps.SLL
            io.rs1_addr := rt
            io.imm := shamt
            ctrl_alu_src2_sel := 1.U
            ctrl_uses_rs1 := false.B
            ctrl_uses_rs2 := true.B
          }
          is(MIPSFuncts.FN_SRL) {
            ctrl_alu_op := ALUOps.SRL
            io.rs1_addr := rt
            io.imm := shamt
            ctrl_alu_src2_sel := 1.U
            ctrl_uses_rs1 := false.B
            ctrl_uses_rs2 := true.B
          }
          is(MIPSFuncts.FN_SRA) {
            ctrl_alu_op := ALUOps.SRA
            io.rs1_addr := rt
            io.imm := shamt
            ctrl_alu_src2_sel := 1.U
            ctrl_uses_rs1 := false.B
            ctrl_uses_rs2 := true.B
          }
          is(MIPSFuncts.FN_JR) {
            ctrl_jump := true.B
            io.rs1_addr := rs
            ctrl_uses_rs1 := true.B
            ctrl_uses_rs2 := false.B
            ctrl_reg_write := false.B
            ctrl_alu_op := ALUOps.ADD
            ctrl_alu_src2_sel := 0.U
          }
        }

        // Special handling for shift instructions
        when(
          funct === MIPSFuncts.FN_SLL || funct === MIPSFuncts.FN_SRL || funct === MIPSFuncts.FN_SRA
        ) {
          io.rs1_addr := rt // Operand 1 for shift is from register rt
          io.rs2_addr := 0.U // Operand 2 (shamt) comes from immediate field
          io.imm := shamt
          ctrl_alu_src2_sel := 1.U // Use immediate
          ctrl_uses_rs1 := false.B
          ctrl_uses_rs2 := true.B
        }
      }

      // I-Type instructions
      is(MIPSOpcodes.OP_ADDI, MIPSOpcodes.OP_ADDIU) {
        ctrl_alu_op := ALUOps.ADD
        io.rs1_addr := rs
        io.rd_addr := rt // ADDI rt, rs, imm
        io.imm := imm16_signed.asUInt
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_SLTI) {
        ctrl_alu_op := ALUOps.SLT
        io.rs1_addr := rs
        io.rd_addr := rt
        io.imm := imm16_signed.asUInt
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_SLTIU) {
        ctrl_alu_op := ALUOps.SLTU
        io.rs1_addr := rs
        io.rd_addr := rt
        io.imm := imm16_signed.asUInt
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_ANDI) {
        ctrl_alu_op := ALUOps.AND
        io.rs1_addr := rs
        io.rd_addr := rt
        io.imm := imm16_unsigned
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_ORI) {
        ctrl_alu_op := ALUOps.OR
        io.rs1_addr := rs
        io.rd_addr := rt
        io.imm := imm16_unsigned
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_XORI) {
        ctrl_alu_op := ALUOps.XOR
        io.rs1_addr := rs
        io.rd_addr := rt
        io.imm := imm16_unsigned
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_LUI) {
        ctrl_alu_op := ALUOps.LUI
        io.rd_addr := rt
        io.imm := imm16
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := false.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_LW) {
        ctrl_alu_op := ALUOps.ADD
        io.rs1_addr := rs
        io.rd_addr := rt
        io.imm := imm16_signed.asUInt
        ctrl_mem_read := true.B
        ctrl_mem_to_reg := true.B
        ctrl_reg_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_SW) {
        ctrl_alu_op := ALUOps.ADD
        io.rs1_addr := rs
        io.rs2_addr := rt
        io.imm := imm16_signed.asUInt
        ctrl_mem_write := true.B
        ctrl_alu_src2_sel := 1.U // Use immediate
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := true.B
      }

      is(MIPSOpcodes.OP_BEQ) {
        ctrl_alu_op := ALUOps.SUB
        io.rs1_addr := rs
        io.rs2_addr := rt
        io.imm := (imm16_signed.asUInt << 2) // Branch address calculation
        ctrl_branch_type := BranchTypes.BEQ
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := true.B
      }

      is(MIPSOpcodes.OP_BNE) {
        ctrl_alu_op := ALUOps.SUB
        io.rs1_addr := rs
        io.rs2_addr := rt
        io.imm := (imm16_signed.asUInt << 2) // Branch address calculation
        ctrl_branch_type := BranchTypes.BNE
        ctrl_uses_rs1 := true.B
        ctrl_uses_rs2 := true.B
      }

      is(MIPSOpcodes.OP_J) {
        val target = Cat(io.in.pc(31, 28), jump_addr, 0.U(2.W))
        io.imm := target
        ctrl_jump := true.B
        ctrl_uses_rs1 := false.B
        ctrl_uses_rs2 := false.B
      }

      is(MIPSOpcodes.OP_JAL) {
        val target = Cat(io.in.pc(31, 28), jump_addr, 0.U(2.W))
        io.imm := target
        io.rd_addr := 31.U // $ra
        ctrl_jump := true.B
        ctrl_reg_write := true.B
        ctrl_alu_op := ALUOps.ADD
        ctrl_alu_src1_sel := 2.U // Use PC
        ctrl_alu_src2_sel := 2.U // Use constant 4
        ctrl_uses_rs1 := false.B
        ctrl_uses_rs2 := false.B
      }
    }
  }

  // Connect wire control signals to output bundle
  io.ctrl.alu_op := ctrl_alu_op
  io.ctrl.alu_src1_sel := ctrl_alu_src1_sel
  io.ctrl.alu_src2_sel := ctrl_alu_src2_sel
  io.ctrl.branch_type := ctrl_branch_type
  io.ctrl.jump := ctrl_jump
  io.ctrl.mem_read := ctrl_mem_read
  io.ctrl.mem_write := ctrl_mem_write
  io.ctrl.mem_to_reg := ctrl_mem_to_reg
  io.ctrl.reg_write := ctrl_reg_write
  io.ctrl.uses_rs1 := ctrl_uses_rs1
  io.ctrl.uses_rs2 := ctrl_uses_rs2

  // Set hazard detection flags
  io.uses_rs1 := ctrl_uses_rs1
  io.uses_rs2 := ctrl_uses_rs2
}
