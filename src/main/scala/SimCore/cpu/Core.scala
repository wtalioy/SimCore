package SimCore.cpu

import chisel3._
import chisel3.util._ // Required for Mux, etc.
// Stage Modules
import SimCore.cpu.stages.IFU
import SimCore.cpu.stages.IDU
import SimCore.cpu.stages.EXEU
// Component Modules
import SimCore.cpu.components.RegFile
import SimCore.cpu.components.HazardDetectionUnit
import SimCore.cpu.components.ForwardingSelect // Enum from HDU
// Pipeline Data and Register Modules
import SimCore.cpu.pipeline.PipelineRegister
import SimCore.cpu.pipeline.IFIDData
import SimCore.cpu.pipeline.IDEXData
import SimCore.cpu.pipeline.EXMEMData
import SimCore.cpu.pipeline.MEMWBData

// It's good practice to define global Bundle types if they are used across many modules.
// For now, we are relying on individual module IO definitions.

class Core extends Module {
  val io = IO(new Bundle {
    // Placeholder for IBus (Instruction Memory Interface)
    val ibus_req_ready = Output(Bool())
    val ibus_req_addr = Output(UInt(32.W))
    val ibus_resp_valid = Input(Bool())
    val ibus_resp_data = Input(UInt(32.W))

    // Placeholder for DBus (Data Memory Interface)
    val dbus_req_ready = Output(Bool())
    val dbus_req_addr = Output(UInt(32.W))
    val dbus_req_wdata = Output(UInt(32.W))
    val dbus_req_wen = Output(Bool())
    val dbus_resp_valid = Input(Bool())
    val dbus_resp_rdata = Input(UInt(32.W))

    // Debug outputs (optional)
    // val debug_pc = Output(UInt(32.W))
    // val debug_instr = Output(UInt(32.W))
  })

  // Instantiate pipeline stage modules
  val ifu = Module(new IFU())
  val idu = Module(new IDU())
  val exeu = Module(new EXEU())
  val regFile = Module(new RegFile())
  val hdu = Module(new HazardDetectionUnit())

  // Instantiate Pipeline Registers
  // Provide NOP values for flushing
  val ifid_reg = Module(new PipelineRegister(new IFIDData, Some(IFIDData.NOP)))
  val idex_reg = Module(new PipelineRegister(new IDEXData, Some(IDEXData.NOP)))
  val exmem_reg = Module(
    new PipelineRegister(new EXMEMData, Some(EXMEMData.NOP))
  )
  val memwb_reg = Module(
    new PipelineRegister(new MEMWBData, Some(MEMWBData.NOP))
  )

  // --- Stall & Flush Logic ---
  // Stall signal: For now, primarily from DMem access in EXEU.stall_in
  // A DMem access stalls if EXEU issues a dbus_req and dbus_resp is not yet valid.
  val dmem_stall_condition = exeu.io.dbus_req_ready && !io.dbus_resp_valid
  exeu.io.stall_in := dmem_stall_condition // EXEU internal stall for multi-cycle ops (like MemUnit)

  // Hazard Detection Unit Connections
  hdu.io.id_rs_addr := idu.io.uop_rs1_addr // From IDU, before ID/EX register
  hdu.io.id_rt_addr := idu.io.uop_rs2_addr // From IDU, before ID/EX register
  hdu.io.id_reads_rs := idu.io.ctrl.reads_rs // UPDATED: Connect from IDU ctrl bundle
  hdu.io.id_reads_rt := idu.io.ctrl.reads_rt // UPDATED: Connect from IDU ctrl bundle
  hdu.io.ex_rd_addr := idex_reg.io.out.rd_addr
  hdu.io.ex_reg_write_en := idex_reg.io.out.ctrl.reg_write_en
  hdu.io.ex_is_load := idex_reg.io.out.ctrl.is_load
  hdu.io.mem_rd_addr := exmem_reg.io.out.rd_addr_wb
  hdu.io.mem_reg_write_en := exmem_reg.io.out.reg_write_en_wb

