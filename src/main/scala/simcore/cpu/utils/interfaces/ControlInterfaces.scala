package simcore.cpu.utils.interfaces

import chisel3._
import chisel3.util._

/**
 * Control signal bundles - separated by pipeline stage
 */

// Control signals for ID/EX stage
class ControlIO extends Bundle {
  // ALU control
  val alu_op = UInt(4.W)
  val alu_src1_sel = UInt(2.W)
  val alu_src2_sel = UInt(2.W)
  
  // Branch/Jump control
  val branch_type = UInt(4.W)
  val jump = Bool()
  
  // Memory control
  val mem_read = Bool()
  val mem_write = Bool()
  val mem_to_reg = Bool()
  
  // Register file control
  val reg_write = Bool()
  
  // Operand usage flags for hazard detection
  val uses_rs1 = Bool()
  val uses_rs2 = Bool()
}

object ControlIO {
  def NOP(): ControlIO = {
    val bundle = Wire(new ControlIO())
    bundle.alu_op := 0.U
    bundle.alu_src1_sel := 0.U
    bundle.alu_src2_sel := 0.U
    bundle.branch_type := 0.U
    bundle.jump := false.B
    bundle.mem_read := false.B
    bundle.mem_write := false.B
    bundle.mem_to_reg := false.B
    bundle.reg_write := false.B
    bundle.uses_rs1 := false.B
    bundle.uses_rs2 := false.B
    bundle
  }
}

// Memory/Writeback control signals for EX/MEM stage
class MemWbCtrlIO extends Bundle {
  val mem_read = Bool()
  val mem_write = Bool()
  val mem_to_reg = Bool()
  val reg_write = Bool()
}

object MemWbCtrlIO {
  def NOP(): MemWbCtrlIO = {
    val bundle = Wire(new MemWbCtrlIO())
    bundle.mem_read := false.B
    bundle.mem_write := false.B
    bundle.mem_to_reg := false.B
    bundle.reg_write := false.B
    bundle
  }
}

// Writeback control signals for MEM/WB stage
class WbCtrlIO extends Bundle {
  val reg_write = Bool()
}

object WbCtrlIO {
  def NOP(): WbCtrlIO = {
    val bundle = Wire(new WbCtrlIO())
    bundle.reg_write := false.B
    bundle
  }
} 