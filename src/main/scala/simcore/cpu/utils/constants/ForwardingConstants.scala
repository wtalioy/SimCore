package simcore.cpu.utils.constants

import chisel3._
import chisel3.util._

/**
 * Forwarding unit constants
 */
object ForwardingSelects {
  val NO_FORWARD = 0.U(3.W)          // No forwarding needed
  val FORWARD_FROM_EX = 1.U(3.W)     // Forward from EX stage (ALU result)
  val FORWARD_FROM_MEM = 2.U(3.W)    // Forward from MEM stage
  val FORWARD_FROM_WB = 3.U(3.W)     // Forward from WB stage
  val FORWARD_FROM_BRANCH = 4.U(3.W) // Forward from branch unit result
} 