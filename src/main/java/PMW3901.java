

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Spi;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayShort;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;

public class PMW3901 {

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

    public PMW3901(int spi_port, int spi_cs) throws Exception{
        this(spi_port, spi_cs, BG_CS_FRONT_BCM);
    }

    public PMW3901(int spi_port, int spi_cs, int _spi_cs_gpio) throws Exception {
        spi_cs_gpio = _spi_cs_gpio;
        
        int fd = Spi.wiringPiSPISetupMode(0, 400000, 3);
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

        secret_sauce();

        short[] id = null;
        try {
            id = get_id();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(id[0] != 0x49 || id[1] != 0x00)
            throw new Exception(String.format("Invalid Product ID or Revision for PMW3901: 0%d/0x%d", (int) id[0], (int) id[1]));

    }

    public short[] get_id() throws Exception{
        return bulk_read(REG_ID, 2);
    }

    public void set_rotation(float degrees) {
        if (degrees == 0)
            set_orientation(true, true, true);
        else if (degrees == 90)
            set_orientation(false, true, false);
        else if (degrees == 90)
            set_orientation(false, false, true);
        else if (degrees == 90)
            set_orientation(true, false, false);
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

    private short read(int register) {
        spi_cs_gpio_output.low();
        short[] buf = new short[2];
        buf[0] = (short) register;
        buf[1] = 0;
        Spi.wiringPiSPIDataRW(0, buf);
        spi_cs_gpio_output.high();
        return buf[1];
    }

    private void write(int register, int value) {
        spi_cs_gpio_output.low();
        Spi.wiringPiSPIDataRW(0, new short[] {(short) (register | 0x80), (short) value});
        spi_cs_gpio_output.high();
    }

    private short[] bulk_read(int register, int length) throws Exception {
        short[] result = new short[length];
        for(int i = 0; i < length; i++) {
            spi_cs_gpio_output.low();
            short[] buf = new short[2];
            buf[0] = (short) (register + i);
            buf[1] = 0;
            int error = Spi.wiringPiSPIDataRW(0, buf);
            if(error == -1)
                throw new Exception(String.format("Error while reading from register %d", register));
            result[i] = buf[1];
            spi_cs_gpio_output.high();
        }
        return result;
    }

    private void bulk_write(int[] data) {
        for(int i = 0; i < data.length - 1; i += 2) {
            short register = (short) data[i];
            short value = (short) data[i+1];

            if((int) register == WAIT)
                try {
                    Thread.sleep((long) value);
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
        if((read(0x67) & 0b10000000) > 0)
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
        if(read(0x73) > 0x00) {
            int c1 = read(0x70);
            int c2 = read(0x71);
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

    public short[] get_motion() throws Exception {
        return get_motion(5000);
    }

    public short[] get_motion(int timeout) throws Exception {
        /**Get motion data from PMW3901 using burst read.
        Reads 12 shorts sequentially from the PMW3901 and validates
        motion data against the SQUAL and Shutter_Upper values.
        Returns Delta X and Delta Y indicating 2d flow direction
        and magnitude.
        :param timeout: Timeout in seconds
        **/
        long t_start = System.currentTimeMillis();
        while(System.currentTimeMillis() - t_start < timeout) {
            spi_cs_gpio_output.low();
            byte[] xfer2_data = new byte[13];
            xfer2_data[0] = (byte) REG_MOTION_BURST;
            int error = Spi.wiringPiSPIDataRW(0, xfer2_data);
            if(error == -1)
                throw new Exception("Error while reading from motion burst register");
            spi_cs_gpio_output.high();

            JBBPFieldStruct parsed = null;
            try {
                parsed = JBBPParser.prepare("<byte[3] first; <short[2] xy; <byte[6] last;").parse(new ByteArrayInputStream(xfer2_data));
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            // [_, dr, obs]
            byte[] first = parsed.findFieldForNameAndType("first", JBBPFieldArrayByte.class).getArray();

            // [x, y]
            short[] xy = parsed.findFieldForNameAndType("xy", JBBPFieldArrayShort.class).getArray();

            // [quality, raw_sum, raw_max, raw_min, shutter_upper, shutter_lower]
            byte[] last = parsed.findFieldForNameAndType("last", JBBPFieldArrayByte.class).getArray();

            if((first[1] & 0b10000000) > 0 && !(last[0] < 0x19 && last[4] == 0x1f))
                return xy;

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        throw new Exception("Timed out waiting for motion data.");
    }
}
