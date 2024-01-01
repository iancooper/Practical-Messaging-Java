import java.util.HashMap;

public interface IAmARoutingSlip extends IAmAMessage {
    public int getCurrentStep();
    public void setCurrentStep(int newStep);

    public void addStep(int order, Step newStep);
    public HashMap<Integer, Step> getSteps();
}
