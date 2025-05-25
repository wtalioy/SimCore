package simcore.cpu.components

import chisel3._
import chisel3.util._

/**
 * Hazard Detection Unit
 * Detects load-use hazards and generates stall signals
 */
class HazardDetectionUnit(addrBits: Int) extends Module {
  val io = IO(new Bundle {
    // ID stage register addresses and usage flags
    val id_rs1_addr = Input(UInt(addrBits.W))
    val id_rs2_addr = Input(UInt(addrBits.W))
    val id_uses_rs1 = Input(Bool())
    val id_uses_rs2 = Input(Bool())
    
    // EX stage information
    val ex_rd_addr = Input(UInt(addrBits.W))
    val ex_is_load = Input(Bool())
    
    // Pipeline control output
    val load_use_stall = Output(Bool())
  })

  // Detect load-use hazard:
  // When a load instruction is in EX stage and the next instruction uses its destination register
  val rs1_hazard = io.id_uses_rs1 && io.ex_is_load && (io.ex_rd_addr === io.id_rs1_addr) && (io.ex_rd_addr =/= 0.U)
  val rs2_hazard = io.id_uses_rs2 && io.ex_is_load && (io.ex_rd_addr === io.id_rs2_addr) && (io.ex_rd_addr =/= 0.U)
  
  io.load_use_stall := rs1_hazard || rs2_hazard
} 