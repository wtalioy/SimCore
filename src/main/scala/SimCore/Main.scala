package SimCore

import chisel3._
import chisel3.util._
import _root_.circt.stage._

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
    val chiselStageOption = Seq(
      chisel3.stage.ChiselGeneratorAnnotation(() => core()),
      CIRCTTargetAnnotation(CIRCTTarget.Verilog)
    )
    val firtoolOptions = Seq(
      FirtoolOption("--lowering-options=disallowLocalVariables,locationInfoStyle=wrapInAtSquareBracket"),
      FirtoolOption("--split-verilog"),
      FirtoolOption("-o=" + build_dir),
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--preserve-aggregate=none"),
    )
    val executeOptions = chiselStageOption ++ firtoolOptions
    val executeArgs = Array("-td", build_dir)
    (new ChiselStage).execute(executeArgs, executeOptions)
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