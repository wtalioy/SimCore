package SimCore.cpu.stages

import chisel3._
import chisel3.util._
import SimCore.cpu.utils.DBusIO
import SimCore.cpu.utils.EXMEM_Bundle
import SimCore.cpu.utils.MEMWB_Bundle
import SimCore.cpu.Config

/** Memory Access Unit
  * Handles memory operations (load/store) and passes ALU results through
  * for register writeback.
  */
class MEM extends Module with Config {
  val io = IO(new Bundle {
    // Input from EX stage
    val in = Input(new EXMEM_Bundle(XLEN, GPR_LEN))
    
    // Output to WB stage
    val out = Output(new MEMWB_Bundle(XLEN, GPR_LEN))
    
    // Memory interface - using DBusIO directly, not Flipped
    val dbus = new DBusIO(XLEN)
    
    // Stall signal
    val stall_out = Output(Bool())
  })
  
  // Default output values
  io.out := MEMWB_Bundle.NOP(XLEN, GPR_LEN)
  io.stall_out := false.B
  
  // Default memory interface values
  io.dbus.req_ready := false.B
  io.dbus.req_addr := 0.U
  io.dbus.req_wdata := 0.U
  io.dbus.req_wen := false.B
  
  when(io.in.valid) {
    // Pass through basic pipeline data
    io.out.pc := io.in.pc
    io.out.rd_addr := io.in.rd_addr
    io.out.ctrl.reg_write := io.in.ctrl.reg_write
    io.out.valid := io.in.valid
    
    // Memory operation handling
    when(io.in.ctrl.mem_read || io.in.ctrl.mem_write) {
      // Activate memory interface
      io.dbus.req_ready := true.B
      io.dbus.req_addr := io.in.alu_result
      io.dbus.req_wen := io.in.ctrl.mem_write
      
      // For store operations, provide write data
      when(io.in.ctrl.mem_write) {
        io.dbus.req_wdata := io.in.rs2_data
      }
      
      // Stall logic - stall if memory operation is not complete
      io.stall_out := !io.dbus.resp_valid
      
      // Result selection for output
      when(io.in.ctrl.mem_read && io.dbus.resp_valid) {
        // Load operation - use data from memory
        io.out.result := io.dbus.resp_rdata
      }.otherwise {
        // Use ALU result
        io.out.result := io.in.alu_result
      }
    }.otherwise {
      // For non-memory operations, just pass through ALU result
      io.out.result := io.in.alu_result
    }
  }
} 