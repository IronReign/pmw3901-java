package spitest;

import com.pi4j.io.spi.SpiDevice;

import java.io.IOException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.wiringpi.Spi;

public class PMW3901 {


    // SPI operations
    public static byte WRITE_CMD = 0x40;
    public static byte READ_CMD  = 0x41;

    public static int WAIT = -1;

    public static int BG_CS_FRONT_BCM = 7;
    public static int BG_CS_BACK_BCM = 8;

    public static int REG_ID = 0x00;
    public static int REG_DATA_READY = 0x02;
    public static int REG_MOTION_BURST = 0x16;
    public static int REG_POWER_UP_RESET = 0x3a;
    public static int REG_ORIENTATION = 0x5b;
    public static int REG_RESOLUTION = 0x4e; //PAA5100 only

    public static int REG_RAWDATA_GRAB = 0x58;
    public static int REG_RAWDATA_GRAB_STATUS = 0x59;

    private int spi_cs_gpio;
    GpioPinDigitalOutput spi_cs_gpio_output;

    public PMW3901(int spi_port, int spi_cs, int spi_cs_gpio) {
        if(spi_cs_gpio == -1)
            this.spi_cs_gpio = BG_CS_FRONT_BCM;
        else
            this.spi_cs_gpio = spi_cs_gpio;
        
        int fd = Spi.wiringPiSPISetup(0, 400000);
        if (fd <= -1) {
            System.out.println(" ==>> SPI SETUP FAILED");
            return;
        }

        GpioController gpio = GpioFactory.getInstance();
        spi_cs_gpio_output = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(spi_cs_gpio), PinState.LOW);
        spi_cs_gpio_output.low();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        spi_cs_gpio_output.high();

        write(REG_POWER_UP_RESET, 0x5a);
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for(int offset = 0; offset < 5; offset++)
            read(REG_DATA_READY + offset);

        _secret_sauce();

