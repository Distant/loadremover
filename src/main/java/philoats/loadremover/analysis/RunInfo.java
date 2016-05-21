package philoats.loadremover.analysis;

public final class RunInfo {
    public final int runStartFrame;
    public final int runEndFrame;
    public final int videoStartFrame;
    public final int videoEndFrame;
    public final int width;
    public final int height;
    public final int x;
    public final int y;


    public RunInfo(int runStartFrame, int runEndFrame, int videoStartFrame, int videoEndFrame, int width, int height, int x, int y) {
        this.runStartFrame = runStartFrame;
        this.runEndFrame = runEndFrame;
        this.videoStartFrame = videoStartFrame;
        this.videoEndFrame = videoEndFrame;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }
}
