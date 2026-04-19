package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.study.RegisterStudyForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.service.MemberService;
import com.study.study_community_platform.service.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;
    private final MemberService memberService;

    @GetMapping("/new")
    public String registerForm(Model model){

        RegisterStudyForm form = new RegisterStudyForm();
        model.addAttribute("studyForm", form);

        return "studies/registerStudyForm";
    }

    @PostMapping("/new")
    public String register(@Login Member loginMember,
                           @Validated @ModelAttribute("studyForm") RegisterStudyForm form,
                           BindingResult bindingResult){

        if(bindingResult.hasErrors()){
            return "studies/registerStudyForm";
        }

        studyService.registerStudy(loginMember.getId(), form.getTitle(), form.getContent(),
                form.getMethod(), form.getRegion(), form.getCapacity());

        return "redirect:/";
    }

    @GetMapping
    public String list(Model model){

        List<Study> studies = studyService.findStudies();
        model.addAttribute("studies", studies);
        return "studies/studyList";
    }
}
