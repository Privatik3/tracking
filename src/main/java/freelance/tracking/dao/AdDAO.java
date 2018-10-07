package freelance.tracking.dao;

import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Transactional
@Repository
public class AdDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AdInfo> getAdInfo(String time, String sort) {

        String SQL = "SELECT * FROM data WHERE schedule_id = (SELECT id FROM schedule WHERE time = ?) ORDER BY " + sort;

        return jdbcTemplate.query(SQL, (rs, i) -> {
            AdInfo ad = new AdInfo();

            ad.setPosition(rs.getInt("position"));
            ad.setId(rs.getString("ad_id"));
            ad.setTitle(rs.getString("title"));
            ad.setPrice(rs.getString("price"));

            int totalView = rs.getInt("total_view");
            int delayView = rs.getInt("delay_view");

            ad.setStats(String.format("%d(+%d)", totalView, delayView));

            String prom = rs.getString("promotion");
            ad.setPremium(prom.contains("1"));
            ad.setVip(prom.contains("2"));
            ad.setUrgent(prom.contains("3"));
            ad.setUpped(prom.contains("4"));

            return ad;
        }, time);
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