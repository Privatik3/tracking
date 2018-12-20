package freelance.tracking;

import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.Utility;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.task.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TimerDownloadTask extends TimerTask {

    private final AdDAO adDAO;
    private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));

    @Autowired
    public TimerDownloadTask(AdDAO adDAO) {
        this.adDAO = adDAO;
    }

    @Override
    public void run() {
        try {
            System.out.println("===================== CREATE SCHEDULE =====================");

            List<Record> records = adDAO.getNotReadyTasks();
            List<Schedule> newSchedule = new ArrayList<>();
            for (Record record : records) {
                long startDay = getNextDay(record.getAddTime());
                long timeDiff = getCurDay() - startDay;
                if (timeDiff < 0) continue;

                try {
                    int time = (int) timeDiff / (1000 * 60 * 60);
                    HashMap<String, String> params = record.getParams();
                    params.replace("token", String.format("trk_%s_%s", record.getId(), time));

                    Utility.sendDelayTask(params);
                    newSchedule.add(Schedule.builder()
                            .taskId(record.getId())
                            .time(time)
                            .status(Status.QUEUE).build());

                    if (time >= record.getAllTime()) {
                        record.setTime(time);
                        record.setStatus(freelance.tracking.dao.entity.task.Status.COMPLETE);
                    } else {
                        record.setTime(time);
                        record.setStatus(freelance.tracking.dao.entity.task.Status.PROCESSING);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            adDAO.createSchedules(newSchedule);
            adDAO.updatetaskInfo(records);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getCurDay() {
        calendar.setTime(new Date());
        return calendar.getTimeInMillis();
    }

    private long getNextDay(Date target) {
        calendar.setTime(target);
        calendar.add(Calendar.DATE, 1);
        return calendar.getTimeInMillis();
    }
}
