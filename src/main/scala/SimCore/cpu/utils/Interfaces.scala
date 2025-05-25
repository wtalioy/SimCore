package SimCore.cpu.utils

import chisel3._
import chisel3.util._

// Memory interfaces
class IBusIO(dataBits: Int) extends Bundle {
  val req_ready = Output(Bool())
  val req_addr = Output(UInt(dataBits.W))
  val resp_valid = Input(Bool())
  val resp_data = Input(UInt(dataBits.W))
}

class DBusIO(dataBits: Int) extends Bundle {
  val req_ready = Output(Bool())
  val req_addr = Output(UInt(dataBits.W))
  val req_wdata = Output(UInt(dataBits.W))
  val req_wen = Output(Bool())
  val resp_valid = Input(Bool())
  val resp_rdata = Input(UInt(dataBits.W))
}

// Pipeline stage interfaces - following a consistent pattern

// IF to ID
class IFID_Bundle(dataBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val instr = UInt(dataBits.W)
  val valid = Bool()
}

object IFID_Bundle {
  def NOP(dataBits: Int): IFID_Bundle = {
    val bundle = Wire(new IFID_Bundle(dataBits))
    bundle.pc := 0.U
    bundle.instr := 0.U
    bundle.valid := false.B
    bundle
  }
}

// ID to EX
class IDEX_Bundle(dataBits: Int, addrBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val rs1_data = UInt(dataBits.W)
  val rs2_data = UInt(dataBits.W)
  val rs1_addr = UInt(addrBits.W)
  val rs2_addr = UInt(addrBits.W)
  val rd_addr = UInt(addrBits.W)
  val imm = UInt(dataBits.W)
  val ctrl = new ControlBundle()
  val valid = Bool()
}

object IDEX_Bundle {
  def NOP(dataBits: Int, addrBits: Int): IDEX_Bundle = {
    val bundle = Wire(new IDEX_Bundle(dataBits, addrBits))
    bundle.pc := 0.U
    bundle.rs1_data := 0.U
    bundle.rs2_data := 0.U
    bundle.rs1_addr := 0.U
    bundle.rs2_addr := 0.U
    bundle.rd_addr := 0.U
    bundle.imm := 0.U
    bundle.ctrl := ControlBundle.NOP()
    bundle.valid := false.B
    bundle
  }
}

// EX to MEM
class EXMEM_Bundle(dataBits: Int, addrBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val alu_result = UInt(dataBits.W)
  val rd_addr = UInt(addrBits.W)
  val rs2_data = UInt(dataBits.W) // For store operations
  val ctrl = new MemWbCtrlBundle()
  val valid = Bool()
}

object EXMEM_Bundle {
  def NOP(dataBits: Int, addrBits: Int): EXMEM_Bundle = {
    val bundle = Wire(new EXMEM_Bundle(dataBits, addrBits))
    bundle.pc := 0.U
    bundle.alu_result := 0.U
    bundle.rd_addr := 0.U
    bundle.rs2_data := 0.U
    bundle.ctrl := MemWbCtrlBundle.NOP()
    bundle.valid := false.B
    bundle
  }
}

// MEM to WB
class MEMWB_Bundle(dataBits: Int, addrBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val result = UInt(dataBits.W) // From ALU or memory
  val rd_addr = UInt(addrBits.W)
  val ctrl = new WbCtrlBundle()
  val valid = Bool()
}

object MEMWB_Bundle {
  def NOP(dataBits: Int, addrBits: Int): MEMWB_Bundle = {
    val bundle = Wire(new MEMWB_Bundle(dataBits, addrBits))
    bundle.pc := 0.U
    bundle.result := 0.U
    bundle.rd_addr := 0.U
    bundle.ctrl := WbCtrlBundle.NOP()
    bundle.valid := false.B
    bundle
  }
}

// Control signal bundles - separated by pipeline stage
class ControlBundle extends Bundle {
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
  def NOP(): ControlBundle = {
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
}

// Memory/Writeback control signals
class MemWbCtrlBundle extends Bundle {
  val mem_read = Bool()
  val mem_write = Bool()
  val mem_to_reg = Bool()
  val reg_write = Bool()
}

object MemWbCtrlBundle {
  def NOP(): MemWbCtrlBundle = {
    val bundle = Wire(new MemWbCtrlBundle())
    bundle.mem_read := false.B
    bundle.mem_write := false.B
    bundle.mem_to_reg := false.B
    bundle.reg_write := false.B
    bundle
  }
}

// Writeback control signals
class WbCtrlBundle extends Bundle {
  val reg_write = Bool()
}

object WbCtrlBundle {
  def NOP(): WbCtrlBundle = {
    val bundle = Wire(new WbCtrlBundle())
    bundle.reg_write := false.B
    bundle
  }
}
