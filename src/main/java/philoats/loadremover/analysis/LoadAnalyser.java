package philoats.loadremover.analysis;

import org.bytedeco.javacv.Frame;

import java.util.List;

public interface LoadAnalyser {
    void onNext(Frame frame);
    void setRegion(int x, int y, int width, int height);
    int getValidFrameCount();
    List<Boolean> getValidFrames();
}
