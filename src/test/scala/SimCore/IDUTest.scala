package SimCore

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SimCore.cpu.stages.IDU
import SimCore.cpu.utils.ALUOps
import SimCore.cpu.utils.BranchTypes
import SimCore.cpu.utils.PCNextSelVals

class IDUTest extends AnyFlatSpec with chiseltest.ChiselScalatestTester {
  behavior of "IDU"

  it should "decode R-type ADD instruction correctly" in {
    test(new IDU) { dut =>
      // ADD rd, rs1, rs2 (R-type)
      // opcode=0x00, rs1=5, rs2=6, rd=7, funct=0x20 (ADD)
      val instr = "b00000000101001100111000000100000".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(5.U)
      dut.io.uop_rs2_addr.expect(6.U)
      dut.io.uop_rd_addr.expect(7.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_is_imm.expect(false.B)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(false.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.is_load.expect(false.B)
      dut.io.ctrl.is_store.expect(false.B)
      dut.io.ctrl.reg_write_en.expect(true.B)
      dut.io.ctrl.mem_to_reg.expect(false.B)
      dut.io.ctrl.reads_rs.expect(true.B)
      dut.io.ctrl.reads_rt.expect(true.B)
    }
  }

  it should "decode I-type ADDI instruction correctly" in {
    test(new IDU) { dut =>
      // ADDI rt, rs, imm (I-type)
      // opcode=0x08, rs=5, rt=7, imm=0x123
      val instr = "b00100000101001110000000100100011".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(5.U)
      dut.io.uop_rd_addr.expect(7.U)
      dut.io.uop_imm.expect(0x123.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_is_imm.expect(true.B)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(false.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.is_load.expect(false.B)
      dut.io.ctrl.is_store.expect(false.B)
      dut.io.ctrl.reg_write_en.expect(true.B)
      dut.io.ctrl.mem_to_reg.expect(false.B)
      dut.io.ctrl.reads_rs.expect(true.B)
      dut.io.ctrl.reads_rt.expect(false.B)
    }
  }

  it should "decode LW instruction correctly" in {
    test(new IDU) { dut =>
      // LW rt, offset(rs) (I-type)
      // opcode=0x23, rs=5, rt=7, offset=0x100
      val instr = "b10001100101001110000000100000000".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(5.U)
      dut.io.uop_rd_addr.expect(7.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_is_imm.expect(true.B)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(false.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.is_load.expect(true.B)
      dut.io.ctrl.is_store.expect(false.B)
      dut.io.ctrl.reg_write_en.expect(true.B)
      dut.io.ctrl.mem_to_reg.expect(true.B)
      dut.io.ctrl.reads_rs.expect(true.B)
      dut.io.ctrl.reads_rt.expect(false.B)
    }
  }

  it should "decode SW instruction correctly" in {
    test(new IDU) { dut =>
      // SW rt, offset(rs) (I-type)
      // opcode=0x2B, rs=5, rt=7, offset=0x100
      val instr = "b10101100101001110000000100000000".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(5.U)
      dut.io.uop_rs2_addr.expect(7.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_is_imm.expect(true.B)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(false.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.is_load.expect(false.B)
      dut.io.ctrl.is_store.expect(true.B)
      dut.io.ctrl.reg_write_en.expect(false.B)
      dut.io.ctrl.mem_to_reg.expect(false.B)
      dut.io.ctrl.reads_rs.expect(true.B)
      dut.io.ctrl.reads_rt.expect(true.B)
    }
  }

  it should "decode BEQ instruction correctly" in {
    test(new IDU) { dut =>
      // BEQ rs, rt, offset (I-type branch)
      // opcode=0x04, rs=5, rt=7, offset=0x100
      val instr = "b00010000101001110000000100000000".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(5.U)
      dut.io.uop_rs2_addr.expect(7.U)
      dut.io.uop_pc_next_sel.expect(PCNextSelVals.BRANCH)
      dut.io.ctrl.is_branch.expect(true.B)
      dut.io.ctrl.branch_type.expect(BranchTypes.BEQ)
      dut.io.ctrl.is_jump.expect(false.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.reg_write_en.expect(false.B)
      dut.io.ctrl.reads_rs.expect(true.B)
      dut.io.ctrl.reads_rt.expect(true.B)
    }
  }

  it should "decode J instruction correctly" in {
    test(new IDU) { dut =>
      // J target (J-type)
      // opcode=0x02, target=0x123456
      val instr = "b00001000000000010001000110101010".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_pc_next_sel.expect(PCNextSelVals.JUMP_J_JAL)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(true.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.reg_write_en.expect(false.B)
      dut.io.ctrl.reads_rs.expect(false.B)
      dut.io.ctrl.reads_rt.expect(false.B)
    }
  }

  it should "decode JAL instruction correctly" in {
    test(new IDU) { dut =>
      // JAL target (J-type)
      // opcode=0x03, target=0x123456
      val instr = "b00001100000000010001000110101010".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rd_addr.expect(31.U) // $ra = r31
      dut.io.uop_pc_next_sel.expect(PCNextSelVals.JUMP_J_JAL)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(true.B)
      dut.io.ctrl.is_jal.expect(true.B)
      dut.io.ctrl.reg_write_en.expect(true.B)
      dut.io.ctrl.reads_rs.expect(false.B)
      dut.io.ctrl.reads_rt.expect(false.B)
    }
  }

  it should "decode JR instruction correctly" in {
    test(new IDU) { dut =>
      // JR rs (R-type jump)
      // opcode=0x00, rs=5, rt=0, rd=0, shamt=0, funct=0x08 (JR)
      val instr = "b00000000101000000000000000001000".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(5.U)
      dut.io.uop_pc_next_sel.expect(PCNextSelVals.JUMP_JR)
      dut.io.ctrl.alu_op.expect(ALUOps.COPY_A)
      dut.io.ctrl.is_branch.expect(false.B)
      dut.io.ctrl.is_jump.expect(true.B)
      dut.io.ctrl.is_jal.expect(false.B)
      dut.io.ctrl.reg_write_en.expect(false.B)
      dut.io.ctrl.reads_rs.expect(true.B)
      dut.io.ctrl.reads_rt.expect(false.B)
    }
  }

  it should "decode NOP instruction correctly" in {
    test(new IDU) { dut =>
      // NOP (SLL r0, r0, 0)
      // opcode=0x00, rs=0, rt=0, rd=0, shamt=0, funct=0x00 (SLL)
      val instr = "b00000000000000000000000000000000".U
      
      dut.io.valid_in.poke(true.B)
      dut.io.uop_instr_in.poke(instr)
      dut.io.uop_pc_in.poke(0x1000.U)
      dut.clock.step(1)
      
      dut.io.valid_out.expect(true.B)
      dut.io.uop_rs1_addr.expect(0.U)
      dut.io.uop_rs2_addr.expect(0.U)
      dut.io.uop_rd_addr.expect(0.U)
      dut.io.uop_pc_next_sel.expect(PCNextSelVals.PC_PLUS_4)
      dut.io.ctrl.reg_write_en.expect(false.B)
    }
  }
} 