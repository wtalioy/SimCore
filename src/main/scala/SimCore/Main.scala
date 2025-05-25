package SimCore

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

import SimCore.cpu.Core
import SimCore.cpu.GlobalConfig

/**
 * Main entry point for SimCore
 * Handles different build configurations and export targets
 */
object Main extends App {
  val target = if (args.length > 0) args(0) else "default"
  val build_dir = "./build"
  println(s"Build target: $target")
  
  def exportVerilog(core: () => chisel3.RawModule): Unit = {
    println("Export Verilog Started")
    
    // Use ChiselStage.emitSystemVerilog which is available in Chisel 7.0.0-RC1
    val chiselArgs = Array("--target-dir", build_dir)
    val firtoolOpts = Array(
      "--lowering-options=disallowLocalVariables,locationInfoStyle=wrapInAtSquareBracket",
      "--split-verilog",
      "--disable-all-randomization",
      "--preserve-aggregate=none"
    )
    
    // Use the object method instead of the instance method
    ChiselStage.emitSystemVerilogFile(
      core(),
      chiselArgs,
      firtoolOpts
    )
  }
  
  // Handle different build targets
  target match {
    case "simulation" => {
      GlobalConfig.SIM = true
      GlobalConfig.DEBUG = true
      exportVerilog(() => new Core())
    }
    case "synthesis" => {
      GlobalConfig.SIM = false
      GlobalConfig.DEBUG = false
      exportVerilog(() => new Core())
    }
    case _ => {
      // Default build
      exportVerilog(() => new Core())
    }
  }
} 