package freelance.tracking.dao.entity;

public class AdInfo {

    private int scheduleID;

    private String id;
    private Param position;

    private Param title;
    private Param url;
    private Param price;

    private Param stats;

    private Param premium;
    private Param vip;
    private Param urgent;
    private Param upped;

    private boolean old;
    private boolean selected;

    public void updatePrev(AdInfo prevAd) {
        this.position.update(prevAd.position);
        this.price.update(prevAd.price);
        this.stats.update(prevAd.stats);

        this.premium.update(prevAd.premium);
        this.vip.update(prevAd.vip);
        this.urgent.update(prevAd.urgent);
        this.upped.update(prevAd.upped);
    }

    public String getId() {
        return id;
    }

    public int getScheduleID() {
        return scheduleID;
    }

    public void setScheduleID(int scheduleID) {
        this.scheduleID = scheduleID;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPosition(Param position) {
        this.position = position;
    }

    public void setTitle(Param title) {
        this.title = title;
    }

    public void setUrl(Param url) {
        this.url = url;
    }

    public void setPrice(Param price) {
        this.price = price;
    }

    public void setStats(Param stats) {
        this.stats = stats;
    }

    public void setPremium(Param premium) {
        this.premium = premium;
    }

    public void setVip(Param vip) {
        this.vip = vip;
    }

    public void setUrgent(Param urgent) {
        this.urgent = urgent;
    }

    public void setUpped(Param upped) {
        this.upped = upped;
    }

    public boolean isOld() {
        return old;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setOld(boolean old) {
        this.old = old;
    }

    public Param getPosition() {
        return position;
    }

    public Param getTitle() {
        return title;
    }

    public Param getUrl() {
        return url;
    }

    public Param getPrice() {
        return price;
    }

    public Param getStats() {
        return stats;
    }

    public Param getPremium() {
        return premium;
    }

    public Param getVip() {
        return vip;
    }

    public Param getUrgent() {
        return urgent;
    }

    public Param getUpped() {
        return upped;
    }
}
