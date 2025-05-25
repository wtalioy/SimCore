package SimCore.cpu

import chisel3._
import chisel3.util._

// Import stage modules
import SimCore.cpu.stages.IF
import SimCore.cpu.stages.ID
import SimCore.cpu.stages.EXE
import SimCore.cpu.stages.MEM
import SimCore.cpu.stages.WB

// Import component modules
import SimCore.cpu.components.RegFile
import SimCore.cpu.components.HazardDetectionUnit
import SimCore.cpu.components.ForwardingUnit
import SimCore.cpu.components.PipelineControlUnit
import SimCore.cpu.utils.ForwardingSelects

// Import pipeline interfaces
import SimCore.cpu.utils.IBusIO
import SimCore.cpu.utils.DBusIO
import SimCore.cpu.utils.IFID_Bundle
import SimCore.cpu.utils.IDEX_Bundle
import SimCore.cpu.utils.EXMEM_Bundle
import SimCore.cpu.utils.MEMWB_Bundle

// Import pipeline register module
import SimCore.cpu.pipeline.PipelineRegister

/**
 * CPU Core
 * Five-stage pipeline implementation
 */
class Core extends Module {
  val io = IO(new Bundle {
    // Instruction memory interface
    val ibus = new IBusIO()
    
    // Data memory interface
    val dbus = new DBusIO()
  })

  //==========================================================================
  // Pipeline Stage Module Instantiation
  //==========================================================================
  val ifu = Module(new IF())
  val idu = Module(new ID())
  val exeu = Module(new EXE())
  val memu = Module(new MEM())
  val wbsu = Module(new WB())
  
  // Standard 32-bit RISC-V regfile with 2 read ports and 1 write port
  val regFile = Module(new RegFile(dataBits = 32, regCount = 32, readPorts = 2, writePorts = 1))
  
  //==========================================================================
  // Control Module Instantiation
  //==========================================================================
  val hazardUnit = Module(new HazardDetectionUnit())
  val forwardingUnit = Module(new ForwardingUnit())
  val pipelineControl = Module(new PipelineControlUnit())

  //==========================================================================
  // Pipeline Register Instantiation
  //==========================================================================
  val ifIdReg = Module(new PipelineRegister(new IFID_Bundle()))
  val idExReg = Module(new PipelineRegister(new IDEX_Bundle()))
  val exMemReg = Module(new PipelineRegister(new EXMEM_Bundle()))
  val memWbReg = Module(new PipelineRegister(new MEMWB_Bundle()))

  //==========================================================================
  // Pipeline Control Connections
  //==========================================================================
  // Connect hazard detection unit
  hazardUnit.io.id_rs1_addr := idu.io.rs1_addr
  hazardUnit.io.id_rs2_addr := idu.io.rs2_addr
  hazardUnit.io.id_uses_rs1 := idu.io.uses_rs1
  hazardUnit.io.id_uses_rs2 := idu.io.uses_rs2
  hazardUnit.io.ex_rd_addr := idExReg.io.out.rd_addr
  hazardUnit.io.ex_is_load := idExReg.io.out.ctrl.mem_read
  
  // Connect forwarding unit
  forwardingUnit.io.rs1_addr := idExReg.io.out.rs1_addr
  forwardingUnit.io.rs2_addr := idExReg.io.out.rs2_addr
  forwardingUnit.io.uses_rs1 := idExReg.io.out.ctrl.uses_rs1
  forwardingUnit.io.uses_rs2 := idExReg.io.out.ctrl.uses_rs2
  forwardingUnit.io.ex_mem_rd_addr := exMemReg.io.out.rd_addr
  forwardingUnit.io.mem_wb_rd_addr := memWbReg.io.out.rd_addr
  forwardingUnit.io.ex_mem_reg_write := exMemReg.io.out.ctrl.reg_write
  forwardingUnit.io.mem_wb_reg_write := memWbReg.io.out.ctrl.reg_write
  
  // Connect pipeline control unit
  pipelineControl.io.mem_stall := memu.io.stall_out
  pipelineControl.io.load_use_stall := hazardUnit.io.load_use_stall
  pipelineControl.io.branch_taken := exeu.io.branch_taken

  //==========================================================================
  // Pipeline Stage 1: Instruction Fetch
  //==========================================================================
  // Connect memory interface
  io.ibus <> ifu.io.ibus

  // Control flow signals
  ifu.io.stall := pipelineControl.io.pipeline_stall
  ifu.io.redirect_valid := exeu.io.branch_taken
  ifu.io.redirect_target := exeu.io.branch_target

