package SimCore

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SimCore.cpu.components.RegFile

class RegFileTest extends AnyFlatSpec with chiseltest.ChiselScalatestTester {
  behavior of "RegFile"

  it should "initialize with zeroes" in {
    test(new RegFile) { dut =>
      // Test a few random registers to verify they start with zero
      for (i <- Seq(0, 1, 5, 10, 15, 31)) {
        dut.io.rs1_addr.poke(i.U)
        dut.clock.step(1)
        dut.io.rs1_data.expect(0.U)
      }
    }
  }

  it should "write and read from all registers except for r0" in {
    test(new RegFile) { dut =>
      // Test writing to and reading from registers 1-31
      for (i <- 1 until 32) {
        val testValue = (i * 0x10001).U // Unique value for each register
        
        // Write to register i
        dut.io.rd_addr.poke(i.U)
        dut.io.rd_data.poke(testValue)
        dut.io.wen.poke(true.B)
        dut.clock.step(1)
        
        // Turn off write enable
        dut.io.wen.poke(false.B)
        
        // Read from register i and verify its value
        dut.io.rs1_addr.poke(i.U)
        dut.clock.step(1)
        dut.io.rs1_data.expect(testValue)
      }
    }
  }

  it should "guarantee that register 0 is always zero" in {
    test(new RegFile) { dut =>
      // Try writing a non-zero value to r0
      dut.io.rd_addr.poke(0.U)
      dut.io.rd_data.poke(0xDEADBEEFL.U)
      dut.io.wen.poke(true.B)
      dut.clock.step(1)
      
      // Verify r0 is still zero after attempted write
      dut.io.rs1_addr.poke(0.U)
      dut.io.wen.poke(false.B)
      dut.clock.step(1)
      dut.io.rs1_data.expect(0.U)
    }
  }

  it should "support simultaneous reads from different registers" in {
    test(new RegFile) { dut =>
      // Write to two different registers
      dut.io.rd_addr.poke(1.U)
      dut.io.rd_data.poke(0x12345678L.U)
      dut.io.wen.poke(true.B)
      dut.clock.step(1)
      
      dut.io.rd_addr.poke(2.U)
      dut.io.rd_data.poke(0x87654321L.U)
      dut.io.wen.poke(true.B)
      dut.clock.step(1)
      
      // Turn off write enable
      dut.io.wen.poke(false.B)
      
      // Read from both registers simultaneously
      dut.io.rs1_addr.poke(1.U)
      dut.io.rs2_addr.poke(2.U)
      dut.clock.step(1)
      dut.io.rs1_data.expect(0x12345678L.U)
      dut.io.rs2_data.expect(0x87654321L.U)
    }
  }
} 