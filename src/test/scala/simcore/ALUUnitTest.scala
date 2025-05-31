package simcore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim // Added
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.components.ALUUnit
import simcore.cpu.utils.constants.ALUOps


class ALUUnitTest extends AnyFlatSpec with ChiselSim { // Updated
  val XLEN = 32

  behavior of "ALUUnit"

  it should "perform ADD operation correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      dut.io.alu_op.poke(ALUOps.ADD)
      dut.io.operand1.poke(5.U)
      dut.io.operand2.poke(3.U)
      dut.clock.step(1)
      dut.io.result.expect(8.U)
    }
  }

  it should "perform SUB operation correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      dut.io.alu_op.poke(ALUOps.SUB)
      dut.io.operand1.poke(10.U)
      dut.io.operand2.poke(4.U)
      dut.clock.step(1)
      dut.io.result.expect(6.U)
    }
  }

  it should "perform AND operation correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      dut.io.alu_op.poke(ALUOps.AND)
      dut.io.operand1.poke(0x0f.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      dut.io.result.expect(0x03.U) // 0000 0011
    }
  }

  it should "perform OR operation correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      dut.io.alu_op.poke(ALUOps.OR)
      dut.io.operand1.poke(0x0f.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      dut.io.result.expect(0x3f.U) // 0011 1111
    }
  }

  it should "perform XOR operation correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      dut.io.alu_op.poke(ALUOps.XOR)
      dut.io.operand1.poke(0x0f.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      dut.io.result.expect(0x3c.U) // 0011 1100
    }
  }

  // it should "perform NOR operation correctly" in {
  //   simulate(new ALUUnit) { dut => // Updated
  //     dut.io.alu_op.poke(ALUOps.NOR)
  //     dut.io.operand1.poke(0x0f.U) // 0000 1111
  //     dut.io.operand2.poke(0x33.U) // 0011 0011
  //     dut.clock.step(1)
  //     // ~(0x0F | 0x33) = ~(0x3F) = 0xFFFFFFC0 (32-bit)
  //     // Use a proper bitwise complement in UInt context to avoid negative numbers
  //     dut.io.result.expect(("hffffffff".U(32.W) ^ 0x3f.U))
  //   }
  // }

  it should "perform SLT operation correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      // Test when operand1 < operand2
      dut.io.alu_op.poke(ALUOps.SLT)
      dut.io.operand1.poke(5.U)
      dut.io.operand2.poke(10.U)
      dut.clock.step(1)
      dut.io.result.expect(1.U)

      // Test when operand1 > operand2
      dut.io.alu_op.poke(ALUOps.SLT)
      dut.io.operand1.poke(20.U)
      dut.io.operand2.poke(10.U)
      dut.clock.step(1)
      dut.io.result.expect(0.U)

      // Test with negative values (using a sign bit in the 32-bit representation)
      dut.io.alu_op.poke(ALUOps.SLT)
      // Use 0x80000003 to represent -5 in 32-bit 2's complement
      dut.io.operand1.poke("h80000003".U(32.W))
      dut.io.operand2.poke(5.U)
      dut.clock.step(1)
      dut.io.result.expect(1.U)
    }
  }

  it should "perform shift operations correctly" in {
    simulate(new ALUUnit(XLEN)) { dut => // Updated
      // SLL - Shift Left Logical
      dut.io.alu_op.poke(ALUOps.SLL)
      dut.io.operand1.poke(0x1.U)
      dut.io.operand2.poke(
        4.U
      ) // This is shamt, needs to be connected to lower 5 bits of operand2 if used as shamt in MIPS
      dut.clock.step(1)
      dut.io.result.expect(0x10.U)

      // SRL - Shift Right Logical
      dut.io.alu_op.poke(ALUOps.SRL)
      dut.io.operand1.poke(0x10.U)
      dut.io.operand2.poke(2.U)
      dut.clock.step(1)
      dut.io.result.expect(0x4.U)

      // SRA - Shift Right Arithmetic (sign-extending)
      dut.io.alu_op.poke(ALUOps.SRA)
      dut.io.operand1.poke(0x80000000L.U) // Most significant bit is 1
      dut.io.operand2.poke(4.U)
      dut.clock.step(1)
      dut.io.result.expect(0xf8000000L.U) // Should sign-extend
    }
  }
}
