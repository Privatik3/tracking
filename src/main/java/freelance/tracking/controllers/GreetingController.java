package freelance.tracking.controllers;


import freelance.tracking.TimerDownloadTask;
import freelance.tracking.TimerUpdateTask;
import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.Utility;
import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import freelance.tracking.dao.entity.Report;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.task.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GreetingController {

    private final AdDAO adDAO;

    @Autowired
    public GreetingController(AdDAO adDAO) {
        this.adDAO = adDAO;
    }

    @CrossOrigin
    @RequestMapping(value = "/init", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Integer> init(
            @RequestParam(name="taskID") String taskID,
            @RequestParam(name="day") String day) throws Exception {

        List<Schedule> schedules = adDAO.updateTask(Integer.parseInt(taskID), Integer.parseInt(day));
        List<Schedule> complete = schedules.stream().filter(s -> s.getStatus() == Status.COMPLETE).collect(Collectors.toList());
        adDAO.prepareData(new ArrayList<>(complete), Integer.parseInt(taskID));

        return complete.stream().map(Schedule::getTime).collect(Collectors.toList());
    }

    @CrossOrigin
    @RequestMapping(value = "/general-report", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Report> getGeneralReport(
            @RequestParam(name="reportID") String reportID) throws Exception {

        return adDAO.getGeneralReport(reportID);
    }

    @CrossOrigin
    @PostMapping("/get-ads")
    public String getAds(
            @ModelAttribute("taskID") String taskID,
            @ModelAttribute("time") String time,
            @ModelAttribute("sort") String sort,
            @ModelAttribute("selectedID") String selectedID, Model model) {

        List<AdInfo> ads = adDAO.getAdInfo(taskID, time, sort);

        if (!selectedID.isEmpty()) {
            Optional<AdInfo> any = ads.stream().filter(ad -> ad.getId().equals(selectedID)).findAny();
            any.ifPresent(ad -> ad.setSelected(true));
        }

        model.addAttribute("ads", ads);
        return "greeting";
    }

    @CrossOrigin
    @RequestMapping(value = "/stat", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<AdStat> stat(
            @ModelAttribute("day") String day,
            @RequestParam(name="taskID") String taskID,
            @RequestParam(name="adID") String adID) {

        return adDAO.getAdStat(adID, taskID, Integer.parseInt(day));
    }

    @CrossOrigin
    @GetMapping("/update")
    @ResponseBody
    public void update() {
        Date date = new Date();
        int timerOffset = (60 - date.getMinutes()) * 60_000;
        timerOffset += (60 - date.getSeconds()) * 1_000;

        System.out.println("-------------------- START TIMER --------------------");
        new Timer().schedule(new TimerDownloadTask(adDAO), timerOffset, 60 * 60 * 1000);
        new Timer().schedule(new TimerUpdateTask(adDAO), (timerOffset + (10 * 60 * 1000)), 15 * 60 * 1000);
    }
}
