package SimCore.cpu.pipeline

import chisel3._
import chisel3.util._

/**
 * Generic Pipeline Register.
 * Creates a register that holds data between pipeline stages with stall and flush control.
 *
 * @param T The Bundle type for data passing between stages
 * @param gen A generator for the Bundle type
 */
class PipelineRegister[T <: Data](gen: T, resetValue: Option[T] = None) extends Module {
  val io = IO(new Bundle {
    // Data interface
    val in = Input(gen.cloneType)
    val out = Output(gen.cloneType)

    // Control interface
    val stall = Input(Bool())  // When true, register holds current value
    val flush = Input(Bool())  // When true, register resets to zero
  })

  // Main data register - initialize with zeros
  val dataReg = RegInit(0.U.asTypeOf(gen.cloneType))
  
  // Update logic with priority: flush > stall > normal operation
  when (io.flush) {
    dataReg := 0.U.asTypeOf(gen.cloneType)
  }.elsewhen (!io.stall) {
    dataReg := io.in
  }
  
  // Connect register to output
  io.out := dataReg
} 