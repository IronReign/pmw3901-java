package spitest;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.util.Console;

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

    /**
     * Sample SPI Program
     *
     * @param args (none)
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String args[]) throws InterruptedException, IOException {

        // print program title/header
        console.title("<-- The Pi4J Project -->", "SPI test program using MCP3004/MCP3008 AtoD Chip");

        // allow for user to exit program using CTRL-C
        console.promptForExit();

        // This SPI example is using the Pi4J SPI interface to communicate with
        // the SPI hardware interface connected to a MCP3004/MCP3008 AtoD Chip.
        //
        // Please make sure the SPI is enabled on your Raspberry Pi via the
        // raspi-config utility under the advanced menu option.
        //
        // see this blog post for additional details on SPI and WiringPi
        // http://wiringpi.com/reference/spi-library/
        //
        // see the link below for the data sheet on the MCP3004/MCP3008 chip:
        // http://ww1.microchip.com/downloads/en/DeviceDoc/21294E.pdf

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