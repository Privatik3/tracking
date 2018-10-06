package freelance.tracking.controllers;


import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class GreetingController {

    @Autowired
    AdDAO adDAO;

    @CrossOrigin(origins = "http://superseller.pro")
    @GetMapping("/init")
    public String init(Model model) {
        return "init";
    }

    @CrossOrigin(origins = "http://superseller.pro")
    @GetMapping("/greeting")
    public String getAds(
            @RequestParam(name="time") String time,
            @RequestParam(name="sort", required=false, defaultValue="total_view") String sort, Model model) {

        List<AdInfo> ads = adDAO.getAdInfo(time, sort);
        model.addAttribute("ads", ads);
        return "greeting";
    }

    @CrossOrigin(origins = "http://superseller.pro")
    @GetMapping("/stat")
    public String stat(
            @RequestParam(name="taskID", required=false, defaultValue="2") String taskID,
            @RequestParam(name="adID", required=false, defaultValue="1439859209") String adID, Model model) {


        List<AdStat> stats = adDAO.getAdStat(adID, taskID);
        model.addAttribute("stats", stats);
        return "stat";
    }
}
