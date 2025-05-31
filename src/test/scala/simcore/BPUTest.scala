package simcore

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import simcore.cpu.components._
import simcore.cpu.utils.constants.BPTypes
import simcore.cpu.utils.isPowerOf2

class BPUTest extends AnyFlatSpec with ChiselSim {
  val XLEN = 32
  val ENTRY_NUM = 8
  val TAG_BITS = 8
  val INSTR_BYTES = 4

  behavior of "Branch Prediction Unit"

  it should "initialize with no predictions taken" in {
    simulate(new BTB(ENTRY_NUM, XLEN, TAG_BITS)) { dut =>
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      dut.io.pred.out.hit.expect(false.B)
    }
  }

  it should "predict and update unconditional jumps correctly" in {
    simulate(new BTB(ENTRY_NUM, XLEN, TAG_BITS)) { dut =>
      // First encounter of jump - should not predict taken
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      dut.io.pred.out.hit.expect(false.B)
      
      // Update the BTB with a jump instruction
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.jump)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x2000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(false.B)
      dut.clock.step(1)
      
      // Reset update signals
      dut.io.update.valid.poke(false.B)
      
      // Now predict for the same PC - should predict taken
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      dut.io.pred.out.hit.expect(true.B)
      dut.io.pred.out.target.expect(0x2000.U)
    }
  }

  it should "predict and update conditional branches correctly" in {
    simulate(new BTB(ENTRY_NUM, XLEN, TAG_BITS)) { dut =>
      // First encounter of branch - should not predict taken
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      dut.io.pred.out.hit.expect(false.B)
      
      // Update the BTB with a conditional branch instruction (taken)
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x1020.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(false.B)
      dut.clock.step(1)
      
      // Reset update signals
      dut.io.update.valid.poke(false.B)
      
      // Now predict for the same PC - should predict taken (initialized to weakly taken)
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      dut.io.pred.out.hit.expect(true.B)
      dut.io.pred.out.target.expect(0x1020.U)
      
      // Update counter when branch is not taken
      val hit_index = dut.io.pred.out.index.peek()
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x1020.U)
      dut.io.update.taken.poke(false.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // Update counter again when branch is not taken
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x1020.U)
      dut.io.update.taken.poke(false.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // Reset update signals
      dut.io.update.valid.poke(false.B)
      
      // Now predict again - should predict not taken after two consecutive not-taken updates
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      dut.io.pred.out.hit.expect(true.B)
    }
  }

  it should "handle multiple entries correctly" in {
    simulate(new BTB(ENTRY_NUM, XLEN, TAG_BITS)) { dut =>
      // Add first entry
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.jump)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x2000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(false.B)
      dut.clock.step(1)
      
      // Add second entry
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.jump)
      dut.io.update.pc.poke(0x1100.U)
      dut.io.update.target.poke(0x2100.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(false.B)
      dut.clock.step(1)
      
      // Reset update signals
      dut.io.update.valid.poke(false.B)
      
      // Check first entry
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      dut.io.pred.out.hit.expect(true.B)
      dut.io.pred.out.target.expect(0x2000.U)
      
      // Check second entry
      dut.io.pred.in.pc.poke(0x1100.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      dut.io.pred.out.hit.expect(true.B)
      dut.io.pred.out.target.expect(0x2100.U)
      
      // Check non-existent entry
      dut.io.pred.in.pc.poke(0x1200.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      dut.io.pred.out.hit.expect(false.B)
    }
  }

  it should "correctly update existing entries" in {
    simulate(new BTB(ENTRY_NUM, XLEN, TAG_BITS)) { dut =>
      // Add an entry
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.jump)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x2000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(false.B)
      dut.clock.step(1)
      
      // Reset update signals
      dut.io.update.valid.poke(false.B)
      
      // Verify entry
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      dut.io.pred.out.hit.expect(true.B)
      dut.io.pred.out.target.expect(0x2000.U)
      
      val hit_index = dut.io.pred.out.index.peek()
      
      // Update the same entry with a new target
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.jump)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x3000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // Reset update signals
      dut.io.update.valid.poke(false.B)
      
      // Verify updated entry
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      dut.io.pred.out.hit.expect(true.B)
      dut.io.pred.out.target.expect(0x3000.U)
    }
  }

  it should "demonstrate saturating counter behavior" in {
    simulate(new BTB(ENTRY_NUM, XLEN, TAG_BITS)) { dut =>
      // Add a conditional branch entry
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.target.poke(0x1020.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(false.B)
      dut.clock.step(1)
      
      // Get the index
      dut.io.update.valid.poke(false.B)
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      val hit_index = dut.io.pred.out.index.peek()
      dut.io.pred.out.taken.expect(true.B)
      
      // Series of updates to demonstrate counter behavior
      
      // 1. Update not taken - should go from weakly taken (10) to weakly not taken (01)
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.taken.poke(false.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // Check prediction - should still be not taken
      dut.io.update.valid.poke(false.B)
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      
      // 2. Update not taken again - should go to strongly not taken (00)
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.taken.poke(false.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // 3. Update taken - should go to weakly not taken (01)
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // Check prediction - should still be not taken
      dut.io.update.valid.poke(false.B)
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(false.B)
      
      // 4. Update taken again - should go to weakly taken (10)
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
      
      // Check prediction - should now be taken
      dut.io.update.valid.poke(false.B)
      dut.io.pred.in.pc.poke(0x1000.U)
      dut.clock.step(1)
      dut.io.pred.out.taken.expect(true.B)
      
      // 5. Update taken again - should go to strongly taken (11)
      dut.io.update.valid.poke(true.B)
      dut.io.update.bp_type.poke(BPTypes.cond)
      dut.io.update.pc.poke(0x1000.U)
      dut.io.update.taken.poke(true.B)
      dut.io.update.hit.poke(true.B)
      dut.io.update.index.poke(hit_index)
      dut.clock.step(1)
    }
  }
} 