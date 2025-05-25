package SimCore.cpu.components

import chisel3._
import chisel3.util._
import SimCore.cpu.Config
import SimCore.cpu.GlobalConfig

/**
 * Unified Register File with configurable parameters
 * 
 * Features:
 * - Configurable data width, register count, and read/write ports
 * - Register 0 hardwired to 0
 * - Default configuration matches standard RISC-V regfile using Config trait
 */
class RegFile(
    val dataBits: Int = -1,  // Will use XLEN from Config if -1
    val regCount: Int = -1,  // Will use GPR_NUM from Config if -1
    val readPorts: Int = -1, // Will use READ_PORT_NUM from Config if -1
    val writePorts: Int = -1 // Will use WRITE_PORT_NUM from Config if -1
) extends Module with Config {
  // Use provided parameters or default to Config trait values
  private val actualDataBits = if (dataBits == -1) XLEN else dataBits
  private val actualRegCount = if (regCount == -1) GPR_NUM else regCount
  private val actualReadPorts = if (readPorts == -1) READ_PORT_NUM else readPorts
  private val actualWritePorts = if (writePorts == -1) WRITE_PORT_NUM else writePorts
  
  require(actualReadPorts > 0, "At least one read port is required")
  require(actualWritePorts > 0, "At least one write port is required")
  
  val addrWidth = log2Ceil(actualRegCount)
  
  val io = IO(new Bundle {
    // Read ports
    val raddr = Vec(actualReadPorts, Input(UInt(addrWidth.W)))
    val rdata = Vec(actualReadPorts, Output(UInt(actualDataBits.W)))
    
    // Write ports
    val waddr = Vec(actualWritePorts, Input(UInt(addrWidth.W)))
    val wdata = Vec(actualWritePorts, Input(UInt(actualDataBits.W)))
    val wen   = Vec(actualWritePorts, Input(Bool()))
  })
  
  // Register file storage
  val regs = Reg(Vec(actualRegCount, UInt(actualDataBits.W)))
  
  // Read ports logic - register 0 always reads as 0
  for (i <- 0 until actualReadPorts) {
    io.rdata(i) := Mux(io.raddr(i) === 0.U, 0.U, regs(io.raddr(i)))
  }
  
  // Write ports logic - handle multiple writes to same register by prioritizing lower indices
  // Use foldRight to give priority to lower indices (port 0 over port 1, etc.)
  for (writePort <- (0 until actualWritePorts).reverse) {
    when(io.wen(writePort) && io.waddr(writePort) =/= 0.U) {
      regs(io.waddr(writePort)) := io.wdata(writePort)
    }
  }
  
  // Debug signals
  if (GlobalConfig.DEBUG) {
    dontTouch(regs)
  }
} 