package SimCore.cpu.utils

import chisel3._
import chisel3.util._
import SimCore.cpu.Config
import SimCore.cpu.GlobalConfig

/**
 * Interface bundles for SimCore CPU
 * All bundles parameterized using the Config trait
 */
trait InterfaceConfig extends Config

// Memory interfaces
class IBusIO extends Bundle with InterfaceConfig {
  val req_ready = Output(Bool())
  val req_addr = Output(UInt(XLEN.W))
  val resp_valid = Input(Bool())
  val resp_data = Input(UInt(XLEN.W))
}

class DBusIO extends Bundle with InterfaceConfig {
  val req_ready = Output(Bool())
  val req_addr = Output(UInt(XLEN.W))
  val req_wdata = Output(UInt(XLEN.W))
  val req_wen = Output(Bool())
  val resp_valid = Input(Bool())
  val resp_rdata = Input(UInt(XLEN.W))
}

// Pipeline stage interfaces - following a consistent pattern

// IF to ID
class IFID_Bundle extends Bundle with InterfaceConfig {
  val pc = UInt(XLEN.W)
  val instr = UInt(XLEN.W)
  val valid = Bool()
}

object IFID_Bundle {
  def apply(): IFID_Bundle = {
    val bundle = Wire(new IFID_Bundle())
    bundle.pc := 0.U
    bundle.instr := 0.U
    bundle.valid := false.B
    bundle
  }
  
  def NOP = apply()
}

// ID to EX
class IDEX_Bundle extends Bundle with InterfaceConfig {
  val pc = UInt(XLEN.W)
  val rs1_data = UInt(XLEN.W)
  val rs2_data = UInt(XLEN.W)
  val rs1_addr = UInt(GPR_LEN.W)
  val rs2_addr = UInt(GPR_LEN.W)
  val rd_addr = UInt(GPR_LEN.W)
  val imm = UInt(XLEN.W)
  val ctrl = new ControlBundle()
  val valid = Bool()
}

object IDEX_Bundle {
  def apply(): IDEX_Bundle = {
    val bundle = Wire(new IDEX_Bundle())
    bundle.pc := 0.U
    bundle.rs1_data := 0.U
    bundle.rs2_data := 0.U
    bundle.rs1_addr := 0.U
    bundle.rs2_addr := 0.U
    bundle.rd_addr := 0.U
    bundle.imm := 0.U
    bundle.ctrl := ControlBundle.NOP
    bundle.valid := false.B
    bundle
  }
  
  def NOP = apply()
}

// EX to MEM
class EXMEM_Bundle extends Bundle with InterfaceConfig {
  val pc = UInt(XLEN.W)
  val alu_result = UInt(XLEN.W)
  val rd_addr = UInt(GPR_LEN.W)
  val rs2_data = UInt(XLEN.W) // For store operations
  val ctrl = new MemWbCtrlBundle()
  val valid = Bool()
}

object EXMEM_Bundle {
  def apply(): EXMEM_Bundle = {
    val bundle = Wire(new EXMEM_Bundle())
    bundle.pc := 0.U
    bundle.alu_result := 0.U
    bundle.rd_addr := 0.U
    bundle.rs2_data := 0.U
    bundle.ctrl := MemWbCtrlBundle.NOP
    bundle.valid := false.B
    bundle
  }
  
  def NOP = apply()
}

// MEM to WB
class MEMWB_Bundle extends Bundle with InterfaceConfig {
  val pc = UInt(XLEN.W)
  val result = UInt(XLEN.W) // From ALU or memory
  val rd_addr = UInt(GPR_LEN.W)
  val ctrl = new WbCtrlBundle()
  val valid = Bool()
}

object MEMWB_Bundle {
  def apply(): MEMWB_Bundle = {
    val bundle = Wire(new MEMWB_Bundle())
    bundle.pc := 0.U
    bundle.result := 0.U
    bundle.rd_addr := 0.U
    bundle.ctrl := WbCtrlBundle.NOP
    bundle.valid := false.B
    bundle
  }
  
  def NOP = apply()
}

// Control signal bundles - separated by pipeline stage
class ControlBundle extends Bundle with InterfaceConfig {
  // ALU control
  val alu_op = UInt(4.W)
  val alu_src1_sel = UInt(2.W)
  val alu_src2_sel = UInt(2.W)
  
  // Branch/Jump control
  val branch_type = UInt(3.W)
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

object ControlBundle {
  def apply(): ControlBundle = {
    val bundle = Wire(new ControlBundle())
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
  
  def NOP = apply()
}

// Memory/Writeback control signals
class MemWbCtrlBundle extends Bundle with InterfaceConfig {
  val mem_read = Bool()
  val mem_write = Bool()
  val mem_to_reg = Bool()
  val reg_write = Bool()
}

object MemWbCtrlBundle {
  def apply(): MemWbCtrlBundle = {
    val bundle = Wire(new MemWbCtrlBundle())
    bundle.mem_read := false.B
    bundle.mem_write := false.B
    bundle.mem_to_reg := false.B
    bundle.reg_write := false.B
    bundle
  }
  
  def NOP = apply()
}

// Writeback control signals
class WbCtrlBundle extends Bundle with InterfaceConfig {
  val reg_write = Bool()
}

object WbCtrlBundle {
  def apply(): WbCtrlBundle = {
    val bundle = Wire(new WbCtrlBundle())
    bundle.reg_write := false.B
    bundle
  }
  
  def NOP = apply()
}

// Forwarding Unit constants
object ForwardingSelects {
  val NO_FORWARD = 0.U(2.W)
  val FORWARD_FROM_MEM = 1.U(2.W) 
  val FORWARD_FROM_WB = 2.U(2.W)
} 