        int[] id = self.get_id()
        if id[0] != 0x49 or id[1] != 0x00:
            raise RuntimeError("Invalid Product ID or Revision for PMW3901: 0x{:02x}/0x{:02x}".format(product_id, revision))

    }

    public void get_id(){
        return read(REG_ID, 2);
    }

    public void set_rotation(float degrees) {
        if (degrees == 0)
            set_orientation(True, True, True);
        else if (degrees == 90)
            set_orientation(False, True, False);
        else if (degrees == 90)
            set_orientation(False, False, True);
        else if (degrees == 90)
            set_orientation(True, False, False);
        else
            throw new IllegalArgumentException("Degrees must be one of 0, 90, 180, or 270");
    }

    public void set_orientation(boolean invert_x, boolean invert_y, boolean swap_xy) {
        /**Set orientation of PMW3901 manually.
        Swapping is performed before flipping.
        :param invert_x: invert the X axis
        :param invert_y: invert the Y axis
        :param swap_xy: swap the X/Y axes
        **/
        int value = 0;
        if (swap_xy)
            value |= 0b10000000;
        if (invert_y)
            value |= 0b01000000;
        if (invert_x)
            value |= 0b00100000;
        write(REG_ORIENTATION, value);   
    }

    // private void write(int register, int value) {
    //     spi_cs_gpio_output.low();
    //     byte[] packet = new byte[] {
    //         WRITE_CMD,
    //         (byte) register,
    //         (byte) value
    //     };
    //     try {
    //         spi_dev.write(packet);
    //     } catch (IOException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
    //     spi_cs_gpio_output.high();
    // }

    private void write(int register, int value) {
        spi_cs_gpio_output.low();
        
        spi_cs_gpio_output.high();
    }

    private byte[] read(int register) {
        spi_cs_gpio_output.low();
        byte[] packet = new byte[] {
            READ_CMD,
            (byte) register,
            0b00000000
        };
        byte[] result = null;
        try {
            result = spi_dev.write(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        spi_cs_gpio_output.high();
        return result;
    }

    private void bulk_write(int[] data) {
        for(int i = 0; i < data.length; i += 2) {
            byte register = (byte) data[i];
            byte value = (byte) data[i+1];

            if((int) register == WAIT)
                try {
                    Thread.sleep(value);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            else
                write(register, value);
        }
    }

    private void secret_sauce() {
        /**Write the secret sauce registers.
        Don't ask what these do, the datasheet refuses to explain.
        They are some proprietary calibration magic.
        **/
        bulk_write(new int[] {
            0x7f, 0x00,
            0x55, 0x01,
            0x50, 0x07,

            0x7f, 0x0e,
            0x43, 0x10
        });
        if((read(0x67)[0] & 0b10000000) == 1)
            write(0x48, 0x04);
        else
            write(0x48, 0x02);
        bulk_write(new int[] {
            0x7f, 0x00,
            0x51, 0x7b,

            0x50, 0x00,
            0x55, 0x00,
            0x7f, 0x0E
        });
        if(read(0x73)[0] == 0x00) {
            int c1 = read(0x70)[0];
            int c2 = read(0x71)[0];
            if(c1 <= 28)
                c1 += 14;
            if (c1 > 28)
                c1 += 11;
            c1 = (int) Math.max(0, Math.min(0x3F, c1));
            c2 = (int) ((c2 * 45) / 100);
            bulk_write( new int[] {
                0x7f, 0x00,
                0x61, 0xad,
                0x51, 0x70,
                0x7f, 0x0e
            });
            write(0x70, c1);
            write(0x71, c2);
        }
        bulk_write(new int[] {
            0x7f, 0x00,
            0x61, 0xad,
            0x7f, 0x03,
            0x40, 0x00,
            0x7f, 0x05,

            0x41, 0xb3,
            0x43, 0xf1,
            0x45, 0x14,
            0x5b, 0x32,
            0x5f, 0x34,
            0x7b, 0x08,
            0x7f, 0x06,
            0x44, 0x1b,
            0x40, 0xbf,
            0x4e, 0x3f,
            0x7f, 0x08,
            0x65, 0x20,
            0x6a, 0x18,

            0x7f, 0x09,
            0x4f, 0xaf,
            0x5f, 0x40,
            0x48, 0x80,
            0x49, 0x80,

            0x57, 0x77,
            0x60, 0x78,
            0x61, 0x78,
            0x62, 0x08,
            0x63, 0x50,
            0x7f, 0x0a,
            0x45, 0x60,
            0x7f, 0x00,
            0x4d, 0x11,

            0x55, 0x80,
            0x74, 0x21,
            0x75, 0x1f,
            0x4a, 0x78,
            0x4b, 0x78,

            0x44, 0x08,
            0x45, 0x50,
            0x64, 0xff,
            0x65, 0x1f,
            0x7f, 0x14,
            0x65, 0x67,
            0x66, 0x08,
            0x63, 0x70,
            0x7f, 0x15,
            0x48, 0x48,
            0x7f, 0x07,
            0x41, 0x0d,
            0x43, 0x14,

            0x4b, 0x0e,
            0x45, 0x0f,
            0x44, 0x42,
            0x4c, 0x80,
            0x7f, 0x10,

            0x5b, 0x02,
            0x7f, 0x07,
            0x40, 0x41,
            0x70, 0x00,
            WAIT, 0x0A,  // Sleep for 10ms

            0x32, 0x44,
            0x7f, 0x07,
            0x40, 0x40,
            0x7f, 0x06,
            0x62, 0xf0,
            0x63, 0x00,
            0x7f, 0x0d,
            0x48, 0xc0,
            0x6f, 0xd5,
            0x7f, 0x00,

            0x5b, 0xa0,
            0x4e, 0xa8,
            0x5a, 0x50,
            0x40, 0x80,
            WAIT, 0xF0,

            0x7f, 0x14,  // Enable LED_N pulsing
            0x6f, 0x1c,
            0x7f, 0x00
        });
    }

    public double[] get_motion(int timeout) {
        /**Get motion data from PMW3901 using burst read.
        Reads 12 bytes sequentially from the PMW3901 and validates
        motion data against the SQUAL and Shutter_Upper values.
        Returns Delta X and Delta Y indicating 2d flow direction
        and magnitude.
        :param timeout: Timeout in seconds
        **/
        long t_start = System.currentTimeMillis();
        while(System.currentTimeMillis() - t_start < timeout) {
            // self.cs_pin.reset()
            // data = self.spi_dev.xfer2([REG_MOTION_BURST] + [0 for x in range(12)])
            // self.cs_pin.set()
            // (_, dr, obs,
            //  x, y, quality,
            //  raw_sum, raw_max, raw_min,
            //  shutter_upper,
            //  shutter_lower) = struct.unpack("<BBBhhBBBBBB", bytearray(data))

            // if dr & 0b10000000 and not (quality < 0x19 and shutter_upper == 0x1f):
            //     return x, y

            Thread.sleep(10);
        }
        throw new Exception("Timed out waiting for motion data.");
    }
}
