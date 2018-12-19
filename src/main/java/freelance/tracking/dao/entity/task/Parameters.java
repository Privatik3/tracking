package freelance.tracking.dao.entity.task;

import lombok.Data;

public @Data class Parameters {
    Param[] parameters;

    public Parameters(Param[] parameters) {
        this.parameters = parameters;
    }
}
