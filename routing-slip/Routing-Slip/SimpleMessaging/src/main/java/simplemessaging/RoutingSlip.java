package simplemessaging;

import java.util.HashMap;

public class RoutingSlip implements IAmARoutingSlip{

    private int currentStep = 1;
    private HashMap<Integer, Step> steps = new HashMap<>();

    @Override
    public int getCurrentStep() {
        return currentStep;
    }

    @Override
    public void setCurrentStep(int newStep) {
        currentStep = newStep;
    }

    @Override
    public void addStep(int order, Step newStep) {
        steps.put(order, newStep);
    }

    @Override
    public HashMap<Integer,Step> getSteps() {
        return steps;
    }
}
