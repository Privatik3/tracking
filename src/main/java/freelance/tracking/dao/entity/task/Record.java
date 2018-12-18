package freelance.tracking.dao.entity.task;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;

public @Data class Record {

    private int id;
    private String nick;
    private String title;
    private Date addTime;
    private int time;
    private int allTime;
    private Status status;

    private HashMap<String, String> params;
}