  val load_use_stall = hdu.io.load_use_hazard_stall
  val overall_stall_condition = dmem_stall_condition || load_use_stall

  ifu.io.stall := overall_stall_condition
  ifid_reg.io.stall := overall_stall_condition
  idex_reg.io.stall := dmem_stall_condition // Stall ID/EX if MEM stage is busy
  // Load-use stall will control what goes INTO idex_reg

  exmem_reg.io.stall := dmem_stall_condition // EX/MEM stalls if DMem is busy
  memwb_reg.io.stall := false.B // MEM/WB not stalled by these conditions for now

  // Flush signal: From EXEU redirect (branch taken/jump)
  val flush_from_exeu_redirect = exeu.io.redirect_valid_out

  ifid_reg.io.flush := flush_from_exeu_redirect
  idex_reg.io.flush := flush_from_exeu_redirect
  // EXMEM and MEMWB are generally not flushed this way for MIPS basic pipeline.
  exmem_reg.io.flush := false.B
  memwb_reg.io.flush := false.B

  // --- Pipeline Connections ---

  // IFU <-> IBus (Top Level IO)
  io.ibus_req_ready <> ifu.io.ibus.req_ready
  io.ibus_req_addr <> ifu.io.ibus.req_addr
  io.ibus_resp_valid <> ifu.io.ibus.resp_valid
  io.ibus_resp_data <> ifu.io.ibus.resp_data
  ifu.io.redirect_valid := exeu.io.redirect_valid_out // EXEU redirect directly to IFU
  ifu.io.redirect_target := exeu.io.redirect_target_out

  // IFU -> IF/ID Register
  ifid_reg.io.in.pc := ifu.io.uop_pc
  ifid_reg.io.in.instr := ifu.io.uop_instr
  ifid_reg.io.valid_in := ifu.io.valid_out && !load_use_stall

  // IF/ID Register -> IDU
  idu.io.valid_in := ifid_reg.io.valid_out
  idu.io.uop_pc_in := ifid_reg.io.out.pc
  idu.io.uop_instr_in := ifid_reg.io.out.instr

  // IDU -> ID/EX Register (Input Side)
  val idex_reg_in_data = Wire(new IDEXData())
  // Default NOP values if load_use_stall is active
  when(load_use_stall || flush_from_exeu_redirect) { // Prioritize flush for NOP insertion
    idex_reg_in_data := IDEXData.NOP
  }.otherwise {
    idex_reg_in_data.pc := idu.io.uop_pc_out
    regFile.io.rs1_addr := idu.io.uop_rs1_addr
    regFile.io.rs2_addr := idu.io.uop_rs2_addr
    idex_reg_in_data.rs1_data := regFile.io.rs1_data
    idex_reg_in_data.rs2_data := regFile.io.rs2_data
    idex_reg_in_data.rd_addr := idu.io.uop_rd_addr
    idex_reg_in_data.imm := idu.io.uop_imm
    idex_reg_in_data.ctrl := idu.io.ctrl
  }
  idex_reg.io.in := idex_reg_in_data
  // Valid into ID/EX register is IDU output valid AND not a load-use stall (which inserts NOP)
  // If flushed, valid_in to reg is don't care as flush takes precedence for reg's valid_out
  idex_reg.io.valid_in := idu.io.valid_out && !load_use_stall

  // Forwarding Muxes for EXEU inputs
  val forwarded_rs1_data = MuxLookup(
    hdu.io.forward_A_select,
    idex_reg.io.out.rs1_data
  )( // Default: NO_FWD
    Seq(
      ForwardingSelect.FWD_FROM_EXMEM -> exmem_reg.io.out.wb_data_from_alu,
      ForwardingSelect.FWD_FROM_MEMWB -> memwb_reg.io.out.data_to_wb
    )
  )
  val forwarded_rs2_data = MuxLookup(
    hdu.io.forward_B_select,
    idex_reg.io.out.rs2_data
  )( // Default: NO_FWD
    Seq(
      ForwardingSelect.FWD_FROM_EXMEM -> exmem_reg.io.out.wb_data_from_alu,
      ForwardingSelect.FWD_FROM_MEMWB -> memwb_reg.io.out.data_to_wb
    )
  )

