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

            // Проводим настройку фильтров
            params.putIfAbsent("position", "true");

            // Отключаем не нужные
            params.remove("photo");
            params.remove("description");
            params.remove("descriptionLength");
            params.remove("sellerName");
            params.remove("Статистика просмотров");
            params.remove("phone");

            Record record = new Record();
            record.setTitle(params.get("title").replaceAll("\\s\\|\\s\\d+-.*$", ""));
            record.setNick(params.get("token"));
            record.setAllTime(Integer.parseInt(params.get("days")) * 24);
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
    public ResponseEntity<String> removeTask(
            @RequestParam(name = "serverID") Integer serverID,
            @RequestParam(name = "taskID") Integer taskID) {

        try {
            // TODO Реализовать удаление таска
            return ResponseEntity.ok(null);
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
