package simcore.cpu.utils.constants

import chisel3._
import chisel3.util._

/**
 * Branch Prediction Type constants
 */
object BPTypes {
  val dontcare = "??"
  val jump     = "01" // unconditional jump
  val cond     = "10" // conditional jump
  def apply() = UInt(2.W)
}