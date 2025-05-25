package simcore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.stages.ID
import simcore.cpu.utils.ALUOps
import simcore.cpu.utils.BranchTypes
import simcore.cpu.utils.IFID_Bundle

class IDUTest extends AnyFlatSpec with ChiselSim { // Updated
  behavior of "Decode Stage"

  it should "decode R-type ADD instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // ADD rd, rs1, rs2 (R-type)
      // opcode=0x00, rs1=5, rs2=6, rd=7, funct=0x20 (ADD)
      val instr = "b00000000101001100111000000100000".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(5.U)
      dut.io.rs2_addr.expect(6.U)
      dut.io.rd_addr.expect(14.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_sel.expect(0.U) // Register value
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(false.B)
      dut.io.ctrl.mem_read.expect(false.B)
      dut.io.ctrl.mem_write.expect(false.B)
      dut.io.ctrl.reg_write.expect(true.B)
      dut.io.ctrl.mem_to_reg.expect(false.B)
      dut.io.uses_rs1.expect(true.B)
      dut.io.uses_rs2.expect(true.B)
    }
  }

  it should "decode I-type ADDI instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // ADDI rt, rs, imm (I-type)
      // opcode=0x08, rs=5, rt=7, imm=0x123
      val instr = "b00100000101001110000000100100011".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(5.U)
      dut.io.rd_addr.expect(7.U)
      dut.io.imm.expect(0x123.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_sel.expect(1.U) // Immediate
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(false.B)
      dut.io.ctrl.mem_read.expect(false.B)
      dut.io.ctrl.mem_write.expect(false.B)
      dut.io.ctrl.reg_write.expect(true.B)
      dut.io.ctrl.mem_to_reg.expect(false.B)
      dut.io.uses_rs1.expect(true.B)
      dut.io.uses_rs2.expect(false.B)
    }
  }

  it should "decode LW instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // LW rt, offset(rs) (I-type)
      // opcode=0x23, rs=5, rt=7, offset=0x100
      val instr = "b10001100101001110000000100000000".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(5.U)
      dut.io.rd_addr.expect(7.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_sel.expect(1.U) // Immediate
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(false.B)
      dut.io.ctrl.mem_read.expect(true.B)
      dut.io.ctrl.mem_write.expect(false.B)
      dut.io.ctrl.reg_write.expect(true.B)
      dut.io.ctrl.mem_to_reg.expect(true.B)
      dut.io.uses_rs1.expect(true.B)
      dut.io.uses_rs2.expect(false.B)
    }
  }

  it should "decode SW instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // SW rt, offset(rs) (I-type)
      // opcode=0x2B, rs=5, rt=7, offset=0x100
      val instr = "b10101100101001110000000100000000".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(5.U)
      dut.io.rs2_addr.expect(7.U)
      dut.io.ctrl.alu_op.expect(ALUOps.ADD)
      dut.io.ctrl.alu_src2_sel.expect(1.U) // Immediate
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(false.B)
      dut.io.ctrl.mem_read.expect(false.B)
      dut.io.ctrl.mem_write.expect(true.B)
      dut.io.ctrl.reg_write.expect(false.B)
      dut.io.ctrl.mem_to_reg.expect(false.B)
      dut.io.uses_rs1.expect(true.B)
      dut.io.uses_rs2.expect(true.B)
    }
  }

  it should "decode BEQ instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // BEQ rs, rt, offset (I-type branch)
      // opcode=0x04, rs=5, rt=7, offset=0x100
      val instr = "b00010000101001110000000100000000".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(5.U)
      dut.io.rs2_addr.expect(7.U)
      dut.io.ctrl.branch_type.expect(BranchTypes.BEQ)
      dut.io.ctrl.jump.expect(false.B)
      dut.io.ctrl.reg_write.expect(false.B)
      dut.io.uses_rs1.expect(true.B)
      dut.io.uses_rs2.expect(true.B)
    }
  }

  it should "decode J instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // J target (J-type)
      // opcode=0x02, target=0x123456
      val instr = "b00001000000000010001000110101010".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(true.B)
      dut.io.ctrl.reg_write.expect(false.B)
      dut.io.uses_rs1.expect(false.B)
      dut.io.uses_rs2.expect(false.B)
    }
  }

  it should "decode JAL instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // JAL target (J-type)
      // opcode=0x03, target=0x123456
      val instr = "b00001100000000010001000110101010".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rd_addr.expect(31.U) // $ra = r31
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(true.B)
      dut.io.ctrl.alu_src1_sel.expect(2.U) // PC
      dut.io.ctrl.alu_src2_sel.expect(2.U) // Constant 4
      dut.io.ctrl.reg_write.expect(true.B)
      dut.io.uses_rs1.expect(false.B)
      dut.io.uses_rs2.expect(false.B)
    }
  }

  it should "decode JR instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // JR rs (R-type jump)
      // opcode=0x00, rs=5, rt=0, rd=0, shamt=0, funct=0x08 (JR)
      val instr = "b00000000101000000000000000001000".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(5.U)
      dut.io.ctrl.branch_type.expect(BranchTypes.NONE)
      dut.io.ctrl.jump.expect(true.B)
      dut.io.ctrl.reg_write.expect(false.B)
      dut.io.uses_rs1.expect(true.B)
      dut.io.uses_rs2.expect(false.B)
    }
  }

  it should "decode NOP instruction correctly" in {
    simulate(new ID) { dut => // Updated
      // NOP (SLL r0, r0, 0)
      // opcode=0x00, rs=0, rt=0, rd=0, shamt=0, funct=0x00 (SLL)
      val instr = "b00000000000000000000000000000000".U
      
      // Create input bundle properly using Chisel hardware types
      dut.io.in.pc.poke(0x1000.U)
      dut.io.in.instr.poke(instr)
      dut.io.in.valid.poke(true.B)
      
      dut.clock.step(1)
      
      // Check outputs
      dut.io.valid.expect(true.B)
      dut.io.rs1_addr.expect(0.U)
      dut.io.rs2_addr.expect(0.U)
      dut.io.rd_addr.expect(0.U)
      dut.io.ctrl.reg_write.expect(true.B)
    }
  }
} 