  // IF/ID register control
  ifIdReg.io.in := ifu.io.out
  ifIdReg.io.stall := pipelineControl.io.pipeline_stall
  ifIdReg.io.flush := pipelineControl.io.if_id_flush

  //==========================================================================
  // Pipeline Stage 2: Instruction Decode
  //==========================================================================
  // Connect to ID stage
  idu.io.in := ifIdReg.io.out
  
  // Connect to register file for reading
  regFile.io.raddr(0) := idu.io.rs1_addr
  regFile.io.raddr(1) := idu.io.rs2_addr
  
  // ID/EX register input
  val idExInput = Wire(new IDEX_Bundle())
  
  // Prepare ID/EX register input
  when (hazardUnit.io.load_use_stall || exeu.io.branch_taken) {
    idExInput := IDEX_Bundle.NOP
  }.otherwise {
    idExInput.pc := idu.io.pc
    idExInput.rs1_addr := idu.io.rs1_addr
    idExInput.rs2_addr := idu.io.rs2_addr
    idExInput.rs1_data := regFile.io.rdata(0)
    idExInput.rs2_data := regFile.io.rdata(1)
    idExInput.rd_addr := idu.io.rd_addr
    idExInput.imm := idu.io.imm
    idExInput.ctrl := idu.io.ctrl
    idExInput.valid := idu.io.valid
  }
  
  // ID/EX register control
  idExReg.io.in := idExInput
  idExReg.io.stall := memu.io.stall_out
  idExReg.io.flush := pipelineControl.io.id_ex_flush

  //==========================================================================
  // Pipeline Stage 3: Execute
  //==========================================================================
  // Apply forwarding to register data
  val forwardedRs1Data = MuxLookup(forwardingUnit.io.forward_rs1_sel, idExReg.io.out.rs1_data)(
    Seq(
      ForwardingSelects.FORWARD_FROM_MEM -> exMemReg.io.out.alu_result,
      ForwardingSelects.FORWARD_FROM_WB -> memWbReg.io.out.result
    )
  )
  
  val forwardedRs2Data = MuxLookup(forwardingUnit.io.forward_rs2_sel, idExReg.io.out.rs2_data)(
    Seq(
      ForwardingSelects.FORWARD_FROM_MEM -> exMemReg.io.out.alu_result,
      ForwardingSelects.FORWARD_FROM_WB -> memWbReg.io.out.result
    )
  )

  // Connect to EX stage
  exeu.io.in.pc := idExReg.io.out.pc
  exeu.io.in.rs1_data := forwardedRs1Data
  exeu.io.in.rs2_data := forwardedRs2Data
  exeu.io.in.rd_addr := idExReg.io.out.rd_addr
  exeu.io.in.imm := idExReg.io.out.imm
  exeu.io.in.ctrl := idExReg.io.out.ctrl
  exeu.io.in.valid := idExReg.io.out.valid
  
  // EX/MEM register input
  val exMemInput = Wire(new EXMEM_Bundle())
  exMemInput.pc := idExReg.io.out.pc
  exMemInput.alu_result := exeu.io.out.result
  exMemInput.rd_addr := exeu.io.out.rd_addr
  exMemInput.rs2_data := forwardedRs2Data // For store operations
  exMemInput.ctrl.mem_read := idExReg.io.out.ctrl.mem_read
  exMemInput.ctrl.mem_write := idExReg.io.out.ctrl.mem_write
  exMemInput.ctrl.mem_to_reg := idExReg.io.out.ctrl.mem_to_reg
  exMemInput.ctrl.reg_write := idExReg.io.out.ctrl.reg_write
  exMemInput.valid := exeu.io.out.valid
  
  // EX/MEM register control
  exMemReg.io.in := exMemInput
  exMemReg.io.stall := memu.io.stall_out
  exMemReg.io.flush := pipelineControl.io.ex_mem_flush
  
  //==========================================================================
  // Pipeline Stage 4: Memory Access
  //==========================================================================
  // Connect to MEM stage
  memu.io.in := exMemReg.io.out
  
  // Connect memory interface
  io.dbus <> memu.io.dbus
  
  // MEM/WB register control
  memWbReg.io.in := memu.io.out
  memWbReg.io.stall := false.B // MEM/WB typically not stalled
  memWbReg.io.flush := pipelineControl.io.mem_wb_flush

  //==========================================================================
  // Pipeline Stage 5: Write Back
  //==========================================================================
  // Connect to WB stage
  wbsu.io.in := memWbReg.io.out
  
  // Register file write connections
  regFile.io.waddr(0) := wbsu.io.rd_addr
  regFile.io.wdata(0) := wbsu.io.rd_data
  regFile.io.wen(0) := wbsu.io.rd_write_en
}
