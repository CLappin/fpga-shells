// See LICENSE for license details.
package sifive.fpgashells.ip.xilinx.vcu118ddr4c1

import Chisel._
import chisel3.experimental.{Analog,attach}
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.util.GenericParameterizedBundle
import freechips.rocketchip.config._

// Black Box
class VCU118MIGIODDR(depth : BigInt) extends GenericParameterizedBundle(depth) {
  val c0_ddr4_act_n            = Bool(OUTPUT)
  val c0_ddr4_adr              = Bits(OUTPUT,17)
  val c0_ddr4_ba               = Bits(OUTPUT,3)
  val c0_ddr4_bg               = Bits(OUTPUT,1)
  val c0_ddr4_cke              = Bits(OUTPUT,1)
  val c0_ddr4_odt              = Bits(OUTPUT,1)
  val c0_ddr4_cs_n             = Bits(OUTPUT,1)
  val c0_ddr4_ck_t             = Bits(OUTPUT,1)
  val c0_ddr4_ck_c             = Bits(OUTPUT,1)
  val c0_ddr4_reset_n          = Bool(OUTPUT)
  val c0_ddr4_dm_dbi_n          = Analog(1.W)
  val c0_ddr4_dq               = Analog(64.W)
  val c0_ddr4_dqs_c            = Analog(8.W)
  val c0_ddr4_dqs_t            = Analog(8.W)
}

trait VCU118MIGIOClocksReset extends Bundle {
  val sys_rst                   = Bool(INPUT)
  // "NO_BUFFER" clock source (must be connected to IBUF outside of IP)
  val sys_clk_i             = Bool(INPUT)

  val c0_init_calib_complete    = Bool(OUTPUT)
  val c0_ddr4_ui_clk            = Clock(OUTPUT)
  val c0_ddr4_ui_clk_sync_rst   = Bool(OUTPUT)
  val dby_clk                   = Clock(OUTPUT)
 
  val c0_ddr4_s_axi_aresetn     = Bool(INPUT) 
}

//scalastyle:off
//turn off linter: blackbox name must match verilog module
class vc118ddr4c1(implicit val p:Parameters) extends BlackBox
{
  val io = new VCU118MIGIODDR with VCU118MIGIOClocksReset {

    //axi_s
    //slave interface write address ports
    val c0_ddr4_s_axi_awid            = Bits(INPUT,4)
    val c0_ddr4_s_axi_awaddr          = Bits(INPUT,28)
    val c0_ddr4_s_axi_awlen           = Bits(INPUT,8)
    val c0_ddr4_s_axi_awsize          = Bits(INPUT,3)
    val c0_ddr4_s_axi_awburst         = Bits(INPUT,2)
    val c0_ddr4_s_axi_awlock          = Bits(INPUT,1)
    val c0_ddr4_s_axi_awcache         = Bits(INPUT,4)
    val c0_ddr4_s_axi_awprot          = Bits(INPUT,3)
    val c0_ddr4_s_axi_awqos           = Bits(INPUT,4)
    val c0_ddr4_s_axi_awvalid         = Bool(INPUT)
    val c0_ddr4_s_axi_awready         = Bool(OUTPUT)
    //slave interface write data ports
    val c0_ddr4_s_axi_wdata           = Bits(INPUT,64)
    val c0_ddr4_s_axi_wstrb           = Bits(INPUT,8)
    val c0_ddr4_s_axi_wlast           = Bool(INPUT)
    val c0_ddr4_s_axi_wvalid          = Bool(INPUT)
    val c0_ddr4_s_axi_wready          = Bool(OUTPUT)
    //slave interface write response ports
    val c0_ddr4_s_axi_bready          = Bool(INPUT)
    val c0_ddr4_s_axi_bid             = Bits(OUTPUT,4)
    val c0_ddr4_s_axi_bresp           = Bits(OUTPUT,2)
    val c0_ddr4_s_axi_bvalid          = Bool(OUTPUT)
    //slave interface read address ports
    val c0_ddr4_s_axi_arid            = Bits(INPUT,4)
    val c0_ddr4_s_axi_araddr          = Bits(INPUT,28)
    val c0_ddr4_s_axi_arlen           = Bits(INPUT,8)
    val c0_ddr4_s_axi_arsize          = Bits(INPUT,3)
    val c0_ddr4_s_axi_arburst         = Bits(INPUT,2)
    val c0_ddr4_s_axi_arlock          = Bits(INPUT,1)
    val c0_ddr4_s_axi_arcache         = Bits(INPUT,4)
    val c0_ddr4_s_axi_arprot          = Bits(INPUT,3)
    val c0_ddr4_s_axi_arqos           = Bits(INPUT,4)
    val c0_ddr4_s_axi_arvalid         = Bool(INPUT)
    val c0_ddr4_s_axi_arready         = Bool(OUTPUT)
    //slave interface read data ports
    val c0_ddr4_s_axi_rready          = Bool(INPUT)
    val c0_ddr4_s_axi_rid             = Bits(OUTPUT,4)
    val c0_ddr4_s_axi_rdata           = Bits(OUTPUT,64)
    val c0_ddr4_s_axi_rresp           = Bits(OUTPUT,2)
    val c0_ddr4_s_axi_rlast           = Bool(OUTPUT)
    val c0_ddr4_s_axi_rvalid          = Bool(OUTPUT)
    //dbg
    val dbg_bus                       = Bits(OUTPUT,512) 
  }

 ElaborationArtefacts.add(
  modulename++".vivado.tcl",
   """create_ip -name ddr4 -vendor xilinx.com -library ip -version 2.2 -module_name vcu118ddr4c1 """ ++
   """set_property -dict [list CONFIG.C0_CLOCK_BOARD_INTERFACE {default_250mhz_clk1} """ ++
                            """CONFIG.Example_TG {ADVANCED_TG} """ ++
                            """CONFIG.C0.AxiSelection {true} """ ++
                            """CONFIG.C0.DDR4_AxiAddressWidth {28} """ ++
                            """CONFIG.C0.DDR4_TimePeriod {833} """ ++
                            """CONFIG.C0.DDR4_InputClockPeriod {4000} """ ++
                            """CONFIG.C0.DDR4_CLKOUT0_DIVIDE {5} """ ++
                            """CONFIG.C0.DDR4_MemoryPart {EDY4016AABG-DR-F} """ ++
                            """CONFIG.C0.DDR4_DataWidth {80} """ ++
                            """CONFIG.C0.DDR4_CasWriteLatency {12} """ ++
                            """CONFIG.Component_Name {vcu118ddr4c1} """ ++
                            """CONFIG.Debug_Signal {Disable} """ ++
                            """CONFIG.System_Clock {No_Buffer} """ ++
                            """CONFIG.C0.BANK_GROUP_WIDTH {1}] [get_ips vcu118ddr4c1]"""
  )

   
}
//scalastyle:on
