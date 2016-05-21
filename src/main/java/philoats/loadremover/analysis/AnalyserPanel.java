package philoats.loadremover.analysis;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;


public class AnalyserPanel extends JPanel {

    private static final boolean debugVideo = true;
    private static final int fps = 30;
    private FFmpegFrameGrabber grabber;
    private RunInfo info;

    public AnalyserPanel(FFmpegFrameGrabber grabber, RunInfo info) {
        super();
        this.grabber = grabber;
        setData(info);
    }

    private void setData(RunInfo info) {
        this.info = info;
    }

    public void performAnalysis() {
        try {
            grabber.restart();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try {
            int w = grabber.getImageWidth();
            int h = grabber.getImageHeight();
            int totalFrames = grabber.getLengthInFrames();


            LoadAnalyser analyser = new CrocLoadAnalyser(info.x, info.y, w, h, converter);
            analyser.setRegion(info.x, info.y, info.width, info.height);

            boolean hasNext = true;
            while (hasNext) {
                org.bytedeco.javacv.Frame nextFrame = grabber.grabImage();
                if (nextFrame != null) {
                    analyser.onNext(nextFrame);
                } else
                    hasNext = false;
            }

            System.out.println("Total frames: " + totalFrames);
            System.out.println("Good frames: " + analyser.getValidFrameCount());

            int loadsSeconds = totalFrames / fps;
            int noLoadsSeconds = analyser.getValidFrameCount() / fps;

            String loads = String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(loadsSeconds),
                    TimeUnit.SECONDS.toMinutes(loadsSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(loadsSeconds)),
                    TimeUnit.SECONDS.toSeconds(loadsSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(loadsSeconds)));

            String noLoads = String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(noLoadsSeconds),
                    TimeUnit.SECONDS.toMinutes(noLoadsSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(noLoadsSeconds)),
                    TimeUnit.SECONDS.toSeconds(noLoadsSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(noLoadsSeconds)));

            System.out.println("Time with loads: " + loads);
            System.out.println("Time without loads: " + noLoads);

            if (debugVideo) {
                grabber.restart();
                JLabel label = new JLabel();
                add(label);

                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("testfile.mp4", grabber.getImageWidth(), grabber.getImageHeight());
                recorder.setFormat("mp4");
                recorder.setVideoBitrate(grabber.getVideoBitrate());
                recorder.start();

                Timer timer = new Timer(1, new ActionListener() {
                    private int i;
                    BufferedImage image;
                    Graphics2D bGr;
                    Frame frame;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (i < grabber.getLengthInFrames()) {
                            try {
                                frame = grabber.grabImage();
                            } catch (FrameGrabber.Exception e1) {
                                e1.printStackTrace();
                            }

                            if (i >= info.runStartFrame && i < info.runEndFrame && i < analyser.getValidFrames().size()) {
                                image = converter.getBufferedImage(frame);
                                if (!analyser.getValidFrames().get(i - info.runStartFrame)) {
                                    bGr = image.createGraphics();
                                    bGr.setColor(Color.red);
                                    bGr.fillOval(20, 20, 40, 40);
                                }
                                label.setIcon(new ImageIcon(image));
                            }

                            if (i >= info.videoStartFrame && i < info.videoEndFrame) {
                                if (analyser.getValidFrames().get(i)) {
                                    try {
                                        recorder.record(frame);
                                    } catch (FrameRecorder.Exception e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }

                            repaint();
                            i++;
                        } else {
                            ((Timer) e.getSource()).stop();
                            try {
                                grabber.stop();
                                recorder.stop();
                                recorder.release();
                            } catch (FrameGrabber.Exception | FrameRecorder.Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
                timer.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
