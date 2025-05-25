package simcore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.components.RegFile

/**
 * Comprehensive test for the RegFile component
 * Combines basic and advanced test cases for the parameterized register file
 */
class RegFileComprehensiveTest extends AnyFlatSpec with ChiselSim { 
  behavior of "RegFile Basic Functionality"

  it should "initialize with zeroes" in {
    // Default RegFile with 32 registers, 32-bit width
    simulate(new RegFile()) { dut =>
      // Set all registers to zero first
      for (i <- 1 until 32) {
        dut.io.waddr(0).poke(i.U)
        dut.io.wdata(0).poke(0.U)
        dut.io.wen(0).poke(true.B)
        dut.clock.step(1)
      }
      
      dut.io.wen(0).poke(false.B)
      dut.clock.step(1)
      
      // Now test that registers read as zero
      for (i <- Seq(0, 1, 5, 10, 15, 31)) {
        dut.io.raddr(0).poke(i.U)
        dut.clock.step(1)
        dut.io.rdata(0).expect(0.U)
      }
    }
  }

  it should "write and read from all registers except for r0" in {
    simulate(new RegFile()) { dut =>
      // Test writing to and reading from registers 1-31
      for (i <- 1 until 32) {
        val testValue = (i * 0x10001).U // Unique value for each register
        
        // Write to register i
        dut.io.waddr(0).poke(i.U)
        dut.io.wdata(0).poke(testValue)
        dut.io.wen(0).poke(true.B)
        dut.clock.step(1)
        
        // Turn off write enable
        dut.io.wen(0).poke(false.B)
        
        // Read from register i and verify its value
        dut.io.raddr(0).poke(i.U)
        dut.clock.step(1)
        dut.io.rdata(0).expect(testValue)
      }
    }
  }

  it should "guarantee that register 0 is always zero" in {
    simulate(new RegFile()) { dut =>
      // Try writing a non-zero value to r0
      dut.io.waddr(0).poke(0.U)
      dut.io.wdata(0).poke(0xDEADBEEFL.U)
      dut.io.wen(0).poke(true.B)
      dut.clock.step(1)
      
      // Verify r0 is still zero after attempted write
      dut.io.raddr(0).poke(0.U)
      dut.io.wen(0).poke(false.B)
      dut.clock.step(1)
      dut.io.rdata(0).expect(0.U)
    }
  }

  it should "support simultaneous reads from different registers" in {
    simulate(new RegFile()) { dut =>
      // Write to two different registers
      dut.io.waddr(0).poke(1.U)
      dut.io.wdata(0).poke(0x12345678L.U)
      dut.io.wen(0).poke(true.B)
      dut.clock.step(1)
      
      dut.io.waddr(0).poke(2.U)
      dut.io.wdata(0).poke(0x87654321L.U)
      dut.io.wen(0).poke(true.B)
      dut.clock.step(1)
      
      // Turn off write enable
      dut.io.wen(0).poke(false.B)
      
      // Read from both registers simultaneously
      dut.io.raddr(0).poke(1.U)
      dut.io.raddr(1).poke(2.U)
      dut.clock.step(1)
      dut.io.rdata(0).expect(0x12345678L.U)
      dut.io.rdata(1).expect(0x87654321L.U)
    }
  }

  behavior of "RegFile Advanced Features"

  it should "support custom data width" in {
    simulate(new RegFile(dataBits = 64, regCount = 16, readPorts = 2, writePorts = 1)) { dut =>
      // Write a 64-bit value to register 5
      val testValue = BigInt("DEADBEEFCAFEBABE", 16)
      dut.io.waddr(0).poke(5.U)
      dut.io.wdata(0).poke(testValue.U)
      dut.io.wen(0).poke(true.B)
      dut.clock.step(1)
      
      // Read back the value
      dut.io.wen(0).poke(false.B)
      dut.io.raddr(0).poke(5.U)
      dut.clock.step(1)
      
      // Verify the value
      dut.io.rdata(0).expect(testValue.U)
    }
  }

