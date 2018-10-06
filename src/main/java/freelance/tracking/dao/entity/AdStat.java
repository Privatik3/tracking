package freelance.tracking.dao.entity;

public class AdStat {

    private int position;
    private int price;
    private int totalView;
    private int delayView;
    private String timePos;

    private boolean premium;
    private boolean vip;
    private boolean urgent;
    private boolean upped;

    public String getTimePos() {
        return timePos;
    }

    public void setTimePos(String timePos) {
        this.timePos = timePos + ":00";
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = Integer.parseInt(price.replaceAll("\\s", ""));
    }

    public int getTotalView() {
        return totalView;
    }

    public void setTotalView(int totalView) {
        this.totalView = totalView;
    }

    public int getDelayView() {
        return delayView;
    }

    public void setDelayView(int delayView) {
        this.delayView = delayView;
    }

    public void setPromotion(String prom) {
        this.premium = prom.contains("1");
        this.vip = prom.contains("2");
        this.urgent = prom.contains("3");
        this.upped = prom.contains("4");
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean isVip() {
        return vip;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public boolean isUpped() {
        return upped;
    }
}
