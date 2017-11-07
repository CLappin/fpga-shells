// See LICENSE for license details.
package sifive.fpgashells.devices.xilinx.xilinxvcu118mig

import Chisel._
import chisel3.experimental.{Analog,attach}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.coreplex._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import sifive.fpgashells.ip.xilinx.vcu118mig.{VCU118MIGIOClocksReset, VCU118MIGIODDR, vcu118migc1}
import sifive.fpgashells.devices.xilinx.xilinxvc707mig.{XilinxVC707MIGParams}

class XilinxVCU118MIGPads extends VCU118MIGIODDR

class XilinxVCU118MIGIO extends VCU118MIGIODDR with VCU118MIGIOClocksReset

class XilinxVCU118MIGIsland(c : XilinxVC707MIGParams)(implicit p: Parameters) extends LazyModule with HasCrossing {
  val ranges = AddressRange.fromSets(c.address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val offset = ranges.head.base
  val depth = ranges.head.size
  val crossing = AsynchronousCrossing(8)
  require((depth<=0x80000000L),"vcu118mig supports upto 2GB depth configuraton")
  
  val device = new MemoryDevice
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
      slaves = Seq(AXI4SlaveParameters(
      address       = c.address,
      resources     = device.reg,
      regionType    = RegionType.UNCACHED,
      executable    = true,
      supportsWrite = TransferSizes(1, 256*8),
      supportsRead  = TransferSizes(1, 256*8))),
    beatBytes = 8)))

  lazy val module = new LazyModuleImp(this) {
    val io = IO(new Bundle {
      val port = new XilinxVCU118MIGIO
    })

    //MIG black box instantiation
    val blackbox = Module(new vcu118migc1)
    val (axi_async, _) = node.in(0)

    //ClocksReset
    blackbox.io.sys_rst                 := io.port.sys_rst
    blackbox.io.sys_clk_i               := io.port.sys_clk_i
    io.port.c0_init_calib_complete      := blackbox.io.c0_init_calib_complete
    io.port.c0_ddr4_ui_clk              := blackbox.io.c0_ddr4_ui_clk
    io.port.c0_ddr4_ui_clk_sync_rst     := blackbox.io.c0_ddr4_ui_clk_sync_rst
    io.port.dbg_clk                     := blackbox.io.dbg_clk

    blackbox.io.c0_ddr4_s_axi_aresetn  := io.port.c0_ddr4_s_axi_aresetn

    //pins to top level
    io.port <> blackbox.io

    val awaddr = axi_async.aw.bits.addr - UInt(offset)
    val araddr = axi_async.ar.bits.addr - UInt(offset)

    //slave AXI interface write address ports
    blackbox.io.c0_ddr4_s_axi_awid    := axi_async.aw.bits.id
    blackbox.io.c0_ddr4_s_axi_awaddr  := awaddr //truncated
    blackbox.io.c0_ddr4_s_axi_awlen   := axi_async.aw.bits.len
    blackbox.io.c0_ddr4_s_axi_awsize  := axi_async.aw.bits.size
    blackbox.io.c0_ddr4_s_axi_awburst := axi_async.aw.bits.burst
    blackbox.io.c0_ddr4_s_axi_awlock  := axi_async.aw.bits.lock
    blackbox.io.c0_ddr4_s_axi_awcache := UInt("b0011")
    blackbox.io.c0_ddr4_s_axi_awprot  := axi_async.aw.bits.prot
    blackbox.io.c0_ddr4_s_axi_awqos   := axi_async.aw.bits.qos
    blackbox.io.c0_ddr4_s_axi_awvalid := axi_async.aw.valid
    axi_async.aw.ready        := blackbox.io.c0_ddr4_s_axi_awready

    //slave interface write data ports
    blackbox.io.c0_ddr4_s_axi_wdata   := axi_async.w.bits.data
    blackbox.io.c0_ddr4_s_axi_wstrb   := axi_async.w.bits.strb
    blackbox.io.c0_ddr4_s_axi_wlast   := axi_async.w.bits.last
    blackbox.io.c0_ddr4_s_axi_wvalid  := axi_async.w.valid
    axi_async.w.ready         := blackbox.io.c0_ddr4_s_axi_wready

    //slave interface write response
    blackbox.io.c0_ddr4_s_axi_bready  := axi_async.b.ready
    axi_async.b.bits.id       := blackbox.io.c0_ddr4_s_axi_bid
    axi_async.b.bits.resp     := blackbox.io.c0_ddr4_s_axi_bresp
    axi_async.b.valid         := blackbox.io.c0_ddr4_s_axi_bvalid

    //slave AXI interface read address ports
    blackbox.io.c0_ddr4_s_axi_arid    := axi_async.ar.bits.id
    blackbox.io.c0_ddr4_s_axi_araddr  := araddr // truncated
    blackbox.io.c0_ddr4_s_axi_arlen   := axi_async.ar.bits.len
    blackbox.io.c0_ddr4_s_axi_arsize  := axi_async.ar.bits.size
    blackbox.io.c0_ddr4_s_axi_arburst := axi_async.ar.bits.burst
    blackbox.io.c0_ddr4_s_axi_arlock  := axi_async.ar.bits.lock
    blackbox.io.c0_ddr4_s_axi_arcache := UInt("b0011")
    blackbox.io.c0_ddr4_s_axi_arprot  := axi_async.ar.bits.prot
    blackbox.io.c0_ddr4_s_axi_arqos   := axi_async.ar.bits.qos
    blackbox.io.c0_ddr4_s_axi_arvalid := axi_async.ar.valid
    axi_async.ar.ready        := blackbox.io.c0_ddr4_s_axi_arready

    //slave AXI interface read data ports
    blackbox.io.c0_ddr4_s_axi_rready  := axi_async.r.ready
    axi_async.r.bits.id       := blackbox.io.c0_ddr4_s_axi_rid
    axi_async.r.bits.data     := blackbox.io.c0_ddr4_s_axi_rdata
    axi_async.r.bits.resp     := blackbox.io.c0_ddr4_s_axi_rresp
    axi_async.r.bits.last     := blackbox.io.c0_ddr4_s_axi_rlast
    axi_async.r.valid         := blackbox.io.c0_ddr4_s_axi_rvalid

  }
}

class XilinxVCU118MIG(c : XilinxVC707MIGParams)(implicit p: Parameters) extends LazyModule {
  val ranges = AddressRange.fromSets(c.address)
  val depth = ranges.head.size

  val buffer  = LazyModule(new TLBuffer)
  val toaxi4  = LazyModule(new TLToAXI4(adapterName = Some("mem"), stripBits = 1))
  val indexer = LazyModule(new AXI4IdIndexer(idBits = 4))
  val deint   = LazyModule(new AXI4Deinterleaver(p(CacheBlockBytes)))
  val yank    = LazyModule(new AXI4UserYanker)
  val island  = LazyModule(new XilinxVCU118MIGIsland(c))

  val node: TLInwardNode =
    island.node := island.crossAXI4In := yank.node := deint.node := indexer.node := toaxi4.node := buffer.node

  lazy val module = new LazyModuleImp(this) {
    val io = IO(new Bundle {
      val port = new XilinxVCU118MIGIO
    })

    io.port <> island.module.io.port

    island.module.clock := io.port.c0_ddr4_ui_clk
    island.module.reset := io.port.c0_ddr4_ui_clk_sync_rst
  }
}
