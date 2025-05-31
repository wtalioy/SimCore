package simcore.cpu.utils.constants

import chisel3._
import chisel3.util._

/**
 * PC next selection values
 */
object PCNextSelVals {
  val PC_PLUS_4   = 0.U(2.W)
  val BRANCH      = 1.U(2.W)
  val JUMP_JR     = 2.U(2.W)
  val JUMP_J_JAL  = 3.U(2.W)
}

/**
 * Branch type constants
 */
object BranchTypes {
  val NONE = 0.U(3.W)
  val BEQ  = 1.U(3.W)
  val BNE  = 2.U(3.W)
  val BLT  = 3.U(3.W)
  val BGE  = 4.U(3.W)
  val BLTU = 5.U(3.W)
  val BGEU = 6.U(3.W)
  // Single-operand branches (comparison with zero)
  val BGEZ = 7.U(4.W)  // Branch if rs >= 0
  val BGTZ = 8.U(4.W)  // Branch if rs > 0
  val BLEZ = 9.U(4.W)  // Branch if rs <= 0
  val BLTZ = 10.U(4.W) // Branch if rs < 0
  // JAL and JALR removed as they are handled by jump logic
} 