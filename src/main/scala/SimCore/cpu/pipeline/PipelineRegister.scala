package SimCore.cpu.pipeline

import chisel3._
import chisel3.util._

/**
 * Generic Pipeline Register.
 * @param gen The type of Data Bundle to register.
 * @param initVal Optional initial value for the register (e.g., for flushing to NOP-like state).
 */
class PipelineRegister[T <: Data](gen: T, initVal: Option[T] = None) extends Module {
  val io = IO(new Bundle {
    val in = Input(gen.cloneType)
    val out = Output(gen.cloneType)

    val stall = Input(Bool()) // If true, register holds its value
    val flush = Input(Bool()) // If true, register resets to initVal (or zero if no initVal)
    val valid_in = Input(Bool()) // Valid signal for the input data
    val valid_out = Output(Bool()) // Registered valid signal
  })

  // The main data register
  val data_reg = Reg(gen.cloneType)
  // The valid signal register
  val valid_reg = RegInit(false.B)

  when(io.flush) {
    // On flush, reset to initial value or zeros, and invalidate
    data_reg := initVal.getOrElse(0.U.asTypeOf(gen))
    valid_reg := false.B
  }.elsewhen(!io.stall) {
    // If not stalled and not flushed, pass input to output register
    data_reg := io.in
    valid_reg := io.valid_in
  }.otherwise {
    // Stalled: keep current value (do nothing to data_reg)
    // valid_reg also holds its value unless specifically cleared by flush
  }

  io.out := data_reg
  io.valid_out := valid_reg && !io.flush // Ensure flush overrides valid

  // Alternative simpler register if no initVal and flush implies just invalidation
  // val data_reg = RegEnable(io.in, !io.stall)
  // val valid_reg = RegInit(false.B)
  // when (io.flush) {
  //   valid_reg := false.B
  // } .elsewhen(!io.stall) {
  //  valid_reg := io.valid_in
  // }
  // io.out := data_reg
  // io.valid_out := valid_reg
} 