package simcore.cpu.utils.interfaces

import chisel3._
import chisel3.util._
import simcore.cpu.utils.constants.BPTypes

/**
 * Branch Prediction Unit interfaces
 */
class BTBInIO(pcBits: Int) extends Bundle {
    val pc = UInt(pcBits.W)
}

class BTBOutIO(entryNum: Int, pcBits: Int) extends Bundle {
    val taken = Bool()
    val target = UInt(pcBits.W)
    val hit = Bool()
    val index = UInt(log2Ceil(entryNum).W)
}

class BTBUpdateIO(entryNum: Int, pcBits: Int) extends Bundle {
    val bp_type = BPTypes()
    val taken = Bool() // cond jump taken or not
    val pc = UInt(pcBits.W)
    val target = UInt(pcBits.W)
    val hit = Bool()
    val index = UInt(log2Ceil(entryNum).W) // hit btb index
    val valid = Bool()
} 