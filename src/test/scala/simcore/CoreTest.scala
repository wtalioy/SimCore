package simcore

import chisel3._
import chisel3.util._
// import chiseltest._ // Removed
import chisel3.simulator.scalatest.ChiselSim // Added
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.Core

class CoreTest extends AnyFlatSpec with ChiselSim { // Updated
  behavior of "Core"

  it should "instantiate without errors" in {
    simulate(new simcore.cpu.Core) { dut => // Updated
      // Just test that the core can be instantiated
      dut.clock.step(1)
    }
  }
} 