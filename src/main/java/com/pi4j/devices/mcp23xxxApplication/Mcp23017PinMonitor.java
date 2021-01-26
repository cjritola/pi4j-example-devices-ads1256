/*
 *
 *  *
 *  * -
 *  *   * #%L
 *  *   * **********************************************************************
 *  *   * ORGANIZATION  :  Pi4J
 *  *   * PROJECT       :  Pi4J :: EXTENSION
 *  *   * FILENAME      :  Mcp23017PinMonitor.java
 *  *   *
 *  *   * This file is part of the Pi4J project. More information about
 *  *   * this project can be found here:  https://pi4j.com/
 *  *   * **********************************************************************
 *    * %%
 *  *   * Copyright (C) 2012 - 2021 Pi4J
 *     * %%
 *    * Licensed under the Apache License, Version 2.0 (the "License");
 *    * you may not use this file except in compliance with the License.
 *    * You may obtain a copy of the License at
 *    *
 *    *      http://www.apache.org/licenses/LICENSE-2.0
 *    *
 *    * Unless required by applicable law or agreed to in writing, software
 *    * distributed under the License is distributed on an "AS IS" BASIS,
 *    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    * See the License for the specific language governing permissions and
 *    * limitations under the License.
 *    * #L%
 *  *
 *  *
 *
 *
 */

package com.pi4j.devices.mcp23xxxApplication;

import com.pi4j.Pi4J;
import com.pi4j.devices.appConfig.AppConfigUtilities;
import com.pi4j.devices.base_util.gpio.BaseGpioInOut;
import com.pi4j.devices.base_util.mapUtil.MapUtil;
import com.pi4j.devices.mcp23017.Mcp23017;

import com.pi4j.context.Context;
import com.pi4j.devices.base_util.ffdc.FfdcUtil;
import com.pi4j.devices.base_util.gpio.GpioPinCfgData;
import com.pi4j.devices.mcp23xxxCommon.Mcp23xxxUtil;
import com.pi4j.devices.mcp23xxxCommon.McpConfigData;
import com.pi4j.exception.LifecycleException;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.util.Console;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.HashMap;

import static java.lang.Integer.toHexString;

/**
 * Mcp23017PinMonitor
 *
 * <p>
 *     Read users parameters, control configuring the MCP23017 chip,
 *     reading and writing to pin, and more importantly monitor for
 *     changes on a specific input pin using the MCP23017 Interrupt
 *     line.
 * </p>
 */
public class Mcp23017PinMonitor extends Mcp23017  implements Mcp23xxxPinMonitorIntf{
    /**
     *   CTOR
     * @param pi4j  contect
     * @param parms  users command line parameters
     * @param ffdc    loging
     * @param dioPinData  chip pin configuration
     * @param console  Console
     */
    public Mcp23017PinMonitor(Context pi4j, Mcp23xxxParms parms ,FfdcUtil ffdc,
                              HashMap<Integer, GpioPinCfgData> dioPinData, Console console) {

        super(pi4j,parms,ffdc, dioPinData, console);
        this.jumpTable = new PinInterruptActionIntf[16];
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     *     Using the listener interface available on Pi GPios,assign an
     *     interruptAction to each pin.  The interrupt action for this example are
     *     subblasses of this packages PinInterruptBase.
     * </p>
     */
    public void installInterruptHandler() {
        // TODO Auto-generated method stub
        this.ffdc.ffdcMethodEntry("installInterruptHandler");

        for (int i = 0; i < 7; i++) {
            System.out.println("");
            PinInterruptDefault dummy = new PinInterruptDefault(this.pi4j, this.pin, this.ffdc, this,
                    this.dioPinData, this.cfgU, this.priChipName);
            this.jumpTable[i] = new PinInterruptActionIntf() {
                public void interruptAction(int pinNumber, DigitalState pinState) {
                    dummy.dummyAct(pinNumber, pinState);
                }
            };
        }

        PinInterruptLED action = new PinInterruptLED(this.pi4j, this.pin, this.ffdc, this, this.dioPinData, cfgU,
                this.priChipName);
        this.jumpTable[3] = new PinInterruptActionIntf() {
            public void interruptAction(int pinNumber, DigitalState pinState) {
                action.changeLed(pinNumber, pinState);
            }
        };
        this.jumpTable[4] = new PinInterruptActionIntf() {
            public void interruptAction(int pinNumber, DigitalState pinState) {
                action.changeLed(pinNumber, pinState);
            }
        };

        // 8 - 15
        for (int i = 8; i < 16; i++) {
            System.out.println("");
            PinInterruptDefault dummy = new PinInterruptDefault(this.pi4j, this.pin, this.ffdc, this,
                    this.dioPinData, cfgU, this.priChipName);
            this.jumpTable[i] = new PinInterruptActionIntf() {
                public void interruptAction(int pinNumber, DigitalState pinState) {
                    dummy.dummyAct(pinNumber, pinState);
                }
            };
        }
        PinInterruptLED ledAction = new PinInterruptLED(this.pi4j, this.pin, this.ffdc, this, this.dioPinData,
                cfgU, this.priChipName);
        this.jumpTable[15] = new PinInterruptActionIntf() {
            public void interruptAction(int pinNumber, DigitalState pinState) {
                ledAction.changeLed(pinNumber, pinState);
            }
        };

        this.ffdc.ffdcMethodExit("installInterruptHandler");

    }

    /**
     *<p>
     *     For this piNum, call interruptAction on the interruptAction instance
     *     contained in the jumpTable
     *</p>
     * @param pinNum     MVP pin causing the interrupt
     * @param pinState   Pi Gpio pin state detected
     * @param ffdc       logging
     * @return  true if interrupt processed, false if failed
     */
    public boolean processPinInterrupt(int pinNum, DigitalState pinState, FfdcUtil ffdc) {
        boolean rval = true;
        this.ffdc.ffdcMethodEntry("Application processPinInterrupt PIN " + pinNum);// figure
        this.jumpTable[pinNum].interruptAction(pinNum, pinState);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            rval = false;
        }
        this.ffdc.ffdcMethodExit("Application processPinInterrupt  rval :" + rval);

        return (rval);
    }


