package freelance.tracking.dao.entity;

import freelance.tracking.dao.entity.report.Method;
import freelance.tracking.dao.entity.report.New;
import freelance.tracking.dao.entity.report.Up;


import java.util.List;

public class Report {

    private String date;

    private List<New> newData;
    private List<Up> upData;
    private List<Method> methodData;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<New> getNewData() {
        return newData;
    }

    public void setNewData(List<New> newData) {
        this.newData = newData;
    }

    public List<Up> getUpData() {
        return upData;
    }

    public void setUpData(List<Up> upData) {
        this.upData = upData;
    }

    public List<Method> getMethodData() {
        return methodData;
    }

    public void setMethodData(List<Method> methodData) {
        this.methodData = methodData;
    }
}
