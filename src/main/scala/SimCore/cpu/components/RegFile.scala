package SimCore.cpu.components

import chisel3._
import chisel3.util._

class RegFile extends Module {
  val io = IO(new Bundle {
    // Read Port 1
    val rs1_addr = Input(UInt(5.W))
    val rs1_data = Output(UInt(32.W))
    // Read Port 2
    val rs2_addr = Input(UInt(5.W))
    val rs2_data = Output(UInt(32.W))

    // Write Port
    val rd_addr  = Input(UInt(5.W))
    val rd_data  = Input(UInt(32.W))
    val wen      = Input(Bool()) // Write enable
  })

  // 32 registers, 32-bits each. Register 0 is hardwired to 0.
  val regs = Reg(Vec(32, UInt(32.W)))

  // Read logic (combinational)
  // Handle read-before-write and x0 behavior
  io.rs1_data := Mux(io.rs1_addr === 0.U, 0.U, regs(io.rs1_addr))
  io.rs2_data := Mux(io.rs2_addr === 0.U, 0.U, regs(io.rs2_addr))

  // Write logic (synchronous)
  when(io.wen && io.rd_addr =/= 0.U) {
    regs(io.rd_addr) := io.rd_data
  }
} 