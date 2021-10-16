import javax.swing.*;
import java.awt.*;

public class Test extends JPanel {

    private int tx = 400;
    private int ty = 300;
    PMW3901 sensor;

    Test() throws Exception {
        sensor = new PMW3901(0, 1, PMW3901.BG_CS_BACK_BCM);
        System.out.println("Initialized sensor.");
    }

    private void updatePosition() {
        try {
            if(sensor != null) {
                short[] xy = sensor.get_motion();
                tx += xy[0] / 5;
                ty += xy[1] / 5;
                System.out.printf("Motion: %03d %03d x: %03d y %03d\n", xy[0], xy[1], tx, ty);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.fillOval(tx, ty, 10, 10);
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Optical Flow Sensor");
        Test test = new Test();
        frame.add(test);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while(true) {
            test.updatePosition();
            test.repaint();
        }
    }
}