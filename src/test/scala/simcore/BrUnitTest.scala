package simcore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.components.BrUnit
import simcore.cpu.utils.constants.BranchTypes

class BrUnitTest extends AnyFlatSpec with ChiselSim {
  val XLEN = 32

  behavior of "BrUnit"

  it should "correctly evaluate BEQ condition" in {
    simulate(new BrUnit(XLEN)) { dut =>
      // Equal values should take the branch
      dut.io.pc.poke(0x1000.U)
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.imm.poke(16.U)
      dut.io.is_branch.poke(true.B)
      dut.io.is_jump.poke(false.B)
      dut.io.is_jalr.poke(false.B)
      dut.io.branch_type.poke(BranchTypes.BEQ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      dut.io.branch_target.expect(0x1010.U) // 0x1000 + 16
      
      // Unequal values should not take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(10.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BEQ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
    }
  }

  it should "correctly evaluate BNE condition" in {
    simulate(new BrUnit(XLEN)) { dut =>
      // Unequal values should take the branch
      dut.io.pc.poke(0x1000.U)
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(10.U)
      dut.io.imm.poke(16.U)
      dut.io.is_branch.poke(true.B)
      dut.io.is_jump.poke(false.B)
      dut.io.is_jalr.poke(false.B)
      dut.io.branch_type.poke(BranchTypes.BNE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      dut.io.branch_target.expect(0x1010.U) // 0x1000 + 16
      
      // Equal values should not take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BNE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
    }
  }

  it should "correctly evaluate JALR target calculation" in {
    simulate(new BrUnit(XLEN)) { dut =>
      dut.io.pc.poke(0x1000.U)
      dut.io.rs1_data.poke(0x2000.U)
      dut.io.imm.poke(5.U)
      dut.io.is_branch.poke(false.B)
      dut.io.is_jump.poke(true.B)
      dut.io.is_jalr.poke(true.B)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      dut.io.branch_target.expect(0x2004.U) // (0x2000 + 5) & ~1 = 0x2004 (clear LSB)
    }
  }

  it should "correctly evaluate JAL target calculation" in {
    simulate(new BrUnit(XLEN)) { dut =>
      dut.io.pc.poke(0x1000.U)
      dut.io.imm.poke(0x100.U)
      dut.io.is_branch.poke(false.B)
      dut.io.is_jump.poke(true.B)
      dut.io.is_jalr.poke(false.B)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      dut.io.branch_target.expect(0x1100.U) // 0x1000 + 0x100
    }
  }

  it should "correctly handle signed comparisons" in {
    simulate(new BrUnit(XLEN)) { dut =>
      // Test BLT with signed comparison
      dut.io.pc.poke(0x1000.U)
      dut.io.rs1_data.poke("hFFFFFFFF".U) // -1 in two's complement
      dut.io.rs2_data.poke(1.U)
      dut.io.imm.poke(16.U)
      dut.io.is_branch.poke(true.B)
      dut.io.is_jump.poke(false.B)
      dut.io.is_jalr.poke(false.B)
      dut.io.branch_type.poke(BranchTypes.BLT)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B) // -1 < 1
      
      // Test BLTU with unsigned comparison
      dut.io.branch_type.poke(BranchTypes.BLTU)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B) // hFFFFFFFF > 1 in unsigned comparison
    }
  }

  it should "not take branch when is_branch is false" in {
    simulate(new BrUnit(XLEN)) { dut =>
      // Even if the condition is true, branch should not be taken if is_branch is false
      dut.io.pc.poke(0x1000.U)
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.imm.poke(16.U)
      dut.io.is_branch.poke(false.B)
      dut.io.is_jump.poke(false.B)
      dut.io.is_jalr.poke(false.B)
      dut.io.branch_type.poke(BranchTypes.BEQ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
    }
  }

  it should "correctly evaluate single-operand branch conditions" in {
    simulate(new BrUnit(XLEN)) { dut =>
      dut.io.pc.poke(0x1000.U)
      dut.io.imm.poke(16.U)
      dut.io.is_branch.poke(true.B)
      dut.io.is_jump.poke(false.B)
      dut.io.is_jalr.poke(false.B)
      
      // Test BGEZ (Branch if Greater than or Equal to Zero)
      // Case 1: rs1 > 0
      dut.io.rs1_data.poke(5.U)
      dut.io.branch_type.poke(BranchTypes.BGEZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Case 2: rs1 = 0
      dut.io.rs1_data.poke(0.U)
      dut.io.branch_type.poke(BranchTypes.BGEZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Case 3: rs1 < 0
      dut.io.rs1_data.poke("hFFFFFFFF".U) // -1 in two's complement
      dut.io.branch_type.poke(BranchTypes.BGEZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
      
      // Test BGTZ (Branch if Greater Than Zero)
      // Case 1: rs1 > 0
      dut.io.rs1_data.poke(5.U)
      dut.io.branch_type.poke(BranchTypes.BGTZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Case 2: rs1 = 0
      dut.io.rs1_data.poke(0.U)
      dut.io.branch_type.poke(BranchTypes.BGTZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
      
      // Case 3: rs1 < 0
      dut.io.rs1_data.poke("hFFFFFFFF".U) // -1 in two's complement
      dut.io.branch_type.poke(BranchTypes.BGTZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
      
      // Test BLEZ (Branch if Less than or Equal to Zero)
      // Case 1: rs1 > 0
      dut.io.rs1_data.poke(5.U)
      dut.io.branch_type.poke(BranchTypes.BLEZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
      
      // Case 2: rs1 = 0
      dut.io.rs1_data.poke(0.U)
      dut.io.branch_type.poke(BranchTypes.BLEZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Case 3: rs1 < 0
      dut.io.rs1_data.poke("hFFFFFFFF".U) // -1 in two's complement
      dut.io.branch_type.poke(BranchTypes.BLEZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Test BLTZ (Branch if Less Than Zero)
      // Case 1: rs1 > 0
      dut.io.rs1_data.poke(5.U)
      dut.io.branch_type.poke(BranchTypes.BLTZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
      
      // Case 2: rs1 = 0
      dut.io.rs1_data.poke(0.U)
      dut.io.branch_type.poke(BranchTypes.BLTZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
      
      // Case 3: rs1 < 0
      dut.io.rs1_data.poke("hFFFFFFFF".U) // -1 in two's complement
      dut.io.branch_type.poke(BranchTypes.BLTZ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
    }
  }
}