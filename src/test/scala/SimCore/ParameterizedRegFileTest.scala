package SimCore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import SimCore.cpu.components.RegFile

class RegFileAdvancedTest extends AnyFlatSpec with ChiselSim {
  behavior of "RegFile with advanced configurations"

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
} 