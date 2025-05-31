package simcore.cpu.utils

import chisel3._
import chisel3.util._

object isPowerOf2{
  def apply(x: Int): Boolean = {
    x > 0 && (x & (x-1)) == 0
  }
}

object OHis{
  def apply(sig: UInt, pattern: UInt): Bool = {
    sig === pattern
  }
}