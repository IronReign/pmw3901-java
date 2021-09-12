package spitest;

/**
 * This example code demonstrates how to perform basic SPI communications using the Raspberry Pi.
 * CS0 and CS1 (ship-select) are supported for SPI0.
 *
 * @author Robert Savage
 */
public class Test {
    public static void main(String[] args) {
        PMW3901 sensor = null;
        try {
            sensor = new PMW3901(0, 1, PMW3901.BG_CS_FRONT_BCM);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int tx = 0;
        int ty = 0;
        try {
            while(true) {
                sensor.get_motion();
                // tx += xy[0];
                // ty += xy[1];
                // System.out.printf("Motion: %03d %03d x: %03d y %03d\n", xy[0], xy[1], tx, ty);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}