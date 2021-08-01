package spitest;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.util.Console;
import com.pi4j.io.gpio.*;

import java.util.Arrays;

import java.io.IOException;

/**
 * This example code demonstrates how to perform basic SPI communications using the Raspberry Pi.
 * CS0 and CS1 (ship-select) are supported for SPI0.
 *
 * @author Robert Savage
 */
public class Test {

    // SPI device
    public static SpiDevice spi = null;

    // ADC channel count
    public static short ADC_CHANNEL_COUNT = 8;  // MCP3004=4, MCP3008=8

    // create Pi4J console wrapper/helper
    // (This is a utility class to abstract some of the boilerplate code)
    protected static final Console console = new Console();

    // SPI operations
    public static byte WRITE_CMD = 0x40;
    public static byte READ_CMD  = 0x41;

    public static int BG_CS_FRONT_BCM = 7;
    public static int BG_CS_BACK_BCM = 8;

    public static byte REG_ID = 0x00;
    public static byte REG_DATA_READY = 0x02;
    public static byte REG_MOTION_BURST = 0x16;
    public static byte REG_POWER_UP_RESET = 0x3a;
    public static byte REG_ORIENTATION = 0x5b;
    public static byte REG_RESOLUTION = 0x4e;  // PAA5100 only

    public static byte REG_RAWDATA_GRAB = 0x58;
    public static byte REG_RAWDATA_GRAB_STATUS = 0x59;

    public static void main(String args[]) throws InterruptedException, IOException {

        // GpioController gpio = GpioFactory.getInstance();
        // GpioPinDigitalOutput outputPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(BG_CS_FRONT_BCM), PinState.LOW);
        // Thread.sleep(50);
        // outputPin.high();


        

        // create SPI object instance for SPI for communication
        spi = SpiFactory.getInstance(SpiChannel.CS1,
                SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
                SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0

        // continue running program until user exits using CTRL-C
        while(console.isRunning()) {
            read();
            Thread.sleep(1000);
        }
        console.emptyLine();
    }

    /**
     * Read data via SPI bus from MCP3002 chip.
     * @throws IOException
     */
    public static void read() throws IOException, InterruptedException {
        for(short channel = 0; channel < ADC_CHANNEL_COUNT; channel++){
            byte[] conversion_value = getConversionValue(channel);
            // console.print(String.format(" | %04d", conversion_value)); // print 4 digits with leading zeros
            console.print(Arrays.toString(conversion_value));
        }
        console.print(" |\r");
        Thread.sleep(250);
    }


    /**
     * Communicate to the ADC chip via SPI to get single-ended conversion value for a specified channel.
     * @param channel analog input channel on ADC chip
     * @return conversion value for specified analog input channel
     * @throws IOException
     */
    public static byte[] getConversionValue(short channel) throws IOException {

        // create a data buffer and initialize a conversion request payload
        byte data[] = new byte[] {
                (byte) 0b00000001,                              // first byte, start bit
                (byte)(0b10000000 |( ((channel & 7) << 4))),    // second byte transmitted -> (SGL/DIF = 1, D2=D1=D0=0)
                (byte) 0b00000000                               // third byte transmitted....don't care
        };

        // send conversion request to ADC chip via SPI channel
        byte[] result = spi.write(data);

        return result;

        // // calculate and return conversion value from result bytes
        // int value = (result[1]<< 8) & 0b1100000000; //merge data[1] & data[2] to get 10-bit result
        // value |=  (result[2] & 0xff);
        // return value;
    }
}