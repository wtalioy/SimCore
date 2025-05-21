package SimCore.cpu.components

import chisel3._
import chisel3.util._

object ForwardingSelect extends ChiselEnum {
  val NO_FWD, FWD_FROM_EXMEM, FWD_FROM_MEMWB = Value
}

class HazardDetectionUnitIO extends Bundle {
  // Inputs from ID Stage (current instruction being decoded)
  val id_rs_addr = Input(UInt(5.W))
  val id_rt_addr = Input(UInt(5.W))
  val id_reads_rs = Input(Bool()) // To check if rs is actually a source operand
  val id_reads_rt = Input(Bool()) // To check if rt is actually a source operand

  // Inputs from ID/EX Register (instruction currently in EX stage)
  val ex_rd_addr = Input(UInt(5.W))
  val ex_reg_write_en = Input(Bool())
  val ex_is_load = Input(Bool()) // True if instruction in EX is a load

  // Inputs from EX/MEM Register (instruction currently in MEM stage)
  val mem_rd_addr = Input(UInt(5.W))
  val mem_reg_write_en = Input(Bool())

  // Outputs for forwarding muxes in EX stage
  val forward_A_select = Output(ForwardingSelect())
  val forward_B_select = Output(ForwardingSelect())

  // Output for stall logic
  val load_use_hazard_stall = Output(Bool()) // Stall PC and IF/ID, insert bubble in ID/EX
}

class HazardDetectionUnit extends Module {
  val io = IO(new HazardDetectionUnitIO())

  // Default: No forwarding, no stall
  io.forward_A_select := ForwardingSelect.NO_FWD
  io.forward_B_select := ForwardingSelect.NO_FWD
  io.load_use_hazard_stall := false.B

  // --- Forwarding Logic for Operand A (rs) ---
  when(io.ex_reg_write_en && io.ex_rd_addr =/= 0.U && (io.ex_rd_addr === io.id_rs_addr) && io.id_reads_rs) {
    io.forward_A_select := ForwardingSelect.FWD_FROM_EXMEM
  }.elsewhen(io.mem_reg_write_en && io.mem_rd_addr =/= 0.U && (io.mem_rd_addr === io.id_rs_addr) && io.id_reads_rs) {
    io.forward_A_select := ForwardingSelect.FWD_FROM_MEMWB
  }

  // --- Forwarding Logic for Operand B (rt) ---
  when(io.ex_reg_write_en && io.ex_rd_addr =/= 0.U && (io.ex_rd_addr === io.id_rt_addr) && io.id_reads_rt) {
    io.forward_B_select := ForwardingSelect.FWD_FROM_EXMEM
  }.elsewhen(io.mem_reg_write_en && io.mem_rd_addr =/= 0.U && (io.mem_rd_addr === io.id_rt_addr) && io.id_reads_rt) {
    io.forward_B_select := ForwardingSelect.FWD_FROM_MEMWB
  }

  // --- Load-Use Hazard Detection ---
  when(io.ex_is_load && io.ex_reg_write_en && io.ex_rd_addr =/= 0.U &&
       (((io.ex_rd_addr === io.id_rs_addr) && io.id_reads_rs) || ((io.ex_rd_addr === io.id_rt_addr) && io.id_reads_rt))) {
    io.load_use_hazard_stall := true.B
  }
} 