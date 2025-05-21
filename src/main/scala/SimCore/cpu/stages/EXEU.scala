package SimCore.cpu.stages

import chisel3._
import chisel3.util._
import SimCore.cpu.components.ALUUnit
import SimCore.cpu.components.BrUnit
import SimCore.cpu.components.MemUnit
import SimCore.cpu.utils.ALUOps // For Mux condition on actual_jump_target
import SimCore.cpu.utils.IDUControlOutputs // Import for the new input

// Define ExeReq and ExeResp if not globally defined
// class ExeReq extends Bundle { /* ... */ }
// class ExeResp extends Bundle { /* ... */ }

class EXEU extends Module {
  val io = IO(new Bundle {
    val valid_in = Input(Bool())
    // Data inputs (still needed individually)
    val exe_req_pc = Input(UInt(32.W)) 
    val exe_req_rs1_data = Input(UInt(32.W))
    val exe_req_rs2_data = Input(UInt(32.W))
    val exe_req_rd_addr = Input(UInt(5.W))
    val exe_req_imm = Input(UInt(32.W))
    // Bundled control signals
    val exe_req_ctrl = Input(new IDUControlOutputs())

    // To Writeback stage / RegFile / DMem
    val valid_out = Output(Bool())
    val exe_resp_data = Output(UInt(32.W)) // Result data
    val exe_resp_rd_addr = Output(UInt(5.W)) // Destination register
    val exe_resp_write_en = Output(Bool()) // To RegFile write enable

    // Stall signal (e.g. from DMem)
    val stall_in = Input(Bool())

    // Redirect signal for IFU (e.g. on taken branch/jump)
    val redirect_valid_out = Output(Bool())
    val redirect_target_out = Output(UInt(32.W))

    // DBus interface (simplified)
    val dbus_req_valid = Output(Bool())
    val dbus_req_addr = Output(UInt(32.W))
    val dbus_req_wdata = Output(UInt(32.W))
    val dbus_req_wen = Output(Bool()) // Write enable for store
    val dbus_resp_valid = Input(Bool())
    val dbus_resp_rdata = Input(UInt(32.W))
  })

  val aluUnit = Module(new ALUUnit())
  val brUnit  = Module(new BrUnit())
  val memUnit = Module(new MemUnit())

  // ALU Operand Mux
  val alu_operand2 = Mux(io.exe_req_ctrl.alu_src2_is_imm, io.exe_req_imm, io.exe_req_rs2_data)

  // Connect inputs to functional units
  aluUnit.io.alu_op := io.exe_req_ctrl.alu_op
  aluUnit.io.operand1 := io.exe_req_rs1_data
  aluUnit.io.operand2 := alu_operand2

  // Branch Unit connections
  brUnit.io.rs1_data := io.exe_req_rs1_data
  brUnit.io.rs2_data := io.exe_req_rs2_data 
  brUnit.io.is_branch := io.exe_req_ctrl.is_branch
  brUnit.io.branch_type := io.exe_req_ctrl.branch_type
  // For MIPS, branch target calculation is PC+4 + offset. We'll calculate it below.
  // Jumps are handled separately.
  // For JR, target is rs1_data (passed through ALU with COPY_A for example)
  // For J/JAL, target is pre-calculated from IDU
  brUnit.io.is_jump := false.B // BrUnit only handles conditional branches based on its current setup
  brUnit.io.imm := 0.U // Not used by BrUnit in this configuration


  // Mem Unit connections
  // For LW/SW, address is rs1_data + imm. This is calculated by ALU.
  memUnit.io.addr := aluUnit.io.result
  memUnit.io.wdata := io.exe_req_rs2_data // For SW, data comes from rs2 (MIPS: rt)
  memUnit.io.is_load := io.exe_req_ctrl.is_load
  memUnit.io.is_store := io.exe_req_ctrl.is_store

  io.dbus_req_valid := memUnit.io.dbus_req_valid
  io.dbus_req_addr := memUnit.io.dbus_req_addr
  io.dbus_req_wdata := memUnit.io.dbus_req_wdata
  io.dbus_req_wen := memUnit.io.dbus_req_wen
  memUnit.io.dbus_resp_valid := io.dbus_resp_valid
  memUnit.io.dbus_resp_rdata := io.dbus_resp_rdata

  // Results from functional units
  val alu_result = aluUnit.io.result
  val mem_result = memUnit.io.rdata
  val branch_taken = brUnit.io.branch_taken // From conditional branch unit

  // MIPS Specific Target Calculations
  // Branch target: PC of branch instr + 4 + (sign_extended_imm * 4)
  // imm is already sign-extended from IDU, and it's the word offset for MIPS branches.
  val branch_offset = (io.exe_req_imm.asSInt << 2).asUInt // imm is offset, shift by 2 for byte addr
  val mips_branch_target = io.exe_req_pc + 4.U + branch_offset

  // Jump Register (JR) target is from rs1_data (which ALU can pass through if op=COPY_A)
  // J/JAL target is directly from IDU (now via ctrl bundle)
  val actual_jump_target = Mux(io.exe_req_ctrl.alu_op === ALUOps.COPY_A && io.exe_req_ctrl.is_jump && !io.exe_req_ctrl.is_jal,
                               alu_result, // For JR, target is in rs1 (passed via ALU result)
                               io.exe_req_ctrl.jump_target_from_idu) // For J/JAL

  // Writeback data selection
  val jal_return_address = io.exe_req_pc + 4.U // For MIPS, JAL saves PC+4 (no delay slot in this model)
  io.exe_resp_data := Mux(io.exe_req_ctrl.is_jal, jal_return_address,
                        Mux(io.exe_req_ctrl.mem_to_reg, mem_result, alu_result))

  io.exe_resp_rd_addr := io.exe_req_rd_addr

  // Valid and Write Enable Logic
  val is_taken_branch = io.exe_req_ctrl.is_branch && branch_taken
  val is_unconditional_jump = io.exe_req_ctrl.is_jump // J, JAL, JR

  io.valid_out := io.valid_in && !io.stall_in
  // Reg write enable: from IDU, but disable if stalled, or if it's a taken branch/jump that doesn't write (like BEQ, J)
  // JAL always writes. Other R-types, I-types (LW, ADDI etc.) write based on exe_req_ctrl.reg_write_en.
  io.exe_resp_write_en := io.valid_out && io.exe_req_ctrl.reg_write_en && 
                           !(is_taken_branch && !io.exe_req_ctrl.is_jal) && // Don't write for non-JAL taken branches
                           !(is_unconditional_jump && !io.exe_req_ctrl.is_jal) // Don't write for J, JR
                           // JAL's exe_req_ctrl.reg_write_en is true, so it will write.

  // Redirect Logic
  io.redirect_valid_out := io.valid_in && !io.stall_in && (is_taken_branch || is_unconditional_jump)
  io.redirect_target_out := Mux(is_taken_branch,
                                mips_branch_target,
                                actual_jump_target) // For J, JAL, JR

  // Stall propagation (simplified)
  // stall_out would signal back to previous stages if EXEU is stalled (e.g. by memory)
} 