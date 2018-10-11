package freelance.tracking.dao;

import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import freelance.tracking.dao.entity.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Transactional
@Repository
public class AdDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AdInfo> getAdInfo(String time, String sort) {

        String SQL = "SELECT * FROM data WHERE schedule_id IN ( SELECT id FROM schedule WHERE time IN ( ?, ? ) ) ORDER BY " + sort;

        AtomicInteger scheduleMax = new AtomicInteger();
        List<AdInfo> ads = jdbcTemplate.query(SQL, (rs, i) -> {
            AdInfo ad = new AdInfo();

            int scheduleID = rs.getInt("schedule_id");
            ad.setScheduleID(scheduleID);
            if (scheduleMax.get() < scheduleID)
                scheduleMax.set(scheduleID);

            ad.setId(rs.getString("ad_id"));
            ad.setPosition(new Param(rs.getString("position")));
            ad.setTitle(new Param(rs.getString("title")));
            ad.setUrl(new Param(rs.getString("url")));
            ad.setPrice(new Param(rs.getString("price")));

            int totalView = rs.getInt("total_view");
            int delayView = rs.getInt("delay_view");

            ad.setStats(new Param(String.format("%d (+%d)", totalView, delayView)));

            String prom = rs.getString("promotion");
            ad.setPremium(new Param(prom.contains("1") ? "1" : "0"));
            ad.setVip(new Param(prom.contains("2") ? "1" : "0"));
            ad.setUrgent(new Param(prom.contains("3") ? "1" : "0"));
            ad.setUpped(new Param(prom.contains("4") ? "1" : "0"));

            return ad;
        }, Integer.parseInt(time) - 1, time);

        List<AdInfo> previousAds = new ArrayList<>();
        Iterator<AdInfo> adIter = ads.iterator();
        while (adIter.hasNext()) {
            AdInfo next = adIter.next();
            if (next.getScheduleID() != scheduleMax.get()) {
                previousAds.add(next);
                adIter.remove();
            }
        }

        ads.forEach(ad -> {
            String id = ad.getId();
            Optional<AdInfo> any = previousAds.stream().filter(pAd -> pAd.getId().equals(id)).findAny();
            any.ifPresent(ad::updatePrev);
        });

        return ads;
    }

    public List<AdStat> getAdStat(String adID, String taskID) {

        String SQL = "SELECT position, price, total_view, delay_view, promotion, time FROM data JOIN schedule ON data.schedule_id = schedule.id  WHERE ad_id = ? AND schedule_id IN ( SELECT id FROM schedule WHERE task_id = ? );";

        return jdbcTemplate.query(SQL, (rs, i) -> {
            AdStat stat = new AdStat();

            stat.setPosition(rs.getInt("position"));
            stat.setPrice(rs.getString("price"));
            stat.setTotalView(rs.getInt("total_view"));
            stat.setDelayView(rs.getInt("delay_view"));
            stat.setPromotion(rs.getString("promotion"));
            stat.setTimePos(rs.getString("time"));

            return stat;
        }, adID, taskID);
    }
}