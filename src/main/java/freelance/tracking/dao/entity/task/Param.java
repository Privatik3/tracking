package freelance.tracking.dao.entity.task;

import lombok.Data;

public @Data class Param {

    String name;
    String value;

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
