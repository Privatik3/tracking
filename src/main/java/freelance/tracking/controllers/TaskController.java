package freelance.tracking.controllers;

import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.Utility;
import freelance.tracking.dao.entity.task.Record;
import freelance.tracking.dao.entity.task.Status;
import freelance.tracking.dao.entity.task.TaskLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class TaskController {

    private final AdDAO adDAO;

    @Autowired
    public TaskController(AdDAO adDAO) {
        this.adDAO = adDAO;
    }

    @CrossOrigin
    @RequestMapping(value = "/add_task", method = POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> addDelayTask(@RequestBody String param) {

        try {
            HashMap<String, String> params = Utility.parseTaskParams(param);

            // Ограничиваем количество страниц
            params.putIfAbsent("max_pages", "1");

            // Проводим настройку фильтров
            params.putIfAbsent("position", "true");

            // Отключаем не нужные
            params.remove("photo");
            params.remove("date");
            params.remove("description");
            params.remove("descriptionLength");
            params.remove("sellerName");
            params.remove("phone");

            Record record = new Record();
            record.setTitle(params.get("title").replaceAll("\\s\\|\\s\\d+-.*$", ""));
            record.setNick(params.get("token"));
            record.setAllTime((Integer.parseInt(params.get("days")) * 24) - 1);
            params.remove("days");

            record.setParams(params);
            record.setStatus(Status.WAIT);

            adDAO.createTaskRecord(record);
            return ResponseEntity.ok("{}");
        } catch (TaskLimitException e) {
            System.err.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Не удалось добавить запрос в очередь.");
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/remove_task", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> removeTask(@RequestParam(name = "taskID") Integer taskID) {
        try {
            /*  1. Чистим таблицу data
                2. Удаляем папру таска
                3. Чистим schedule
                4. Чистим task_param
                5. Удаляем сам таск */

            adDAO.clearData(taskID);
            Utility.clearTaskReports(taskID);
            adDAO.clearSchedule(taskID);
            adDAO.clearTaskParams(taskID);
            adDAO.removeTask(taskID);

            return ResponseEntity.ok("{}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/history", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Record> getHistory(@RequestParam(name = "nick") String nick)  {
        return adDAO.getHistory( nick);
    }
}
