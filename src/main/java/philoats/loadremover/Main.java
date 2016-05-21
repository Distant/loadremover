package philoats.loadremover;

import org.bytedeco.javacv.*;
import philoats.loadremover.analysis.AnalyserPanel;
import philoats.loadremover.analysis.RunInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Main {

    private static JFrame jFrame;
    private static FFmpegFrameGrabber grabber;
    private static boolean selecting;
    private static boolean mouseDown;
    private static Point startPos;
    private static Point endPos;
    private static JLabel preview;
    private static JPanel selectionPane;
    private static Rectangle videoBounds = new Rectangle(0, 0, 0, 0);
    private static JSlider frameSlider;
    private static FrameConverter<BufferedImage> converter;

    private static int runStartFrame = -1;
    private static int runEndFrame = -1;
    private static int videoStartFrame = -1;
    private static int videoEndFrame = -1;

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ex) {

        }

        jFrame = new JFrame("Load Remover");
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setResizable(false);
        jFrame.setLocationByPlatform(true);
        jFrame.getContentPane().setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));

        converter = new Java2DFrameConverter();

        preview = new JLabel();
        jFrame.add(preview);

        selectionPane = new JPanel();
        selectionPane.setBackground(new Color(0, 0, 0, 0));
        selectionPane.setBorder(BorderFactory.createDashedBorder(null, 2f, 4f, 4f, false));
        jFrame.getLayeredPane().add(selectionPane);

        chooseFile();

        frameSlider = new JSlider(JSlider.HORIZONTAL, 0, grabber.getLengthInFrames(), 0);
        frameSlider.addChangeListener(e -> {
            int frame = frameSlider.getValue();
            showPreview(frame);
        });
        frameSlider.setPreferredSize(new Dimension(grabber.getImageWidth(), frameSlider.getPreferredSize().height));
        jFrame.getContentPane().add(frameSlider, FlowLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));

        Button startButton = new Button("Start Analysis");
        startButton.addActionListener(e -> {
            AnalyserPanel analyserPanel = new AnalyserPanel(grabber, new RunInfo(runStartFrame, runEndFrame, videoStartFrame, videoEndFrame, videoBounds.width, videoBounds.height, videoBounds.x, videoBounds.y));
            analyserPanel.setLayout(new BorderLayout());
            jFrame.setContentPane(analyserPanel);
            analyserPanel.performAnalysis();
        });

        startButton.setSize(100, 50);
        buttons.add(startButton);

        Button regionButton = new Button("Select Region");
        regionButton.addActionListener(e -> {

            if (selecting) {
                jFrame.setCursor(Cursor.DEFAULT_CURSOR);
                selecting = false;
            } else {
                jFrame.setCursor(Cursor.CROSSHAIR_CURSOR);
                selecting = true;
            }

        });
        regionButton.setSize(100, 50);
        buttons.add(regionButton);

        Button fileButton = new Button("Choose File");
        fileButton.addActionListener(e -> chooseFile());
        fileButton.setSize(100, 50);
        buttons.add(fileButton);

        Button previewDecrementButtonLarge = new Button("<<");
        previewDecrementButtonLarge.addActionListener(e -> frameSlider.setValue(frameSlider.getValue() - 5));
        previewDecrementButtonLarge.setSize(100, 50);
        buttons.add(previewDecrementButtonLarge);

        Button previewDecrementButton = new Button("<");
        previewDecrementButton.addActionListener(e -> frameSlider.setValue(frameSlider.getValue() - 1));
        previewDecrementButton.setSize(100, 50);
        buttons.add(previewDecrementButton);

        Button previewIncrementButton = new Button(">");
        previewIncrementButton.addActionListener(e -> frameSlider.setValue(frameSlider.getValue() + 1));
        previewIncrementButton.setSize(100, 50);
        buttons.add(previewIncrementButton);

        Button previewIncrementButtonLarge = new Button(">>");
        previewIncrementButtonLarge.addActionListener(e -> frameSlider.setValue(frameSlider.getValue() + 5));
        previewIncrementButtonLarge.setSize(100, 50);
        buttons.add(previewIncrementButtonLarge);

        String[] frames = new String[]{"Run Start", "Run End", "Video Start", "Video End"};
        JComboBox<String> frameOptions = new JComboBox<>(frames);
        frameOptions.addActionListener(e -> {
            int index = frameOptions.getSelectedIndex();
            switch (index) {
                case 0:
                    runStartFrame = frameSlider.getValue();
                    break;
                case 1:
                    runEndFrame = frameSlider.getValue();
                    break;
                case 2:
                    videoStartFrame = frameSlider.getValue();
                    break;
                case 3:
                    videoEndFrame = frameSlider.getValue();
                    break;
            }
        });
        buttons.add(frameOptions);

        jFrame.getContentPane().add(buttons, BorderLayout.PAGE_END);

        preview.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseDown = true;
                startPos = e.getPoint();
                if (selecting) {
                    selectionPane.setVisible(true);
                    selectionPane.setBounds(0, 0, 0, 0);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDown = false;
                endPos = e.getPoint();
                if (selecting) {
                    selectionPane.setVisible(false);
                    selecting = false;
                    jFrame.setCursor(Cursor.DEFAULT_CURSOR);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        preview.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selecting) {
                    endPos = e.getPoint();
                    Rectangle rect = new Rectangle(startPos);
                    rect.add(endPos);
                    selectionPane.setBounds(rect.x, rect.y, rect.width, rect.height);
                    selectionPane.invalidate();
                    videoBounds = rect;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        jFrame.pack();
        jFrame.setVisible(true);
    }

    private static void chooseFile() {
        String file = null;
        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(jFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile().getAbsolutePath();
            JMenu menu = new JMenu();
            menu.add(new JMenu());
        }

        grabber = new FFmpegFrameGrabber(file);
        try {
            grabber.start();

            runStartFrame = 0;
            runEndFrame = grabber.getLengthInFrames();
            videoStartFrame = 0;
            videoEndFrame = grabber.getLengthInFrames();

            Rectangle rect = new Rectangle(new Point(0, 0));
            rect.add(new Point(grabber.getImageWidth(), grabber.getImageHeight()));
            videoBounds = rect;

            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.getBufferedImage(grabber.grabImage());
            preview.setIcon(new ImageIcon(image));
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }

        jFrame.pack();
    }

    private static void showPreview(int frame) {
        try {
            grabber.setFrameNumber(frame);
            BufferedImage image = converter.convert(grabber.grabImage());
            if (image != null) {
                preview.setIcon(new ImageIcon(image));
                preview.repaint();
            }
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }
}