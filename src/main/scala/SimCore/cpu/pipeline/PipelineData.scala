package SimCore.cpu.pipeline

import chisel3._
import SimCore.cpu.utils.ALUOps      // Import for NOP definition
import SimCore.cpu.utils.BranchTypes // Import for NOP definition
import SimCore.cpu.utils.ControlBundle // Import for IDEXData field and NOP

// Data Bundle for IF/ID Pipeline Register
class IFIDData extends Bundle {
  val pc    = UInt(32.W)
  val instr = UInt(32.W)
  // val valid = Bool() // Or handle valid propagation outside the data bundle if preferred
}
object IFIDData {
  def NOP: IFIDData = {
    val bundle = Wire(new IFIDData)
    bundle.pc    := 0.U
    bundle.instr := 0.U // MIPS NOP: sll r0, r0, 0
    bundle
  }
}

// Data Bundle for ID/EX Pipeline Register
class IDEXData extends Bundle {
  // Data from IF/ID (PC)
  val pc = UInt(32.W)

  // Data from Register File Read
  val rs1_data = UInt(32.W)
  val rs2_data = UInt(32.W)

  // Decoded signals from IDU (now bundled)
  val rd_addr = UInt(5.W) // Still needed for writeback path clarity before EXEU
  val imm = UInt(32.W) // Still needed as ALU operand
  val ctrl = new ControlBundle() // Bundled control signals

  // Keep reads_rs and reads_rt for HDU if they are not part of ControlBundle
  // For now, let's assume HDU sources them from IDU outputs directly, not from IDEX reg.
  // So, removing from here as they would be redundant if ctrl is passed.
  // If needed directly by HDU from IDEX, they could be added back or sourced from ctrl.

  // val valid = Bool()
}
object IDEXData {
  def NOP: IDEXData = {
    val bundle = Wire(new IDEXData)
    bundle.pc := 0.U
    bundle.rs1_data := 0.U
    bundle.rs2_data := 0.U
    bundle.rd_addr := 0.U
    bundle.imm := 0.U
    bundle.ctrl := ControlBundle.NOP // Initialize the bundle to NOP
    bundle
  }
}

// Data Bundle for EX/MEM Pipeline Register
class EXMEMData extends Bundle {
  // Data for Writeback
  val wb_data_from_alu  = UInt(32.W) // ALU result or JAL return address
  val rd_addr_wb        = UInt(5.W)
  val reg_write_en_wb   = Bool()
  val mem_to_reg_wb     = Bool() // If true, wb_data comes from memory, else from alu_result

  // Data for Memory Stage
  val mem_addr          = UInt(32.W) // Calculated address for LW/SW
  val mem_wdata         = UInt(32.W) // Data for SW
  val is_load_mem       = Bool()
  val is_store_mem      = Bool()

  // Potentially PC for exception handling or other debug
  val pc_ex             = UInt(32.W)

  // val valid = Bool()
}
object EXMEMData {
  def NOP: EXMEMData = {
    val bundle = Wire(new EXMEMData)
    bundle.wb_data_from_alu := 0.U
    bundle.rd_addr_wb := 0.U
    bundle.reg_write_en_wb := false.B // Crucial for NOP
    bundle.mem_to_reg_wb := false.B
    bundle.mem_addr := 0.U
    bundle.mem_wdata := 0.U
    bundle.is_load_mem := false.B
    bundle.is_store_mem := false.B
    bundle.pc_ex := 0.U
    bundle
  }
}

// Data Bundle for MEM/WB Pipeline Register
class MEMWBData extends Bundle {
  val data_to_wb        = UInt(32.W) // Actual data to write (from Mem or ALU via EXMEM)
  val rd_addr_wb        = UInt(5.W)
  val reg_write_en_wb   = Bool()

  // val valid = Bool()
}
object MEMWBData {
  def NOP: MEMWBData = {
    val bundle = Wire(new MEMWBData)
    bundle.data_to_wb := 0.U
    bundle.rd_addr_wb := 0.U
    bundle.reg_write_en_wb := false.B // Crucial for NOP
    bundle
  }
} 