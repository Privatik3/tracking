package freelance.tracking.dao.entity;

import lombok.Data;

public @Data class AdInfo {

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
}
