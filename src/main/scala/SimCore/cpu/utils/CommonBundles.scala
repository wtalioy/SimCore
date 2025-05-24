package SimCore.cpu.utils

import chisel3._
import SimCore.cpu.utils.ALUOps // Import for NOP definition
import SimCore.cpu.utils.BranchTypes // Import for NOP definition

// Instruction Bus Bundle
// From the perspective of a master (like the Core driving the IFU, or IFU driving memory)
// IFU is a master to IMem, Core is a master to IFU's bus pins typically.
// Let's define from Core's perspective to external IMem.
class IBusBundle extends Bundle {
  // Request from Core/IFU to IMem
  val req_addr = Output(UInt(32.W))
  val req_ready = Output(Bool())
  // Response from IMem to Core/IFU
  val resp_data = Input(UInt(32.W))
  val resp_valid = Input(Bool())
}

// Data Bus Bundle
// From Core's perspective to external DMem.
class DBusBundle extends Bundle {
  // Request from Core/EXEU to DMem
  val req_addr = Output(UInt(32.W))
  val req_wdata = Output(UInt(32.W))
  val req_wen = Output(Bool())
  val req_ready = Output(Bool())
  // Response from DMem to Core/EXEU
  val resp_data = Input(UInt(32.W))
  val resp_valid = Input(Bool())
}

// Control signals output by IDU, to be piped to EXEU
class IDUControlOutputs extends Bundle {
  val alu_op = UInt(4.W)
  val alu_src2_is_imm = Bool()
  val is_branch = Bool()
  val branch_type = UInt(3.W)
  val is_jump = Bool()
  val is_jal = Bool()
  val jump_target_from_idu = UInt(32.W) // For J/JAL, pre-calculated by IDU
  val is_load = Bool()
  val is_store = Bool()
  val reg_write_en = Bool() // Initial enable from decode
  val mem_to_reg = Bool() // Control signal for WB mux
  val reads_rs = Bool() // True if the instruction reads register rs
  val reads_rt = Bool() // True if the instruction reads register rt
}

object IDUControlOutputs {
  def NOP: IDUControlOutputs = {
    val bundle = Wire(new IDUControlOutputs)
    bundle.alu_op := ALUOps.ADD // Default, won't matter due to reg_write_en=false
    bundle.alu_src2_is_imm := false.B
    bundle.is_branch := false.B
    bundle.branch_type := BranchTypes.BEQ // Default
    bundle.is_jump := false.B
    bundle.is_jal := false.B
    bundle.jump_target_from_idu := 0.U
    bundle.is_load := false.B
    bundle.is_store := false.B
    bundle.reg_write_en := false.B
    bundle.mem_to_reg := false.B
    bundle.reads_rs := false.B
    bundle.reads_rt := false.B
    bundle
  }
}
