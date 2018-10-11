package freelance.tracking.dao.entity;

public class Param {

    public Param(String curVal) {
        this.curVal = curVal;
    }

    private boolean change;
    private String curVal;
    private String prevVal;

    private Status status;

    protected void update(Param param) {
        prevVal = param.curVal;
        change = !curVal.equals(prevVal);

        if (change)
            updateStatus();
    }

    private void updateStatus() {
        if (prepareVal(curVal) > prepareVal(prevVal))
            status = Status.UP;
        else
            status = Status.DOWN;
    }

    private int prepareVal(String val) {
        return Integer.parseInt(val.split("\\(")[0].replaceAll("\\s", ""));
    }

    public boolean isChange() {
        return change;
    }

    public String getCurVal() {
        return curVal;
    }

    public String getPrevVal() {
        return prevVal;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        UP,
        DOWN
    }
}
