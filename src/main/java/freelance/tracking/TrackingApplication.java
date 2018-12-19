package freelance.tracking;

import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.Utility;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.task.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SpringBootApplication
public class TrackingApplication {

    private static AdDAO adDAO;

    @Autowired
    public void setAdDAO(AdDAO adDAO) {
        TrackingApplication.adDAO = adDAO;
    }

    public static void main(String[] args) {

        /*Thread thread = new Thread(() -> {
            while (adDAO == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }

            for (int time = 0; time < 24; time++) {
                try {
                    List<Record> records = adDAO.getNotReadyTasks();
                    List<Schedule> newSchedule = new ArrayList<>();
                    for (Record record : records) {
                        try {
                            HashMap<String, String> params = record.getParams();
                            params.replace("token", String.format("trk_%s_%s", record.getId(), time));

                            Utility.sendDelayTask(params);
                            newSchedule.add(Schedule.builder()
                                    .taskId(record.getId())
                                    .time(time)
                                    .status(Status.QUEUE).build());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    adDAO.createSchedules(newSchedule);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        Thread.sleep(20 * 60 * 1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();*/

        SpringApplication.run(TrackingApplication.class, args);
    }
}
