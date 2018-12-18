package freelance.tracking.controllers;


import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import freelance.tracking.dao.entity.Report;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class GreetingController {

    private final AdDAO adDAO;

    @Autowired
    public GreetingController(AdDAO adDAO) {
        this.adDAO = adDAO;
    }

    @CrossOrigin
    @GetMapping("/init")
    public List<Schedule> init(
            @RequestParam(name="taskID") String taskID,
            @RequestParam(name="day") String day) throws Exception {

        List<Schedule> schedules = adDAO.updateTask(taskID, Integer.parseInt(day));
        List<Schedule> complete = schedules.stream().filter(s -> s.getStatus() == Status.COMPLETE).collect(Collectors.toList());
        adDAO.prepareData(complete, taskID);

        return complete;
    }

    @CrossOrigin
    @RequestMapping(value = "/general-report", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Report> getGeneralReport(
            @RequestParam(name="reportID") String reportID) {

        return adDAO.getGeneralReport(reportID);
    }

    @CrossOrigin
    @PostMapping("/get-ads")
    public String getAds(
            @ModelAttribute("day") String day,
            @ModelAttribute("time") String time,
            @ModelAttribute("sort") String sort,
            @ModelAttribute("selectedID") String selectedID, Model model) {

        List<AdInfo> ads = adDAO.getAdInfo(time, sort);

        if (!selectedID.isEmpty()) {
            Optional<AdInfo> any = ads.stream().filter(ad -> ad.getId().equals(selectedID)).findAny();
            any.ifPresent(ad -> ad.setSelected(true));
        }

        model.addAttribute("ads", ads);
        return "greeting";
    }

    @CrossOrigin
    @GetMapping("/stat")
    public String stat(
            @RequestParam(name="taskID", required=false, defaultValue="2") String taskID,
            @RequestParam(name="adID", required=false, defaultValue="1439859209") String adID, Model model) {

        List<AdStat> stats = adDAO.getAdStat(adID, taskID);
        model.addAttribute("stats", stats);
        return "stat";
    }

    @CrossOrigin
    @GetMapping("/update")
    public void update() {

        int taskID = 2;

    }
}
