package simcore.cpu.stages

import chisel3._
import chisel3.util._
import simcore.cpu.utils.interfaces.{IBusIO, IFIDIO}
import simcore.cpu.utils.interfaces.{BTBInIO, BTBOutIO, BTBUpdateIO}
import simcore.cpu.Config
import simcore.cpu.GlobalConfig
import simcore.cpu.components.BTB

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
    
    // Branch prediction update interface
    val bp_update = Input(new BTBUpdateIO(BTB_ENTRY_NUM, XLEN))
    
    // Output to ID stage
    val out = Output(new IFIDIO(XLEN))
  })

  // Program Counter register
  val pc = RegInit(PC_START)
  
  // Branch Target Buffer (BTB) for branch prediction
  val btb = Module(new BTB(BTB_ENTRY_NUM, XLEN, BTB_TAG_BITS, BTB_SC_INIT, INSTR_BYTES))
  
  // Connect BTB prediction inputs
  btb.io.pred.in.pc := pc
  
  // Connect BTB update interface
  btb.io.update := io.bp_update
  
  // Output state registers - properly initialize with hardware values
  val outReg = RegInit({
    val init = Wire(new IFIDIO(XLEN))
    init.pc := 0.U
    init.instr := 0.U
    init.valid := false.B
    init.predicted_taken := false.B
    init.predicted_target := 0.U
    init
  })

  // Memory request signal
  val memReqReady = Wire(Bool())
  memReqReady := false.B
  
  // Next PC calculation
  val next_pc = Wire(UInt(XLEN.W))
  next_pc := Mux(btb.io.pred.out.taken, btb.io.pred.out.target, pc + 4.U)
  
  // Connect memory interface
  io.ibus.req_ready := memReqReady
  io.ibus.req_addr := pc

  // Core state machine logic
  when (!io.stall) {
    when (io.redirect_valid) {
      // Branch/jump taken - redirect fetch due to misprediction or external request
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
        outReg.predicted_taken := btb.io.pred.out.taken && btb.io.pred.out.hit
        outReg.predicted_target := btb.io.pred.out.target
        
        // Update PC based on prediction
        pc := next_pc
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
    dontTouch(next_pc)
    dontTouch(btb.io.pred.out)
  }
}
