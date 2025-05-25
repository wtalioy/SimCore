package simcore.cpu.stages

import chisel3._
import chisel3.util._
import simcore.cpu.utils.IBusIO
import simcore.cpu.utils.IFID_Bundle
import simcore.cpu.Config
import simcore.cpu.GlobalConfig

/**
 * Instruction Fetch Unit
 * Responsible for fetching instructions and handling control flow changes
 */
class IF extends Module with Config {
  val io = IO(new Bundle {
    // Memory interface - Note: this should be the actual IBusIO, not Flipped
    val ibus = new IBusIO(XLEN)
    
    // Control flow signals
    val redirect_valid = Input(Bool())
    val redirect_target = Input(UInt(XLEN.W))
    val stall = Input(Bool())
    
    // Output to ID stage
    val out = Output(new IFID_Bundle(XLEN))
  })

  // Program Counter register
  val pc = RegInit(PC_START)
  
  // Output state registers - properly initialize with hardware values
  val outReg = RegInit({
    val init = Wire(new IFID_Bundle(XLEN))
    init.pc := 0.U
    init.instr := 0.U
    init.valid := false.B
    init
  })

  // Memory request signal
  val memReqReady = Wire(Bool())
  memReqReady := false.B
  
  // Connect memory interface
  io.ibus.req_ready := memReqReady
  io.ibus.req_addr := pc

  // Core state machine logic
  when (!io.stall) {
    when (io.redirect_valid) {
      // Branch/jump taken - redirect fetch
      pc := io.redirect_target
      outReg.valid := false.B
    }.otherwise {
      // Normal fetch flow
      memReqReady := true.B
      
      when (io.ibus.resp_valid) {
        // Instruction received from memory
        outReg.pc := pc
        outReg.instr := io.ibus.resp_data
        outReg.valid := true.B
        pc := pc + 4.U
      }.otherwise {
        outReg.valid := false.B
      }
    }
  }
  
  // Connect to output
  io.out := outReg

  // Add debug outputs if needed
  if (GlobalConfig.DEBUG) {
    dontTouch(pc)
    dontTouch(outReg)
  }
}
