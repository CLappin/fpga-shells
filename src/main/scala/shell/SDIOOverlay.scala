// See LICENSE for license details.
package sifive.fpgashells.shell

import chisel3._
import chisel3.experimental.Analog
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import sifive.blocks.devices.spi._

case class SDIOOverlayParams(beatBytes: Int, spiParam: SPIParams, devName: Option[String])(implicit val p: Parameters)
case object SDIOOverlayKey extends Field[Seq[DesignOverlay[SDIOOverlayParams, TLSPI]]](Nil)

// SDIO Port. Not sure how generic this is, it might need to move.
class FPGASDIOPortIO extends Bundle {
  val sdio_clk = Output(Bool())
  val sdio_cmd = Analog(1.W)
  val sdio_dat_0 = Analog(1.W)
  val sdio_dat_1 = Analog(1.W)
  val sdio_dat_2 = Analog(1.W)
  val sdio_dat_3 = Analog(1.W)
}

// HACK that'll go away with new BundleBridge API

class SDIOReplacementBundle(val c: SPIParams) extends Bundle {
  val port = new SPIPortIO(c)
  val sdioClock = Output(Clock())
  val sdioReset = Output(Bool())
}

class BundleBridgeSDIO[D <: Data, T <: LazyModule](lm: => T { val module: { val io: D }}, spiParam: SPIParams)(implicit p: Parameters) extends LazyModule
{
  val child = LazyModule(lm)
  val ioNode = BundleBridgeSource(() => new SDIOReplacementBundle(spiParam))
  override lazy val desiredName = s"BundleBridge_${child.desiredName}"

  lazy val module = new LazyModuleImp(this) {
    val (io, _) = ioNode.out(0)
    io <> child.module.io
    io.sdioClock := clock
    io.sdioReset := reset
  }
}

object BundleBridgeSDIO
{
  def apply[D <: Data, T <: LazyModule](lm: => T { val module: { val io: D }}, spiParam: SPIParams)(implicit p: Parameters, valName: ValName) =
    LazyModule(new BundleBridgeSDIO(lm, spiParam))
}

abstract class SDIOOverlay(
  val params: SDIOOverlayParams)
    extends IOOverlay[FPGASDIOPortIO, TLSPI]
{
  implicit val p = params.p

  def ioFactory = new FPGASDIOPortIO

  val spiSource = BundleBridgeSDIO(new TLSPI(params.beatBytes, params.spiParam).suggestName(params.devName), params.spiParam)
  val spiSink = shell { spiSource.ioNode.sink }

  val designOutput = spiSource.child
}
