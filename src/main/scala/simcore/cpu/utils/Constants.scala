package simcore.cpu.utils

import chisel3._
import chisel3.util._

object PCNextSelVals {
  val PC_PLUS_4   = 0.U(2.W)
  val BRANCH      = 1.U(2.W)
  val JUMP_JR     = 2.U(2.W)
  val JUMP_J_JAL  = 3.U(2.W)
}

object ALUOps {
  val ADD    = 0.U(4.W)
  val SUB    = 1.U(4.W)
  val SLL    = 2.U(4.W)
  val SLT    = 3.U(4.W)
  val SLTU   = 4.U(4.W)
  val XOR    = 5.U(4.W)
  val SRL    = 6.U(4.W)
  val SRA    = 7.U(4.W)
  val OR     = 8.U(4.W)
  val AND    = 9.U(4.W)
  val COPY_A = 10.U(4.W)
  val COPY_B = 11.U(4.W)
  val NOR    = 12.U(4.W)
  val LUI    = 13.U(4.W)
}

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

// Forwarding Unit constants
object ForwardingSelects {
  val NO_FORWARD = 0.U(2.W)
  val FORWARD_FROM_MEM = 1.U(2.W) 
  val FORWARD_FROM_WB = 2.U(2.W)
} 