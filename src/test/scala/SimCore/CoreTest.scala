package SimCore

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CoreTest extends AnyFlatSpec with chiseltest.ChiselScalatestTester {
  behavior of "Core"

  it should "instantiate without errors" in {
    test(new SimCore.cpu.Core) { dut =>
      // Just test that the core can be instantiated
      dut.clock.step(1)
    }
  }
} 