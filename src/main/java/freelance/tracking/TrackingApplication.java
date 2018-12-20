package freelance.tracking;

import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.Utility;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.task.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.*;

@SpringBootApplication
public class TrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackingApplication.class, args);
    }
}
