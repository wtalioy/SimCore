package SimCore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import SimCore.cpu.components.BrUnit
import SimCore.cpu.utils.BranchTypes

class BrUnitTest extends AnyFlatSpec with ChiselSim {
  behavior of "BrUnit"

  it should "correctly evaluate BEQ condition" in {
    simulate(new BrUnit) { dut =>
      // Equal values should take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BEQ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
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
    simulate(new BrUnit) { dut =>
      // Unequal values should take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(10.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BNE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Equal values should not take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BNE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
    }
  }

  it should "correctly evaluate BLT condition" in {
    simulate(new BrUnit) { dut =>
      // rs1 < rs2 should take the branch (signed comparison)
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(10.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BLT)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // rs1 >= rs2 should not take the branch
      dut.io.rs1_data.poke(10.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BLT)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)

      // Test with negative values
      dut.io.rs1_data.poke(-5.S.asUInt)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BLT)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
    }
  }

  it should "correctly evaluate BGE condition" in {
    simulate(new BrUnit) { dut =>
      // rs1 >= rs2 should take the branch (signed comparison)
      dut.io.rs1_data.poke(10.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BGE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // Equal values should also take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BGE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(true.B)
      
      // rs1 < rs2 should not take the branch
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(10.U)
      dut.io.is_branch.poke(true.B)
      dut.io.branch_type.poke(BranchTypes.BGE)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
    }
  }

  it should "not take branch when is_branch is false" in {
    simulate(new BrUnit) { dut =>
      // Even if the condition is true, branch should not be taken if is_branch is false
      dut.io.rs1_data.poke(5.U)
      dut.io.rs2_data.poke(5.U)
      dut.io.is_branch.poke(false.B)
      dut.io.branch_type.poke(BranchTypes.BEQ)
      dut.clock.step(1)
      dut.io.branch_taken.expect(false.B)
    }
  }
}