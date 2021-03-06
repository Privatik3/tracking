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

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Date;
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

    private RowMapper<AdInfo> getAdInfoMapper() {
        return new RowMapper<AdInfo>() {
            @Override
            public AdInfo mapRow(ResultSet rs, int i) throws SQLException {
                AdInfo ad = new AdInfo();
                try {
                    ad.setScheduleID(rs.getInt("schedule_id"));
                    ad.setUpTime(rs.getTimestamp("up_time"));
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
                    ad.setXl(new Param(prom.contains("5") ? "1" : "0"));

                    return ad;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return ad;
            }
        };
    }

    public List<AdInfo> getAdInfo(String taskID, String time, String sort) {

        String SQL = "SELECT * FROM data WHERE schedule_id IN ( SELECT id FROM schedule WHERE time IN ( ?, ? ) AND task_id = ? ) ORDER BY " + sort;

        List<AdInfo> ads = jdbcTemplate.query(SQL, getAdInfoMapper(), Integer.parseInt(time) - 1, time, taskID);
        Integer scheduleMax = Collections.max(ads.stream().map(AdInfo::getScheduleID).collect(Collectors.toList()));

        List<AdInfo> previousAds = new ArrayList<>();
        Iterator<AdInfo> adIter = ads.iterator();
        while (adIter.hasNext()) {
            AdInfo next = adIter.next();
            if (next.getScheduleID() != scheduleMax) {
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

    private List<AdInfo> getAdInfoByHoursList(String taskID, List<Schedule> dayHours) {

        String SQL = "SELECT * FROM data WHERE schedule_id IN ( :ids )";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", dayHours.stream().map(c -> c.getId()).collect(Collectors.toList()));

        NamedParameterJdbcTemplate template =
                new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
        return template.query(SQL, parameters, getAdInfoMapper());
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

    public List<Report> getGeneralReport(String taskID) throws Exception {

        List<Report> result = new ArrayList<>();

        Record history = getHistoryByID(taskID);
        Calendar startDay = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        startDay.setTime(history.getAddTime());
        startDay.add(Calendar.DATE, 1);

        for (int i = 0; i < 7; i++) {
            try {
                List<Schedule> schedules = updateTask(Integer.parseInt(taskID), i);
                List<Schedule> dayHours = schedules.stream().filter(s -> s.getStatus() == Status.COMPLETE).collect(Collectors.toList());
                prepareData(new ArrayList<>(dayHours), Integer.parseInt(taskID));

                System.out.println("DAY: " + i);
                System.out.println("====================================");

                Report report = new Report();

                Calendar newDay = (Calendar) startDay.clone();
                newDay.add(Calendar.DATE, i);
                report.setDate(new SimpleDateFormat("dd.MM.yyyy").format(newDay.getTime()));

                List<New> news = new ArrayList<>();
                List<Up> ups = new ArrayList<>();
                List<Method> methods = new ArrayList<>();

                List<AdInfo> lastHourNew = new ArrayList<>();

                List<AdInfo> allAds = getAdInfoByHoursList(taskID, dayHours);
                for (Schedule hour : dayHours) {
                    int id = hour.getId();
                    int time = hour.getTime();

                    // New
                    New itemNew = new New();

                    List<AdInfo> hourAds = new ArrayList<>();
                    int hourViewCounter = 0;
                    for (AdInfo ad : allAds) {
                        if (ad.getScheduleID() != id) continue;
                        if (Utility.isNew(ad)) {
                            hourAds.add(ad);
                            Optional<AdInfo> prev = lastHourNew.stream().filter(lAd -> lAd.getId().equals(ad.getId())).findFirst();
                            if (prev.isPresent()) {
                                int prevStat = Utility.parseStats(prev.get().getStats().getCurVal())[1];
                                int curStat = Utility.parseStats(ad.getStats().getCurVal())[1];

                                hourViewCounter += curStat - prevStat;
                            }
                        }
                    }

                    itemNew.setHour(time);
                    itemNew.setNewCount(hourAds.size());
                    int totalView = hourAds.stream().mapToInt(ad -> Utility.parseStats(ad.getStats().toString())[1]).sum();
                    itemNew.setTotalView(totalView - hourViewCounter);
                    itemNew.setHourView(hourViewCounter);

                    news.add(itemNew);
                    lastHourNew = new ArrayList<>(hourAds);

                    //UP
                    Up upItem = new Up();
                    Calendar upDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));

                    HashSet<AdInfo> upAds = new HashSet<>();
                    for (AdInfo ad : allAds) {
                        if (ad.getScheduleID() == id) {
                            if (!Utility.isNew(ad)) {
                                upDate.setTime(ad.getUpTime());
                                long timeDiff = upDate.getTimeInMillis() - startDay.getTimeInMillis();
                                int upTime = (int) timeDiff / (1000 * 60 * 60);

                                if (upTime == (time - 1)) {
                                    upAds.add(ad);
                                }
                            }
                        }
                    }

                    upItem.setHour(time);
                    upItem.setUpCount(upAds.size());
                    int totalUpView = upAds.stream().mapToInt(ad -> Utility.parseStats(ad.getStats().toString())[1]).sum();
                    upItem.setTotalView(totalUpView);
//                    upItem.setHourView(0);

                    ups.add(upItem);

                    // Method
                    Method method = new Method();

                    int upCount = 0, selectedCount = 0, vipCount = 0,
                        premiumCount = 0, xlCount = 0;
                    for (AdInfo ad : allAds) {
                        if (ad.getScheduleID() != id) continue;

                        upCount += ad.getUpped().getCurVal().equals("1") ? 1 : 0;
                        selectedCount += ad.getUrgent().getCurVal().equals("1") ? 1 : 0;
                        vipCount += ad.getVip().getCurVal().equals("1") ? 1 : 0;
                        premiumCount += ad.getPremium().getCurVal().equals("1") ? 1 : 0;
                        xlCount += ad.getXl().getCurVal().equals("1") ? 1 : 0;
                    }

                    method.setHour(time);
                    method.setUpCount(upCount);
                    method.setSelectedCount(selectedCount);
                    method.setVipCount(vipCount);
                    method.setPremiumCount(premiumCount);
                    method.setXlCount(xlCount);

                    methods.add(method);
                }

                report.setNewData(news);
                report.setUpData(ups);
                report.setMethodData(methods);

                result.add(report);
            } catch (Exception e) {
//                e.printStackTrace();
                break;
            }
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
        String SQL = String.format("SELECT id, time, report, status FROM schedule where task_id = %d %s ORDER BY time",
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

        String INSER_SQL = "INSERT INTO data (schedule_id, up_time, ad_id, position, title, url, price, total_view, delay_view, promotion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.batchUpdate(INSER_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement pr, int i) throws SQLException {
                    AdInfo ad = adStats.get(i);

                    pr.setInt(1, ad.getScheduleID());
                    pr.setTimestamp(2, new Timestamp(ad.getUpTime().getTime()));
                    pr.setInt(3, Integer.parseInt(ad.getId()));
                    pr.setInt(4, Integer.parseInt(ad.getPosition().toString()));
                    pr.setString(5,
                            ad.getTitle().toString().length() < 100 ? ad.getTitle().toString() :
                                    ad.getTitle().toString().substring(0, 100 - 3) + "...");
                    pr.setString(6, ad.getUrl().toString());
                    pr.setString(7, ad.getPrice().toString());
                    pr.setInt(8, Utility.parseStats(ad.getStats().toString())[0]);
                    pr.setInt(9, Utility.parseStats(ad.getStats().toString())[1]);

                    pr.setString(10,
                            (ad.getPremium().toString().equals("1") ? "1" : "")
                                    + (ad.getVip().toString().equals("1") ? "2" : "")
                                    + (ad.getUrgent().toString().equals("1") ? "3" : "")
                                    + (ad.getUpped().toString().equals("1") ? "4" : "")
                                    + (ad.getXl().toString().equals("1") ? "5" : "")
                    );
                }

                @Override
                public int getBatchSize() {
                    return adStats.size();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println();
        }
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

    public Record getHistoryByID(String taskID) {
        String SQL = "SELECT * FROM task WHERE id = ?";
        return jdbcTemplate.query(SQL, getRecordMapper(), taskID).get(0);
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