  // ID/EX Register -> EXEU
  exeu.io.valid_in := idex_reg.io.valid_out
  exeu.io.exe_req_pc := idex_reg.io.out.pc
  exeu.io.exe_req_rs1_data := forwarded_rs1_data
  exeu.io.exe_req_rs2_data := forwarded_rs2_data
  exeu.io.exe_req_rd_addr := idex_reg.io.out.rd_addr
  exeu.io.exe_req_imm := idex_reg.io.out.imm
  exeu.io.exe_req_ctrl := idex_reg.io.out.ctrl

  // EXEU -> EX/MEM Register
  exmem_reg.io.in.wb_data_from_alu := exeu.io.exe_resp_data // This is ALU result or JAL return PC+4
  exmem_reg.io.in.rd_addr_wb := exeu.io.exe_resp_rd_addr
  exmem_reg.io.in.reg_write_en_wb := exeu.io.exe_resp_write_en
  exmem_reg.io.in.mem_to_reg_wb := idex_reg.io.out.ctrl.mem_to_reg

  exmem_reg.io.in.mem_addr := exeu.io.dbus_req_addr // Address for LW/SW from EXEU (ALU result)
  exmem_reg.io.in.mem_wdata := exeu.io.dbus_req_wdata // Data for SW from EXEU (rs2_data)
  exmem_reg.io.in.is_load_mem := idex_reg.io.out.ctrl.is_load
  exmem_reg.io.in.is_store_mem := idex_reg.io.out.ctrl.is_store
  exmem_reg.io.in.pc_ex := idex_reg.io.out.pc // PC from ID/EX, passed for exceptions/debug

  exmem_reg.io.valid_in := exeu.io.valid_out

  // EX/MEM Register -> MEM Stage (which is mainly connecting to DBus for this design)
  // Data Memory Access (driven by EX/MEM register outputs)
  io.dbus_req_ready := exmem_reg.io.valid_out && (exmem_reg.io.out.is_load_mem || exmem_reg.io.out.is_store_mem)
  io.dbus_req_addr := exmem_reg.io.out.mem_addr
  io.dbus_req_wdata := exmem_reg.io.out.mem_wdata
  io.dbus_req_wen := exmem_reg.io.out.is_store_mem
  // exeu.io.dbus_resp_valid <> io.dbus_resp_valid // This connection is now indirect
  // exeu.io.dbus_resp_rdata <> io.dbus_resp_rdata // via exmem_reg if EXEU still needs it, or directly to MEM/WB logic

  // MEM Stage -> MEM/WB Register
  memwb_reg.io.in.data_to_wb := Mux(
    exmem_reg.io.out.mem_to_reg_wb,
    io.dbus_resp_rdata, // Data from DMem for loads
    exmem_reg.io.out.wb_data_from_alu
  ) // Data from ALU/JAL
  memwb_reg.io.in.rd_addr_wb := exmem_reg.io.out.rd_addr_wb
  memwb_reg.io.in.reg_write_en_wb := exmem_reg.io.out.reg_write_en_wb
  memwb_reg.io.valid_in := exmem_reg.io.valid_out // Valid passes through MEM if no DMem stall not handled earlier
  // Or: exmem_reg.io.valid_out && !(exmem_reg.io.out.is_load_mem && !io.dbus_resp_valid)
  // For simplicity, assuming dmem_stall handles this by stalling exmem_reg.io.valid_out propagation

  // MEM/WB Register -> Write Back to RegFile
  regFile.io.rd_addr := memwb_reg.io.out.rd_addr_wb
  regFile.io.rd_data := memwb_reg.io.out.data_to_wb
  regFile.io.wen := memwb_reg.io.valid_out && memwb_reg.io.out.reg_write_en_wb

  // Note: The Arbiter (`Arb.scala`) is not used in this direct connection scheme.
  // It would be needed if multiple execution units contended for the RegFile write port
  // or if IFU and EXEU shared a memory bus that needed arbitration.
}
