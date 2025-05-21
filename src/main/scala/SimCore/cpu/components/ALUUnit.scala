package SimCore.cpu.components

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.ALUOps // Corrected import for ALUOps

// Define ALU operations if not globally defined
// object ALUOps { ... } // Removed this definition

class ALUUnit extends Module {
  val io = IO(new Bundle {
    val alu_op  = Input(UInt(4.W))
    val operand1 = Input(UInt(32.W))
    val operand2 = Input(UInt(32.W))
    val result   = Output(UInt(32.W))
    // val zero     = Output(Bool()) // For branches
  })

  io.result := 0.U // Default

  switch(io.alu_op) {
    is(ALUOps.ADD)  { io.result := io.operand1 + io.operand2 }
    is(ALUOps.SUB)  { io.result := io.operand1 - io.operand2 }
    is(ALUOps.SLL)  { io.result := io.operand1 << io.operand2(4,0) }
    is(ALUOps.SLT)  { io.result := (io.operand1.asSInt < io.operand2.asSInt).asUInt }
    is(ALUOps.SLTU) { io.result := (io.operand1 < io.operand2).asUInt }
    is(ALUOps.XOR)  { io.result := io.operand1 ^ io.operand2 }
    is(ALUOps.SRL)  { io.result := io.operand1 >> io.operand2(4,0) }
    is(ALUOps.SRA)  { io.result := (io.operand1.asSInt >> io.operand2(4,0)).asUInt }
    is(ALUOps.OR)   { io.result := io.operand1 | io.operand2 }
    is(ALUOps.AND)  { io.result := io.operand1 & io.operand2 }
    is(ALUOps.COPY_A){ io.result := io.operand1 } // For address calculation, JALR
    is(ALUOps.COPY_B){ io.result := io.operand2 } // For LUI
    is(ALUOps.NOR)  { io.result := ~(io.operand1 | io.operand2) } // Added NOR logic
  }

  // io.zero := (io.result === 0.U)
} 