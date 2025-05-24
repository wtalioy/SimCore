package SimCore.cpu.stages

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.IBusBundle

class IFU extends Module {
  val io = IO(new Bundle {
    val ibus = Flipped(
      new IBusBundle()
    ) // IFU is slave to Core's IBus signals, master to IMem
    // For direct connection to memory, this would be new IBusBundle()
    // Let's assume Core passes IBus signals to IFU, IFU uses them

    val redirect_target = Input(UInt(32.W))
    val redirect_valid = Input(Bool())
    val stall = Input(Bool())

    val uop_pc = Output(UInt(32.W))
    val uop_instr = Output(UInt(32.W))
    val valid_out = Output(Bool())
  })

  // Internal logic for IFU
  val pc = RegInit(0.U(32.W)) // Program Counter
  val vpc_reg = RegInit(0.U(32.W))
  val instr_reg = RegInit(0.U(32.W))
  val valid_reg = RegInit(false.B)

  // Request logic wire
  val req_ready_wire = Wire(Bool())
  req_ready_wire := false.B

  // Connect the wire to the output port
  io.ibus.req_ready := req_ready_wire
  io.ibus.req_addr := pc

  when(!io.stall) {
    when(io.redirect_valid) {
      pc := io.redirect_target
      vpc_reg := io.redirect_target
      valid_reg := false.B
    }.otherwise {
      req_ready_wire := true.B
      when(io.ibus.resp_valid) { // Instruction received from memory
        instr_reg := io.ibus.resp_data
        vpc_reg := pc
        pc := pc + 4.U
        valid_reg := true.B
      }.otherwise {
        valid_reg := false.B
      }
    }
  }.otherwise {
    req_ready_wire := false.B
  }

  io.uop_pc := vpc_reg
  io.uop_instr := instr_reg
  io.valid_out := valid_reg
}
