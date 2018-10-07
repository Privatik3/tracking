package freelance.tracking.controllers;


import freelance.tracking.dao.AdDAO;
import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class GreetingController {

    @Autowired
    AdDAO adDAO;

    @CrossOrigin
    @GetMapping("/init")
    public String init(Model model) {
        return "init";
    }

    @CrossOrigin
    @PostMapping("/get-ads")
    public String getAds(
            @ModelAttribute("time") String time,
            @ModelAttribute("sort") String sort,
            @ModelAttribute("selectedID") String selectedID,
            @ModelAttribute("adInfo") AdInfo adInfo, Model model) {

        List<AdInfo> ads = adDAO.getAdInfo(time, sort);


        if (!selectedID.isEmpty()) {
            Optional<AdInfo> any = ads.stream().filter(ad -> ad.getId().equals(selectedID)).findAny();
            any.ifPresent(ad -> {
                ad.setSelected(true);
                ad.changeSelectedStatus(adInfo);
            });
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
}
