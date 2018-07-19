// See LICENSE for license details.
package sifive.fpgashells.shell

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import sifive.blocks.devices.uart._
import freechips.rocketchip.subsystem.{BaseSubsystem, PeripheryBus, PeripheryBusKey}

case class UARTOverlayParams(beatBytes: Int, uartParams: UARTParams, devName: Option[String])(implicit val p: Parameters)
case object UARTOverlayKey extends Field[Seq[DesignOverlay[UARTOverlayParams, TLUART]]](Nil)

// Tack on cts, rts signals available on some FPGAs. They are currently unused
// by our designs.
class FPGAUARTPortIO extends UARTPortIO {
  val rtsn = Output(Bool())
  val ctsn = Input(Bool())
}

class UARTReplacementBundle extends Bundle with HasUARTTopBundleContents

class BundleBridgeUART[D <: Data, T <: LazyModule](lm: => T { val module: { val io: D }})(implicit p: Parameters) extends LazyModule
{
  val child = LazyModule(lm)
  val ioNode = BundleBridgeSource(() => new UARTReplacementBundle())
  override lazy val desiredName = s"BundleBridge_${child.desiredName}"

  lazy val module = new LazyModuleImp(this) {
    val (io, _) = ioNode.out(0)
    io <> child.module.io
  }
}

object BundleBridgeUART
{
  def apply[D <: Data, T <: LazyModule](lm: => T { val module: { val io: D }})(implicit p: Parameters, valName: ValName) =
    LazyModule(new BundleBridgeUART(lm))
}



abstract class UARTOverlay(
  val params: UARTOverlayParams)
    extends IOOverlay[FPGAUARTPortIO, TLUART]
{
  implicit val p = params.p

  def ioFactory = new FPGAUARTPortIO

  val uartSource = BundleBridgeUART(new TLUART(params.beatBytes, params.uartParams).suggestName(params.devName))
  val uartSink = shell { uartSource.ioNode.sink }

  val designOutput = uartSource.child

  shell { InModuleBody {
    io.txd := uartSink.io.port.txd

    // Some FPGAs have this, we don't use it.
    io.rtsn := false.B
  } }
}
