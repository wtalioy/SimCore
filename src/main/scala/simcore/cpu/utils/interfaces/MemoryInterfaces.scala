package simcore.cpu.utils.interfaces

import chisel3._
import chisel3.util._

/**
 * Memory interfaces
 */
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