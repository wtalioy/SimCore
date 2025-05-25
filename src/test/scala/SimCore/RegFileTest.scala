package SimCore

import chisel3._
import chisel3.util._
// import chiseltest._ // Removed
import chisel3.simulator.scalatest.ChiselSim // Added
import org.scalatest.flatspec.AnyFlatSpec
import SimCore.cpu.components.RegFile

class RegFileTest extends AnyFlatSpec with ChiselSim { // Updated
  behavior of "RegFile"

  it should "initialize with zeroes" in {
    simulate(new RegFile()) { dut => // Updated
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
    simulate(new RegFile()) { dut => // Updated
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
    simulate(new RegFile()) { dut => // Updated
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
    simulate(new RegFile()) { dut => // Updated
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
} 