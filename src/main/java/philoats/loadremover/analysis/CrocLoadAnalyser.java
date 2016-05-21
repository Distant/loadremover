package philoats.loadremover.analysis;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CrocLoadAnalyser implements LoadAnalyser {

    private int totalFrames;
    private int goodFrames;
    private int rX, rY, rW, rH;
    private int rXEnd, rYEnd;
    private int hLineSpacing;
    private int vLineSpacing;

    private FrameBuffer<Frame> buffer;
    private List<Boolean> validFrames;
    private int index = 0;
    private BufferedImage oldFrame;

    private FrameConverter<BufferedImage> converter;

    private static final int FRAME_STEP = 30;    // Needs to be an even number
    private static final int STEP_MIN = 4;
    private static final int QUICK_CHECK_POINTS = 30;
    private static final int ACCURATE_CHECK_POINTS = 10000;
    private static final int BRIGHTNESS_THRESHOLD = 15;
    private static final int LINE_COUNT = 2;
    private static final double LINE_SPACING = 0.02;
    private static final double MENU_SIZE = 0.08;
    private static final int MARGIN = 1;

    private boolean loading;

    CrocLoadAnalyser(int x, int y, int width, int height, FrameConverter<BufferedImage> converter) {
        setRegion(x, y, width, height);
        this.converter = converter;
        buffer = new FrameBuffer<>(FRAME_STEP);

        validFrames = new ArrayList<>();
        goodFrames = 0;

        loading = false;
    }

    @Override
    public List<Boolean> getValidFrames() {
        return validFrames;
    }

    @Override
    public int getValidFrameCount() {
        return goodFrames;
    }

    @Override
    public void setRegion(int x, int y, int width, int height) {
        rX = x;
        rY = y;
        rW = (width - 1);
        rH = (height - (int) (height * MENU_SIZE) - 1);
        rXEnd = rX + rW;
        rYEnd = rY + rH;
        hLineSpacing = (int) (rH * (LINE_SPACING / LINE_COUNT));
        vLineSpacing = (int) (rW * (LINE_SPACING) / LINE_COUNT);
    }

    @Override
    public void onNext(Frame frame) {
        index++;
        totalFrames++;
        if (!loading) {   // If a loading screen is active, do not add goodFrames
            goodFrames++;
            validFrames.add(true);
        } else validFrames.add(false);

        buffer.add(cloneFrame(frame));

        BufferedImage bFrame;
        if (index % FRAME_STEP == 0) {
            bFrame = converter.convert(frame);
            if (!loading) {
                if (isLoadingScreen(bFrame)) {
                    loading = true;
                    int firstFrame = recursiveFindFrame(true, true, FRAME_STEP, FRAME_STEP);
                    goodFrames -= (FRAME_STEP - (firstFrame - 1));
                    for (int i = FRAME_STEP; i >= firstFrame; --i) {
                        validFrames.set((i) + (buffer.getOffset() - 1), false);
                    }
                }
            } else if (!isLoadingScreen(bFrame)) {
                loading = false;
                int lastFrame = recursiveFindFrame(false, false, 1, FRAME_STEP);
                System.out.println("Last: " + (lastFrame + buffer.getOffset() - 1));
                for (int i = FRAME_STEP; i >= lastFrame; --i) {
                    validFrames.set((i) + (buffer.getOffset() - 1), true);
                }
                goodFrames += (FRAME_STEP - (lastFrame - 1));
            }
        }
    }

    private int recursiveFindFrame(boolean loadingFrame, boolean isLower, int current, int step) {
        //System.out.println(loadingFrame + "," + isLower + "," + current + "," + step);
        BufferedImage bFrame;

        step /= 2;
        current = current + (isLower ? -step : step);

        if (step <= STEP_MIN) {
            bFrame = converter.convert(buffer.getRelative(current));
            boolean checkBelow = (isLoadingScreen(bFrame) == loadingFrame);
            while (true) {
                if (checkBelow) current--;
                else current++;
                int frameStep = 0;
                if (!checkBelow && current == frameStep)
                    return frameStep;
                if (checkBelow && current == 1)
                    return 1;
                bFrame = converter.convert(buffer.getRelative(current));
                if (loadingFrame) {
                    if (checkBelow) {
                        if (!isLoadingScreen(bFrame))
                            return current + 1;
                    } else if (isLoadingScreen(bFrame))
                        return current;
                } else {
                    if (checkBelow) {
                        if (isLoadingScreen(bFrame))
                            return current + 1;
                    } else if (!isLoadingScreen(bFrame))
                        return current;
                }
            }
        }

        bFrame = converter.convert(buffer.getRelative(current));

        // If the current frame is a desired frame, we need to search lower, else search higher for another desired frame
        if (isLoadingScreen(bFrame) == loadingFrame)
            return recursiveFindFrame(loadingFrame, true, current, step);
        return recursiveFindFrame(loadingFrame, false, current, step);
    }

    private boolean isLoadingScreen(BufferedImage bFrame) {
        if (quickCheck(bFrame)) {
            return checkFrame(ACCURATE_CHECK_POINTS, bFrame);
        }
        return false;
    }

    private boolean quickCheck(BufferedImage bFrame) {
        return checkFrame(QUICK_CHECK_POINTS, bFrame);
    }

    private boolean checkFrame(int points, BufferedImage img) {
        int hPoints = (int) ((((double) rW/rH)/4.0) * points);
        System.out.println("hPoints: " + hPoints + ", rW: " + rW + ", rH: " + rH + ", points: " + points);
        int vPoints = (points - 2*hPoints)/2;
        System.out.println("vPoints: " + vPoints);

        int hPointsPerLine = hPoints/LINE_COUNT;
        int vPointsPerLine = vPoints/LINE_COUNT;
        System.out.println("vPPerLine: " + vPointsPerLine);

        double hSpacing = rW/(hPointsPerLine-1);
        double vSpacing = rH/(vPointsPerLine-1);

        // Check top
        for (int i = 0; i < LINE_COUNT; ++i) {
            for (int j = 0; j < hPointsPerLine; ++j) {
                if (isBright(img.getRGB((int) (j * hSpacing) + rX,
                        i * hLineSpacing + rY
                )))
                    return false;
            }
        }

        // Check bottom
        for (int i = 0; i < LINE_COUNT; ++i) {
            for (int j = 0; j < hPointsPerLine; ++j) {
                if (isBright(img.getRGB((int) (j * hSpacing) + rX,
                        rYEnd - i * hLineSpacing
                )))
                    return false;
            }
        }

        // Check left
        for (int i = 0; i < LINE_COUNT; ++i) {
            for (int j = 0; j < vPointsPerLine; ++j) {
                if (isBright(img.getRGB(i * vLineSpacing + rX,
                        (int) (j * vSpacing) + rY
                )))
                    return false;
            }
        }

        // Check right
        for (int i = 0; i < LINE_COUNT; ++i) {
            for (int j = 0; j < vPointsPerLine; ++j) {
                if (isBright(img.getRGB(rXEnd - i * vLineSpacing,
                        (int) (j * vSpacing + rY)
                )))
                    return false;
            }
        }

        return true;
    }

    private boolean isBright(int col) {
        Color c = Color.decode(Integer.toString(col));
        return ((c.getRed() + c.getBlue() + c.getGreen()) / 3) > BRIGHTNESS_THRESHOLD;
    }

    private static Frame cloneFrame(Frame sourceFrame) {
        Frame newFrame = new Frame();
        int buffersLength = sourceFrame.image.length;
        newFrame.image = new Buffer[buffersLength];
        for (int i = 0; i < buffersLength; i++) {
            newFrame.image[i] = ByteBuffer.allocateDirect(sourceFrame.image[i].capacity());
            sourceFrame.image[i].rewind();
            ((ByteBuffer) (newFrame.image[i])).put((ByteBuffer) sourceFrame.image[i]);
            sourceFrame.image[i].rewind();
            newFrame.image[i].flip();
        }
        newFrame.imageDepth = sourceFrame.imageDepth;
        newFrame.imageHeight = sourceFrame.imageHeight;
        newFrame.imageChannels = sourceFrame.imageChannels;
        newFrame.imageStride = sourceFrame.imageStride;
        newFrame.imageWidth = sourceFrame.imageWidth;
        newFrame.keyFrame = sourceFrame.keyFrame;
        newFrame.opaque = sourceFrame.opaque;
        return newFrame;
    }
}