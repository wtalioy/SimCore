package simcore.cpu.utils.interfaces

import chisel3._
import chisel3.util._

/**
 * Pipeline stage interfaces
 */

// IF to ID
class IFIDIO(dataBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val instr = UInt(dataBits.W)
  val valid = Bool()
}

object IFIDIO {
  def NOP(dataBits: Int): IFIDIO = {
    val bundle = Wire(new IFIDIO(dataBits))
    bundle.pc := 0.U
    bundle.instr := 0.U
    bundle.valid := false.B
    bundle
  }
}

// ID to EX
class IDEXIO(dataBits: Int, addrBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val rs1_data = UInt(dataBits.W)
  val rs2_data = UInt(dataBits.W)
  val rs1_addr = UInt(addrBits.W)
  val rs2_addr = UInt(addrBits.W)
  val rd_addr = UInt(addrBits.W)
  val imm = UInt(dataBits.W)
  val ctrl = new ControlIO()
  val valid = Bool()
}

object IDEXIO {
  def NOP(dataBits: Int, addrBits: Int): IDEXIO = {
    val bundle = Wire(new IDEXIO(dataBits, addrBits))
    bundle.pc := 0.U
    bundle.rs1_data := 0.U
    bundle.rs2_data := 0.U
    bundle.rs1_addr := 0.U
    bundle.rs2_addr := 0.U
    bundle.rd_addr := 0.U
    bundle.imm := 0.U
    bundle.ctrl := ControlIO.NOP()
    bundle.valid := false.B
    bundle
  }
}

// EX to MEM
class EXMEMIO(dataBits: Int, addrBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val alu_result = UInt(dataBits.W)
  val rd_addr = UInt(addrBits.W)
  val rs2_data = UInt(dataBits.W) // For store operations
  val ctrl = new MemWbCtrlIO()
  val valid = Bool()
}

object EXMEMIO {
  def NOP(dataBits: Int, addrBits: Int): EXMEMIO = {
    val bundle = Wire(new EXMEMIO(dataBits, addrBits))
    bundle.pc := 0.U
    bundle.alu_result := 0.U
    bundle.rd_addr := 0.U
    bundle.rs2_data := 0.U
    bundle.ctrl := MemWbCtrlIO.NOP()
    bundle.valid := false.B
    bundle
  }
}

// MEM to WB
class MEMWBIO(dataBits: Int, addrBits: Int) extends Bundle {
  val pc = UInt(dataBits.W)
  val result = UInt(dataBits.W) // From ALU or memory
  val rd_addr = UInt(addrBits.W)
  val ctrl = new WbCtrlIO()
  val valid = Bool()
}

object MEMWBIO {
  def NOP(dataBits: Int, addrBits: Int): MEMWBIO = {
    val bundle = Wire(new MEMWBIO(dataBits, addrBits))
    bundle.pc := 0.U
    bundle.result := 0.U
    bundle.rd_addr := 0.U
    bundle.ctrl := WbCtrlIO.NOP()
    bundle.valid := false.B
    bundle
  }
} 