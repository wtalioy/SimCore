package SimCore.cpu.utils

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
}

object BranchTypes {
  val BEQ  = 0.U(3.W)
  val BNE  = 1.U(3.W)
  val BLT  = 2.U(3.W) // Was 4.U, re-numbering for compactness
  val BGE  = 3.U(3.W) // Was 5.U
  val BLTU = 4.U(3.W) // Was 6.U
  val BGEU = 5.U(3.W) // Was 7.U
  // JAL and JALR removed as they are handled by jump logic
}