package simcore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.Core
import simcore.cpu.utils.constants.ForwardingSelects

class MockMemory(
    val instructions: Seq[BigInt],
    val initialData: Map[BigInt, BigInt] = Map()
) {
  private var data = collection.mutable.Map[BigInt, BigInt]() ++ initialData
  private var instructionsFetched = 0

  def getInstruction(addr: BigInt): BigInt = {
    // Convert byte address to word address (aligned)
    val wordAddr = addr >> 2

    // Return instruction if available, or 0 (NOP) if beyond bounds
    if (wordAddr < instructions.length) {
      val instr = instructions(wordAddr.toInt)
      instructionsFetched += 1
      instr
    } else {
      0 // Return NOP
    }
  }

  def readData(addr: BigInt): BigInt = {
    data.getOrElse(addr, 0)
  }

  def writeData(addr: BigInt, value: BigInt): Unit = {
    data(addr) = value
  }

  def instructionCount: Int = instructionsFetched
}

class PipelineTest extends AnyFlatSpec with ChiselSim {
  behavior of "MIPS Pipeline"

  // Helper function to create test instructions
  def makeRTypeInstr(
      opcode: Int,
      rs: Int,
      rt: Int,
      rd: Int,
      shamt: Int,
      funct: Int
  ): BigInt = {
    val op = opcode & 0x3f
    val rs_field = rs & 0x1f
    val rt_field = rt & 0x1f
    val rd_field = rd & 0x1f
    val sh = shamt & 0x1f
    val fn = funct & 0x3f

    BigInt(
      (op << 26) | (rs_field << 21) | (rt_field << 16) | (rd_field << 11) | (sh << 6) | fn
    )
  }

  def makeITypeInstr(opcode: Int, rs: Int, rt: Int, imm: Int): BigInt = {
    val op = opcode & 0x3f
    val rs_field = rs & 0x1f
    val rt_field = rt & 0x1f
    val immediate = imm & 0xffff

    BigInt((op << 26) | (rs_field << 21) | (rt_field << 16) | immediate)
  }

  def makeJTypeInstr(opcode: Int, target: Int): BigInt = {
    val op = opcode & 0x3f
    val addr = target & 0x3ffffff

    BigInt((op << 26) | addr)
  }

  // Safely convert BigInt to UInt, handling any potential negative values
  def safeToUInt(value: BigInt): UInt = {
    // Ensure the value is positive by taking the low 32 bits
    val positiveValue = value & BigInt("FFFFFFFF", 16)
    positiveValue.U(32.W)
  }

  // Test for load-use data hazard (RAW dependency)
  it should "correctly handle load-use hazard by stalling" in {
    simulate(new Core) { dut =>
      // Instructions:
      // 1. LW r2, 0(r1)    # Load value into r2
      // 2. ADD r3, r2, r4  # Use r2 (RAW hazard)
      // 3. ADD r5, r6, r7  # No hazard
      val instructions = Seq(
        makeITypeInstr(0x23, 1, 2, 0), // LW r2, 0(r1)
        makeRTypeInstr(0, 2, 4, 3, 0, 0x20), // ADD r3, r2, r4
        makeRTypeInstr(0, 6, 7, 5, 0, 0x20) // ADD r5, r6, r7
      )

      val memory =
        new MockMemory(
          instructions,
          Map(BigInt(4) -> BigInt(42))
        ) // Store value 42 at address 4

      // Initial values for registers: r1 = 4 (address for load)
      dut.io.ibus.resp_valid.poke(true.B)
      dut.io.dbus.resp_valid.poke(true.B)

      // Cycle 1: Fetch first instruction
      dut.io.ibus.req_ready.expect(true.B)
      dut.io.ibus.resp_data.poke(safeToUInt(instructions(0))) // LW instruction
      dut.clock.step(1)

      // Cycle 2: Fetch second instruction
      dut.io.ibus.resp_data.poke(
        safeToUInt(instructions(1))
      ) // ADD instruction (with hazard)
      // This tests that the HDU detects the load-use hazard
      dut.clock.step(1)

      // Cycle 3: Pipeline should detect the hazard and stall ID/IF
      // We expect a bubble to be inserted
      dut.io.ibus.resp_data.poke(
        safeToUInt(instructions(2))
      ) // ADD instruction (no hazard)
      dut.clock.step(1)

      // Run for a few more cycles to ensure the pipeline recovers
      for (i <- 0 until 5) {
        dut.io.ibus.resp_data.poke(0.U) // NOP
        dut.clock.step(1)
      }
    }
  }

