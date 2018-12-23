package freelance.tracking.dao.entity;

import lombok.Data;

import java.util.Date;
import java.util.Objects;

public @Data class AdInfo {

    private int scheduleID;

    private String id;
    private Date upTime;
    private Param position;

    private Param title;
    private Param url;
    private Param price;

    private Param stats;

    private Param premium;
    private Param vip;
    private Param urgent;
    private Param upped;
    private Param xl;

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
        this.xl.update(prevAd.xl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdInfo adInfo = (AdInfo) o;
        return Objects.equals(id, adInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
