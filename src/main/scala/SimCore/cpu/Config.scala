package SimCore.cpu

import chisel3._
import chisel3.util._

/**
 * Configuration trait for SimCore CPU
 * 
 * This trait centralizes all parameters and configuration values
 * used throughout the CPU implementation.
 */
trait Config {
  // CPU bit widths
  def XLEN = 32              // Register width
  def GPR_NUM = 32           // Number of general purpose registers
  def GPR_LEN = log2Up(GPR_NUM)  // Bits needed to address registers
  
  // Memory parameters
  def IMEM_SIZE = 1024 * 1024 * 4  // 4MB instruction memory
  def DMEM_SIZE = 1024 * 1024 * 4  // 4MB data memory
  
  // Pipeline parameters
  def FORWARD_PORT_NUM = 2   // Number of forwarding ports
  def READ_PORT_NUM = 2      // Number of read ports on register file
  def WRITE_PORT_NUM = 1     // Number of write ports on register file
  
  // Initial values
  def PC_START = "h80000000".U(XLEN.W)  // Default program counter start
  
  // Debug parameters
  def DEBUG_MODE = false     // Enable debug output
}

/**
 * Global configuration singleton for runtime configuration changes
 */
object GlobalConfig {
  var SIM = true             // Simulation mode flag
  var DEBUG = false          // Debug output flag
} 