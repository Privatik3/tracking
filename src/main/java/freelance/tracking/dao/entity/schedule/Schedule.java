package freelance.tracking.dao.entity.schedule;

import lombok.Builder;
import lombok.Getter;

public @Builder class Schedule {
    private @Getter int id;
    private @Getter int time;
    private @Getter String report;
    private @Getter Status status;

    public void updateInfo(Schedule schedule) {
        this.id = schedule.id;
        this.time = schedule.time;
        this.report = schedule.report;
        this.status = schedule.status;
    }
}