  it should "support multiple read and write ports" in {
    simulate(new RegFile(dataBits = 32, regCount = 32, readPorts = 3, writePorts = 2)) { dut =>
      // Write values to different registers through different write ports
      dut.io.waddr(0).poke(1.U)
      dut.io.wdata(0).poke(0x11111111L.U)
      dut.io.wen(0).poke(true.B)
      
      dut.io.waddr(1).poke(2.U)
      dut.io.wdata(1).poke(0x22222222L.U)
      dut.io.wen(1).poke(true.B)
      
      dut.clock.step(1)
      
      // Turn off write enable
      dut.io.wen(0).poke(false.B)
      dut.io.wen(1).poke(false.B)
      
      // Read from all read ports
      dut.io.raddr(0).poke(1.U)
      dut.io.raddr(1).poke(2.U)
      dut.io.raddr(2).poke(0.U) // Register 0 should be 0
      
      dut.clock.step(1)
      
      // Verify values
      dut.io.rdata(0).expect(0x11111111L.U)
      dut.io.rdata(1).expect(0x22222222L.U)
      dut.io.rdata(2).expect(0.U)
    }
  }

  it should "prioritize lower write ports when writing to the same register" in {
    simulate(new RegFile(dataBits = 32, regCount = 32, readPorts = 1, writePorts = 2)) { dut =>
      // Write to the same register from both write ports
      dut.io.waddr(0).poke(5.U)
      dut.io.wdata(0).poke(0xAAAAAAAAL.U) // This should win
      dut.io.wen(0).poke(true.B)
      
      dut.io.waddr(1).poke(5.U)
      dut.io.wdata(1).poke(0xBBBBBBBBL.U) // This should be ignored
      dut.io.wen(1).poke(true.B)
      
      dut.clock.step(1)
      
      // Turn off write enable
      dut.io.wen(0).poke(false.B)
      dut.io.wen(1).poke(false.B)
      
      // Read the register
      dut.io.raddr(0).poke(5.U)
      dut.clock.step(1)
      
      // Verify that the value from write port 0 was used
      dut.io.rdata(0).expect(0xAAAAAAAAL.U)
    }
  }

  it should "support RISC-V specific configuration" in {
    simulate(new RegFile(dataBits = 32, regCount = 32, readPorts = 2, writePorts = 1)) { dut =>
      // Typical RISC-V usage pattern - write data to a register
      val testAddr = 10.U
      val testData = 0xABCDEF00L.U
      
      dut.io.waddr(0).poke(testAddr)
      dut.io.wdata(0).poke(testData)
      dut.io.wen(0).poke(true.B)
      dut.clock.step(1)
      
      // Turn off write and read back
      dut.io.wen(0).poke(false.B)
      dut.io.raddr(0).poke(testAddr)
      dut.clock.step(1)
      
      // Verify
      dut.io.rdata(0).expect(testData)
      
      // Also test a typical instruction pattern (reading two registers simultaneously)
      val testAddr2 = 15.U
      val testData2 = 0x12345678L.U
      
      // Write to second register
      dut.io.waddr(0).poke(testAddr2)
      dut.io.wdata(0).poke(testData2)
      dut.io.wen(0).poke(true.B)
      dut.clock.step(1)
      
      // Read both registers (simulating reading operands for an instruction)
      dut.io.wen(0).poke(false.B)
      dut.io.raddr(0).poke(testAddr)
      dut.io.raddr(1).poke(testAddr2)
      dut.clock.step(1)
      
      // Verify both values
      dut.io.rdata(0).expect(testData)
      dut.io.rdata(1).expect(testData2)
    }
  }

  it should "handle high-volume writes and reads" in {
    simulate(new RegFile(dataBits = 32, regCount = 32, readPorts = 2, writePorts = 1)) { dut =>
      // Write to all registers
      for (i <- 1 until 32) {
        dut.io.waddr(0).poke(i.U)
        dut.io.wdata(0).poke((i * 0x1000 + 0xAB).U)
        dut.io.wen(0).poke(true.B)
        dut.clock.step(1)
      }
      
      dut.io.wen(0).poke(false.B)
      dut.clock.step(1)
      
      // Read back all registers in random order to verify independence
      val shuffled = scala.util.Random.shuffle((1 until 32).toList)
      for (i <- shuffled) {
        dut.io.raddr(0).poke(i.U)
        dut.clock.step(1)
        dut.io.rdata(0).expect((i * 0x1000 + 0xAB).U)
      }
    }
  }
} 