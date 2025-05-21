package SimCore

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SimCore.cpu.components.ALUUnit
import SimCore.cpu.utils.ALUOps

class ALUUnitTest extends AnyFlatSpec with chiseltest.ChiselScalatestTester {
  behavior of "ALUUnit"

  it should "perform ADD operation correctly" in {
    test(new ALUUnit) { dut =>
      dut.io.alu_op.poke(ALUOps.ADD)
      dut.io.operand1.poke(5.U)
      dut.io.operand2.poke(3.U)
      dut.clock.step(1)
      dut.io.result.expect(8.U)
    }
  }

  it should "perform SUB operation correctly" in {
    test(new ALUUnit) { dut =>
      dut.io.alu_op.poke(ALUOps.SUB)
      dut.io.operand1.poke(10.U)
      dut.io.operand2.poke(4.U)
      dut.clock.step(1)
      dut.io.result.expect(6.U)
    }
  }

  it should "perform AND operation correctly" in {
    test(new ALUUnit) { dut =>
      dut.io.alu_op.poke(ALUOps.AND)
      dut.io.operand1.poke(0x0F.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      dut.io.result.expect(0x03.U) // 0000 0011
    }
  }

  it should "perform OR operation correctly" in {
    test(new ALUUnit) { dut =>
      dut.io.alu_op.poke(ALUOps.OR)
      dut.io.operand1.poke(0x0F.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      dut.io.result.expect(0x3F.U) // 0011 1111
    }
  }

  it should "perform XOR operation correctly" in {
    test(new ALUUnit) { dut =>
      dut.io.alu_op.poke(ALUOps.XOR)
      dut.io.operand1.poke(0x0F.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      dut.io.result.expect(0x3C.U) // 0011 1100
    }
  }

  it should "perform NOR operation correctly" in {
    test(new ALUUnit) { dut =>
      dut.io.alu_op.poke(ALUOps.NOR)
      dut.io.operand1.poke(0x0F.U) // 0000 1111
      dut.io.operand2.poke(0x33.U) // 0011 0011
      dut.clock.step(1)
      // ~(0x0F | 0x33) = ~(0x3F) = 0xFFFFFFC0 (32-bit)
      // But we're only checking the lower bits
      dut.io.result.expect(~0x3F.U)
    }
  }

  it should "perform SLT operation correctly" in {
    test(new ALUUnit) { dut =>
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

      // Test with negative values
      dut.io.alu_op.poke(ALUOps.SLT)
      dut.io.operand1.poke(-5.S.asUInt)
      dut.io.operand2.poke(5.U)
      dut.clock.step(1)
      dut.io.result.expect(1.U)
    }
  }

  it should "perform shift operations correctly" in {
    test(new ALUUnit) { dut =>
      // SLL - Shift Left Logical
      dut.io.alu_op.poke(ALUOps.SLL)
      dut.io.operand1.poke(0x1.U)
      dut.io.operand2.poke(4.U)
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
      dut.io.operand1.poke(0x80000000L.U)  // Most significant bit is 1
      dut.io.operand2.poke(4.U)
      dut.clock.step(1)
      dut.io.result.expect(0xF8000000L.U)  // Should sign-extend
    }
  }
} 