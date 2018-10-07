package freelance.tracking.dao.entity;

public class AdInfo {

    private String id;
    private Integer position;

    private String title;
    private String url;
    private String price;

    private String stats;

    private boolean premium;
    private boolean vip;
    private boolean urgent;
    private boolean upped;

    private boolean selected;

    private ChangeStatus upPosition;
//    private boolean upPrice;
    private ChangeStatus upStats;

    public ChangeStatus getUpPosition() {
        return upPosition;
    }

    public ChangeStatus getUpStats() {
        return upStats;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStats() {
        return stats;
    }

    public void setStats(String stats) {
        this.stats = stats;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public boolean isUpped() {
        return upped;
    }

    public void setUpped(boolean upped) {
        this.upped = upped;
    }

    public void changeSelectedStatus(AdInfo lastAD) {

        upPosition = getStatus(lastAD.getPosition(), this.position);
        upStats = getStatus(lastAD.getStats().split(" ")[0], this.stats.split(" ")[0]);

//        upPrice = Integer.parseInt(lastAD.getPrice().replaceAll("\\s", "")) <
//                Integer.parseInt(this.price.replaceAll("\\s", ""));
    }

    private ChangeStatus getStatus(String s1, String s2) {
        return getStatus(Integer.parseInt(s1), Integer.parseInt(s2));
    }

    private ChangeStatus getStatus(Integer s1, Integer s2) {
        if (s1.equals(s2))
            return ChangeStatus.EQUEALS;

        if (s1 > s2)
            return ChangeStatus.UP;
        else
            return ChangeStatus.DOWN;
    }

    public enum ChangeStatus {
        UP,
        DOWN,
        EQUEALS
    }
}