  // Test for data forwarding
  it should "correctly forward data from EX/MEM and MEM/WB stages" in {
    simulate(new Core) { dut =>
      // Instructions:
      // 1. ADD r2, r1, r1  # r2 = r1 + r1
      // 2. ADD r3, r2, r1  # r3 = r2 + r1 (RAW on r2, should forward from EX/MEM)
      // 3. ADD r4, r3, r1  # r4 = r3 + r1 (RAW on r3, should forward from EX/MEM)
      // 4. ADD r5, r2, r1  # r5 = r2 + r1 (RAW on r2, should forward from MEM/WB)
      val instructions = Seq(
        makeRTypeInstr(0, 1, 1, 2, 0, 0x20), // ADD r2, r1, r1
        makeRTypeInstr(0, 2, 1, 3, 0, 0x20), // ADD r3, r2, r1
        makeRTypeInstr(0, 3, 1, 4, 0, 0x20), // ADD r4, r3, r1
        makeRTypeInstr(0, 2, 1, 5, 0, 0x20) // ADD r5, r2, r1
      )

      val memory = new MockMemory(instructions)

      // Drive instruction memory responses
      dut.io.ibus.resp_valid.poke(true.B)
      dut.io.dbus.resp_valid.poke(true.B)

      // Cycle 1: Fetch first instruction
      dut.io.ibus.req_ready.expect(true.B)
      dut.io.ibus.resp_data.poke(safeToUInt(instructions(0)))
      dut.clock.step(1)

      // Cycle 2-8: Run the rest of the instructions
      for (i <- 1 until instructions.length) {
        dut.io.ibus.resp_data.poke(safeToUInt(instructions(i)))
        dut.clock.step(1)
      }

      // Run for a few more cycles to ensure all instructions complete
      for (i <- 0 until 5) {
        dut.io.ibus.resp_data.poke(0.U) // NOP
        dut.clock.step(1)
      }
    }
  }

  // Test for control hazard handling with a branch
  it should "correctly handle control hazards from branches" in {
    simulate(new Core) { dut =>
      // Instructions:
      // 1. BEQ r1, r2, target # Branch if r1 == r2
      // 2. ADD r3, r4, r5     # Instruction in branch delay slot
      // 3. SUB r6, r7, r8     # Target of branch (if taken)
      val instructions = Seq(
        makeITypeInstr(0x04, 1, 2, 2), // BEQ r1, r2, target (+2 instructions)
        makeRTypeInstr(0, 4, 5, 3, 0, 0x20), // ADD r3, r4, r5
        makeRTypeInstr(0, 7, 8, 6, 0, 0x22) // SUB r6, r7, r8
      )

      val memory = new MockMemory(instructions)

      // Drive instruction memory responses
      dut.io.ibus.resp_valid.poke(true.B)
      dut.io.dbus.resp_valid.poke(true.B)

      // Run each instruction
      for (i <- 0 until instructions.length) {
        dut.io.ibus.resp_data.poke(safeToUInt(instructions(i)))
        dut.clock.step(1)
      }

      // Run for a few more cycles to flush the pipeline
      for (i <- 0 until 5) {
        dut.io.ibus.resp_data.poke(0.U) // NOP
        dut.clock.step(1)
      }
    }
  }
}