    /**
     *  main
     *  <p>
     *      Command line access to the MCP23017 application code.
     *      First process the users input, then call various methods
     *      based upon the users parms.
     *  </p>
     * @param args   Users command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        var console = new Console();
        Context pi4j = Pi4J.newAutoContext();

        // Print program title/header
        console.title("<-- The Pi4J V2 Project Extension  -->", "Mcp23017PinMonitor");

        HashMap<Integer, GpioPinCfgData> dioPinData = new HashMap<Integer, GpioPinCfgData>();


        // mcpObj.monitor_intrp = true;


        Mcp23xxxParms parmsObj = Mcp23xxxAppProcessParms.processMain(pi4j, args, true, dioPinData, console);

        FfdcUtil ffdc = new FfdcUtil(console, pi4j, parmsObj.ffdcControlLevel, Mcp23017PinMonitor.class);
        // String ss = String.format("0x%04X", address);
        // System.out.println(String.format("0x%08X", 234));
       ffdc.ffdcDebugEntry("mcp23017 : Arg processing completed...\n" +
                "");

        Mcp23017PinMonitor mcpObj = new Mcp23017PinMonitor(parmsObj.pi4j, parmsObj, ffdc,  dioPinData, console);

        BaseGpioInOut gpio = new BaseGpioInOut(parmsObj.pi4j, mcpObj.ffdc, mcpObj.dioPinData);
        mcpObj.gpio = gpio;

        AppConfigUtilities cfgU = new AppConfigUtilities(parmsObj.pi4j, ffdc, mcpObj.gpio, console);
        mcpObj.cfgU = cfgU;

        mcpObj.mapUtils = new MapUtil(mcpObj.ffdc, mcpObj.gpio);


        // HashMap<Integer, GpioPinCfgData> dioPinData = new HashMap<Integer, GpioPinCfgData>();
        //cfgU.dioPinData = dioPinData;

        // the bus is that of the main chip. The one connected to the Pi i2c
        // bus. The priChipBus may be some other
        // value if the prichipName is behind a mux.
        // If behind mux that was accounted for in the above call
        // cfgU.enableGpioPath
        HashMap<String, String> chipDetails = cfgU.getChipMapRec(parmsObj.mainChip);
        if (chipDetails != null) {
            String chipBus = chipDetails.get("busNum");
            String chipAddr = chipDetails.get("address");
            parmsObj.address = Integer.parseInt(chipAddr.substring(2), 16);
            parmsObj.bus_num = Integer.parseInt(chipBus.substring(2), 16);
        }
        HashMap<String, String> priChipDetails = cfgU.getChipMapRec(mcpObj.priChipName);
        if (chipDetails != null) {
            String chipBus = priChipDetails.get("busNum");
            String chipAddr = priChipDetails.get("address");
            parmsObj.priChipAddress = Integer.parseInt(chipAddr.substring(2), 16);
            parmsObj.priChipBus_num = Integer.parseInt(chipBus.substring(2), 16);
          // TODO  parmsObj.address = Integer.parseInt(chipAddr.substring(2), 16);
        }

        mcpObj.cfgData = new McpConfigData(ffdc);

        // this
        System.out.println("Args to enable " + parmsObj.pinName + "   " + mcpObj.priChipName);
        boolean returned = cfgU.enableGpioPath(parmsObj.pinName, mcpObj.priChipName); // path
        // to
        // mcp23xx


        // do any extra interrupt handler setup
        mcpObj.installInterruptHandler();


        // set bus_num and address based on mcpObj.priChipName

        // the bus is that of the main chip. The one connected to the Pi i2c
        // bus. The priChipBus may be some other
        // value if the prichipName is behind a mux.
        // If behind mux that was accounted for in the above call
        // cfgU.enableGpioPath
        Mcp23xxxUtil mcpUtil = new Mcp23xxxUtil(parmsObj.pi4j, ffdc, parmsObj.bus_num, parmsObj.priChipAddress, mcpObj.cfgData, mcpObj, console);

        // Prior to running methods, set up control-c handler
        Signal.handle(new Signal("INT"), new SignalHandler() {
            public void handle(Signal sig) {
                System.out.println("Performing ctl-C shutdown");
                ffdc.ffdcFlushShutdown(); // push all logs to the file
                try {
                    pi4j.shutdown();
                } catch (LifecycleException e) {
                    e.printStackTrace();
                }
                Thread.dumpStack();
                System.exit(2);
            }
        });


        if (parmsObj.has_full_keyed_data) { // -z
            HashMap<String, HashMap<String, String>> outerMap = mcpObj.mapUtils.createFullMap(parmsObj.full_keyed_data);
            mcpObj.cfgData.replaceMap(outerMap);
            // mcpObj.cfgData.replaceMap(outerMap);
            // create GPIO Pi pins
            // mcpObj.configUtils.DumpGpiosConfig(mcpObj.cfgData);
            gpio.createGpioInstance(mcpObj.cfgData.getFullMap());
        }

        if (parmsObj.do_reset) {
            mcpObj.reset_chip();
            // mcpObj.dump_regs();
        }

        // do this before pin data as this will set 'banked', needed for correct
        // addressing
        if (parmsObj.has_IOCON_keyed_data) { // -k
            HashMap<String, HashMap<String, String>> mMap;
            mMap = mcpObj.mapUtils.createFullMap(parmsObj.IOCON_keyed_data);
            mcpObj.cfgData.replaceMap(mMap);
            mcpUtil.process_keyed_data();
        }

        if (parmsObj.has_full_pin_keyed_data) { // -m
            HashMap<String, HashMap<String, String>> mMap;
            mMap = mcpObj.mapUtils.createFullMap(parmsObj.full_pin_keyed_data);
            mcpObj.cfgData.replaceMap(mMap);
            mcpUtil.process_keyed_data();
        }

        System.out.println("Chip register configurations completed");
        mcpObj.reinit(parmsObj.priChipName, parmsObj.pinName,parmsObj.bus_num, parmsObj.priChipAddress);

        if (parmsObj.dumpRegs) {
            mcpObj.dump_regs();
            System.exit(0);
        }

       // mcpObj.cfgData.DumpGpiosConfig();

        if (parmsObj.set_pin) {
            try {
                mcpObj.drive_pin(parmsObj.pin, parmsObj.pin_on);
            } catch (InterruptedException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (parmsObj.read_pin) {
            try {
                mcpObj.read_input(parmsObj.pin);
            } catch (InterruptedException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (parmsObj.monitor_intrp) {
            // spin and handle any interrupt that happens
            if ((parmsObj.gpio_num == 0xff) ) { // || (parmsObj.has_up_down == false)
                mcpObj.ffdc.ffdcConfigWarningEntry("Option -i requires -g ");
                mcpObj.ffdc.ffdcDebugEntry("Spin so any Monitors can execute");
                mcpObj.ffdc.ffdcErrorExit("invalid parms supplied", 550);
            } else {
                mcpObj.addListener(parmsObj.off_on,  parmsObj.gpio_num);
                while (true) {
                    try {
                        Thread.sleep(2000, 0);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        // mcpObj.dump_regs(); // comment out or interrupt details may get
        // cleared for usage in interrupt procerssing
        // cfgU.displayEnableReg(mcpObj.bus_num, mainChip);
        // cfgU.runCli();

        mcpObj.ffdc.ffdcDebugEntry("program ending normal");
        // TODO
        // mcpObj.ffdc.displayFfdcEntries();
        // mcpObj.ffdc.displayFfdcConfigWarningEntries();
        //
        ffdc.ffdcFlushShutdown(); // push all logs to the file

        // Shutdown Pi4J
        pi4j.shutdown();


    }





}