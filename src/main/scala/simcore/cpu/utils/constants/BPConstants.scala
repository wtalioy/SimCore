package simcore.cpu.utils.constants

import chisel3._
import chisel3.util._

/**
 * Branch Prediction Type constants
 */
object BPTypes {
  val dontcare = 0.U(2.W)
  val jump     = 1.U(2.W) // unconditional jump
  val cond     = 2.U(2.W) // conditional jump
  def apply() = UInt(2.W)
}