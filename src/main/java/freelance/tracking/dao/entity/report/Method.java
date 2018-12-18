package freelance.tracking.dao.entity.report;

public class Method {

    private int hour;

    private int upCount;
    private int selectedCount;
    private int vipCount;
    private int premiumCount;
    private int xlCount;

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getUpCount() {
        return upCount;
    }

    public void setUpCount(int upCount) {
        this.upCount = upCount;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }

    public int getVipCount() {
        return vipCount;
    }

    public void setVipCount(int vipCount) {
        this.vipCount = vipCount;
    }

    public int getPremiumCount() {
        return premiumCount;
    }

    public void setPremiumCount(int premiumCount) {
        this.premiumCount = premiumCount;
    }

    public int getXlCount() {
        return xlCount;
    }

    public void setXlCount(int xlCount) {
        this.xlCount = xlCount;
    }
}
