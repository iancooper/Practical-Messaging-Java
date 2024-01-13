package simplemessaging;

public class Step {

    private int order;
    private String routingKey;
    private boolean completed;

    public Step(){}

    public Step(int order, String routingKey) {
        this.order = order;
        this.routingKey = routingKey;
    }

    public int getOrder() {
        return order;
    }
    public void setOrder(int order) {
        this.order = order;
    }
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
    public String getRoutingKey() {
        return routingKey;
    }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

}
