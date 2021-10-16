public class HeadlessTest {
    public static void main(String[] args) throws Exception 
    {
        PMW3901 sensor = new PMW3901(0, 1, PMW3901.BG_CS_BACK_BCM);
        // PMW3901i2c sensor = new PMW3901i2c(0, 1, PMW3901i2c.BG_CS_BACK_BCM);

        int tx = 0;
        int ty = 0;

        while(true) {
            short[] xy = sensor.get_motion();
            tx += xy[0] / 5;
            ty += xy[1] / 5;
            System.out.printf("Motion: %03d %03d x: %03d y %03d\n", xy[0], xy[1], tx, ty);
        }
    }
}
