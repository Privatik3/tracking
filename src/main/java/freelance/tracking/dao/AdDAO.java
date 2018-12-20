package freelance.tracking.dao;

import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import freelance.tracking.dao.entity.Param;
import freelance.tracking.dao.entity.Report;
import freelance.tracking.dao.entity.report.Method;
import freelance.tracking.dao.entity.report.New;
import freelance.tracking.dao.entity.report.Up;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.task.Record;
import freelance.tracking.dao.entity.task.TaskLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
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
            try {
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
                ad.setOld(totalView == delayView);

                String prom = rs.getString("promotion");
                ad.setPremium(new Param(prom.contains("1") ? "1" : "0"));
                ad.setVip(new Param(prom.contains("2") ? "1" : "0"));
                ad.setUrgent(new Param(prom.contains("3") ? "1" : "0"));
                ad.setUpped(new Param(prom.contains("4") ? "1" : "0"));

                return ad;
            } catch (Exception e) {
                e.printStackTrace();
            }

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

    public List<AdStat> getAdStat(String adID, String taskID, int day) {

        String SQL =
                "SELECT position, price, total_view, delay_view, promotion, time FROM data JOIN schedule ON data.schedule_id = schedule.id  " +
                        "WHERE ad_id = ? AND schedule_id IN ( SELECT id FROM schedule WHERE task_id = ? " + String.format("AND ( time >= %d AND time < %d ) )", day * 24, (day * 24) + 24);
        List<AdStat> adStats = jdbcTemplate.query(SQL, (rs, i) -> {
            AdStat stat = new AdStat();

            stat.setPosition(60 - rs.getInt("position"));
//            stat.setPrice(rs.getString("price"));
            stat.setTotalView(rs.getInt("total_view"));
            stat.setDelayView(rs.getInt("delay_view"));
            stat.setPromotion(rs.getString("promotion"));
            stat.setTimePos(rs.getString("time"));

            return stat;
        }, adID, taskID);

        // Вычесляем значения для графика просмотров
        for (int i = adStats.size() - 1; i > 0; i--) {
            AdStat stat = adStats.get(i);
            stat.setTotalView(stat.getTotalView() - adStats.get(i - 1).getTotalView());
        }
        adStats.get(0).setTotalView(0);

        return adStats;
    }

    public List<Report> getGeneralReport(String reportID) {

        List<Report> result = new ArrayList<>();

        int dayCount = ThreadLocalRandom.current().nextInt(1, 7 + 1);
        for (int i = 0; i < dayCount; i++) {

            Report report = new Report();
            report.setDate(String.valueOf(14 + i) + ".11.2018");

            int scip = ThreadLocalRandom.current().nextInt(1, 5 + 1);
            int[] scipNum = new int[scip];
            for (int j = 0; j < scipNum.length; j++) {
                scipNum[j] = ThreadLocalRandom.current().nextInt(1, 24 + 1);
            }

            // New
            List<New> news = new ArrayList<>();

            for (int j = 0; j < 24; j++) {
                int finalJ = j;
                if (Arrays.stream(scipNum).anyMatch(s -> s == finalJ))
                    continue;

                New newData = new New();
                newData.setHour(j);
                int newCount = ThreadLocalRandom.current().nextInt(1, 25 + 1);
                newData.setNewCount(newCount);
                int totalView = ThreadLocalRandom.current().nextInt(newCount * 2, (newCount * 5) + 1);
                newData.setTotalView(totalView);
                newData.setHourView(totalView / ThreadLocalRandom.current().nextInt(2, 4 + 1));

                news.add(newData);
            }
            report.setNewData(news);

            // UP
            List<Up> ups = new ArrayList<>();
            for (int j = 0; j < 24; j++) {
                int finalJ = j;
                if (Arrays.stream(scipNum).anyMatch(s -> s == finalJ))
                    continue;

                Up upData = new Up();
                upData.setHour(j);
                int upCount = ThreadLocalRandom.current().nextInt(1, 25 + 1);
                upData.setUpCount(upCount);
                int totalView = ThreadLocalRandom.current().nextInt(upCount * 2, (upCount * 5) + 1);
                upData.setTotalView(totalView);
                upData.setHourView(totalView / ThreadLocalRandom.current().nextInt(2, 4 + 1));

                ups.add(upData);
            }
            report.setUpData(ups);

            // Method
            List<Method> methods = new ArrayList<>();
            for (int j = 0; j < 24; j++) {
                int finalJ = j;
                if (Arrays.stream(scipNum).anyMatch(s -> s == finalJ))
                    continue;

                Method method = new Method();
                method.setHour(j);
                method.setPremiumCount(ThreadLocalRandom.current().nextInt(1, 8 + 1));
                method.setUpCount(ThreadLocalRandom.current().nextInt(1, 8 + 1));
                method.setSelectedCount(ThreadLocalRandom.current().nextInt(1, 8 + 1));
                method.setXlCount(ThreadLocalRandom.current().nextInt(1, 8 + 1));
                method.setVipCount(ThreadLocalRandom.current().nextInt(1, 8 + 1));

                methods.add(method);
            }
            report.setMethodData(methods);

            result.add(report);
        }

        return result;
    }

    public List<Schedule> updateTask(int taskID) throws Exception {
        return updateTask(taskID, -1);
    }

    public List<Schedule> updateTask(int taskID, int day) throws Exception {

        List<Schedule> schedule = getSchedule(taskID, day);
        if (schedule.size() == 0)
            throw new Exception("Не было получаено результата");

        List<Schedule> unDoneSchedule = schedule.stream().filter(
                s -> s.getStatus() == Status.QUEUE || s.getStatus() == Status.PROCESSING).collect(Collectors.toList());

        requestScheduleUpdate(unDoneSchedule);
        for (Schedule task : unDoneSchedule) {
            Optional<Schedule> target = schedule.stream().filter(s -> s.getId() == task.getId()).findFirst();
            target.ifPresent(t -> t.updateInfo(task));
        }

        Utility.checkDownloaded(taskID, schedule);
        return schedule;
    }

    public void createSchedules(List<Schedule> newSchedule) {

        String INSERT_SQL = "INSERT INTO schedule (task_id, time, status) VALUES ( ?, ?, ? )";
        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Schedule schedule = newSchedule.get(i);
                ps.setInt(1, schedule.getTaskId());
                ps.setInt(2, schedule.getTime());
                ps.setInt(3, schedule.getStatus().ordinal());
            }

            @Override
            public int getBatchSize() {
                return newSchedule.size();
            }
        });
    }

    private List<Schedule> getSchedule(int taskID, int day) {
        String SQL = String.format("SELECT id, time, report, status FROM schedule where task_id = %d %s",
                taskID, (day == -1 ? "" : String.format("AND ( time >= %d AND time < %d )", day * 24, (day * 24) + 24)));

        return jdbcTemplate.query(SQL, new RowMapper<Schedule>() {
            @Override
            public Schedule mapRow(ResultSet rs, int i) throws SQLException {
                return Schedule.builder()
                        .id(rs.getInt("id"))
                        .taskId(taskID)
                        .time(rs.getInt("time"))
                        .report(rs.getString("report"))
                        .status(Status.values()[rs.getInt("status")]).build();
            }
        });
    }

    private void requestScheduleUpdate(List<Schedule> unDoneSchedule) {
        if (unDoneSchedule.size() == 0)
            return;

        Utility.requestUpdate(unDoneSchedule);
        updateSheduleInfo(unDoneSchedule);
    }

    private void updateSheduleInfo(List<Schedule> unDoneSchedule) {

        String UPDATE_SQL = "UPDATE schedule SET time = ?, report = ?, status = ? WHERE id = ?";
        jdbcTemplate.batchUpdate(UPDATE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Schedule schedule = unDoneSchedule.get(i);
                ps.setInt(1, schedule.getTime());
                ps.setString(2, schedule.getReport());
                ps.setInt(3, schedule.getStatus().ordinal());

                ps.setInt(4, schedule.getId());
            }

            @Override
            public int getBatchSize() {
                return unDoneSchedule.size();
            }
        });
    }

    public void prepareData(List<Schedule> complete, int taskID) {
        if (complete.size() == 0)
            return;

        String SQL = "SELECT DISTINCT schedule_id FROM data WHERE schedule_id IN ( :ids )";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", complete.stream().map(c -> c.getId()).collect(Collectors.toList()));

        NamedParameterJdbcTemplate template =
                new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
        List<Integer> ids = template.queryForList(SQL, parameters, Integer.class);

        Iterator<Schedule> iterator = complete.iterator();
        while (iterator.hasNext()) {
            Schedule schedule = iterator.next();
            if (ids.contains(schedule.getId()))
                iterator.remove();
        }

        List<AdInfo> adStats = Utility.prepareData(complete, taskID);
        insertData(adStats, taskID);
    }

    public void insertData(List<AdInfo> adStats, int taskID) {

        String INSER_SQL = "INSERT INTO data (schedule_id, ad_id, position, title, url, price, total_view, delay_view, promotion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(INSER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pr, int i) throws SQLException {
                AdInfo ad = adStats.get(i);

                pr.setInt(1, ad.getScheduleID());
                pr.setInt(2, Integer.parseInt(ad.getId()));
                pr.setInt(3, Integer.parseInt(ad.getPosition().toString()));
                pr.setString(4,
                        ad.getTitle().toString().length() < 100 ? ad.getTitle().toString() :
                                ad.getTitle().toString().substring(0, 100 - 3) + "...");
                pr.setString(5, ad.getUrl().toString());
                pr.setString(6, ad.getPrice().toString());

                String[] stata = ad.getStats().toString().split(" \\(\\+");
                pr.setInt(7, Integer.parseInt(stata[0]));
                pr.setInt(8, Integer.parseInt(stata[1].replace(")", "")));

                pr.setString(9,
                        (ad.getPremium().toString().equals("1") ? "1" : "")
                                + (ad.getVip().toString().equals("1") ? "2" : "")
                                + (ad.getUrgent().toString().equals("1") ? "3" : "")
                                + (ad.getUpped().toString().equals("1") ? "4" : "")
                );
            }

            @Override
            public int getBatchSize() {
                return adStats.size();
            }
        });
    }

    public void createTaskRecord(Record record) throws TaskLimitException {

        checkUserLimit(record.getNick());
        final String INSERT_SQL = "INSERT INTO task (nick, title, all_time, status) VALUES ( ?, ?, ?, ? )";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                    ps.setString(1, record.getNick());
                    ps.setString(2, record.getTitle());
                    ps.setInt(3, record.getAllTime());
                    ps.setInt(4, record.getStatus().ordinal());
                    return ps;
                },
                keyHolder);

        insertParams(String.valueOf(keyHolder.getKey()), record.getParams());
    }

    private void checkUserLimit(String nick) throws TaskLimitException {
        String SQL = "SELECT COUNT( id ) FROM task WHERE nick = ?";
        Integer limit = jdbcTemplate.queryForObject(SQL, Integer.class, nick);

        if (limit > 0)
            throw new TaskLimitException("Превышен лимит запросов, не возможно создать больше одного запроса");
    }

    private void insertParams(String taskID, HashMap<String, String> params) {

        String SQL = "INSERT INTO task_param ( task_id, name, value ) VALUES ( ?, ?, ? )";
        Iterator<Map.Entry<String, String>> paramItr = params.entrySet().iterator();

        jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<String, String> param = paramItr.next();
                ps.setString(1, taskID);
                ps.setString(2, param.getKey());
                ps.setString(3, param.getValue());
            }

            @Override
            public int getBatchSize() {
                return params.size();
            }
        });
    }

    public List<Record> getNotReadyTasks() {
        String SQL = "SELECT * FROM task WHERE NOT status = 2";
        List<Record> notReady = jdbcTemplate.query(SQL, getRecordMapper());

        Iterator<Record> iterator = notReady.iterator();
        while (iterator.hasNext()) {
            Record record = iterator.next();
            HashMap<String, String> params = getTaskParams(record.getId());
            if (params.size() == 0)
                iterator.remove();

            record.setParams(params);
        }

        return notReady;
    }

    private HashMap<String, String> getTaskParams(int taskID) {
        String SQL = "SELECT name, value FROM task_param where task_id = ?";

        HashMap<String, String> params = new HashMap<>();
        try {
            jdbcTemplate.query(SQL, rs -> {
                params.put(
                        rs.getString("name"),
                        rs.getString("value"));
            }, taskID);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return params;
    }

    public List<Record> getHistory(String nick) {
        String SQL = "SELECT * FROM task WHERE nick = ?";
        return jdbcTemplate.query(SQL, getRecordMapper(), nick);
    }

    private RowMapper<Record> getRecordMapper() {
        return new RowMapper<Record>() {
            @Override
            public Record mapRow(ResultSet rs, int i) throws SQLException {
                Record record = new Record();

                record.setId(rs.getInt("id"));
                record.setNick(rs.getString("nick"));
                record.setTitle(rs.getString("title"));
                record.setAddTime(rs.getDate("add_time"));
                record.setTime(rs.getInt("time"));
                record.setAllTime(rs.getInt("all_time"));
                record.setStatus(freelance.tracking.dao.entity.task.Status.values()[rs.getInt("status")]);

                return record;
            }
        };
    }

    public void updatetaskInfo(List<Record> records) {

        String UPDATE_SQL = "UPDATE task SET time = ?, status = ? WHERE id = ?";
        jdbcTemplate.batchUpdate(UPDATE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Record record = records.get(i);

                ps.setInt(1, record.getTime());
                ps.setInt(2, record.getStatus().ordinal());
                ps.setInt(3, record.getId());
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });

    }

    public void clearData(Integer taskID) {
        String DELETE_SQL = "DELETE FROM data WHERE schedule_id IN ( SELECT id FROM schedule WHERE task_id = ? )";
        jdbcTemplate.update(DELETE_SQL, taskID);
    }

    public void clearSchedule(Integer taskID) {
        String DELETE_SQL = "DELETE FROM schedule WHERE task_id = ?";
        jdbcTemplate.update(DELETE_SQL, taskID);
    }

    public void clearTaskParams(Integer taskID) {
        String DELETE_SQL = "DELETE FROM task_param WHERE task_id = ?";
        jdbcTemplate.update(DELETE_SQL, taskID);
    }

    public void removeTask(Integer taskID) {
        String DELETE_SQL = "DELETE FROM task WHERE id = ?";
        jdbcTemplate.update(DELETE_SQL, taskID);
    }
}