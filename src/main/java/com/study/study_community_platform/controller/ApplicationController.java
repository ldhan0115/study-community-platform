package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public String listForm(
            @Login Member member,
            Model model){

        model.addAttribute("applications", applicationService.findApplicationsByMember(member.getId()));
        return "applications/applicationList";
    }
}
