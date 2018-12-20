package freelance.tracking;

import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.Utility;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.task.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimerUpdateTask extends TimerTask {

    private final AdDAO adDAO;

    @Autowired
    public TimerUpdateTask(AdDAO adDAO) {
        this.adDAO = adDAO;
    }

    @Override
    public void run() {
        try {
            System.out.println("===================== UPDATE TASKS =====================");
            List<Record> records = adDAO.getNotReadyTasks();
            for (Record record : records) {
                List<Schedule> schedules = adDAO.updateTask(record.getId());
                List<Schedule> complete = schedules.stream().filter(s -> s.getStatus() == Status.COMPLETE).collect(Collectors.toList());
                adDAO.prepareData(new ArrayList<>(complete), record.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
