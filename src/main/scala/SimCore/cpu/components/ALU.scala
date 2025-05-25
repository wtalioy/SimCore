package SimCore.cpu.components

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.ALUOps

/** Arithmetic Logic Unit Performs the core arithmetic and logical operations
  * for the CPU
  */
class ALUUnit extends Module {
  val io = IO(new Bundle {
    val alu_op = Input(UInt(4.W))
    val operand1 = Input(UInt(32.W))
    val operand2 = Input(UInt(32.W))
    val result = Output(UInt(32.W))
  })

  // Default value
  val resultWire = Wire(UInt(32.W))
  resultWire := 0.U

  // ALU operation implementation
  switch(io.alu_op) {
    // Basic arithmetic/logic operations
    is(ALUOps.ADD) { resultWire := io.operand1 + io.operand2 }
    is(ALUOps.SUB) { resultWire := io.operand1 - io.operand2 }
    is(ALUOps.SLL) { resultWire := io.operand1 << io.operand2(4, 0) }
    is(ALUOps.SLT) {
      resultWire := Mux(io.operand1.asSInt < io.operand2.asSInt, 1.U, 0.U)
    }
    is(ALUOps.SLTU) { resultWire := Mux(io.operand1 < io.operand2, 1.U, 0.U) }
    is(ALUOps.XOR) { resultWire := io.operand1 ^ io.operand2 }
    is(ALUOps.SRL) { resultWire := io.operand1 >> io.operand2(4, 0) }
    is(ALUOps.SRA) {
      resultWire := (io.operand1.asSInt >> io.operand2(4, 0)).asUInt
    }
    is(ALUOps.OR) { resultWire := io.operand1 | io.operand2 }
    is(ALUOps.AND) { resultWire := io.operand1 & io.operand2 }
    is(ALUOps.NOR) { resultWire := ~(io.operand1 | io.operand2) }
    is(ALUOps.COPY_A) { resultWire := io.operand1 }
    is(ALUOps.COPY_B) { resultWire := io.operand2 }
    is(ALUOps.LUI) { resultWire := Cat(io.operand2(15, 0), 0.U(16.W)) }
  }

  // Connect to output
  io.result := resultWire
}
