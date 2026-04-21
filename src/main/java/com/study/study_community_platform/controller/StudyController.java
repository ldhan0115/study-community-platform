package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.study.EditStudyForm;
import com.study.study_community_platform.controller.web.study.RegisterStudyForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.service.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;

    // 스터디 등록 폼 이동
    @GetMapping("/new")
    public String registerForm(Model model){

        RegisterStudyForm form = new RegisterStudyForm();
        model.addAttribute("studyForm", form);

        return "studies/registerStudyForm";
    }

    // 스터디 등록
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

    // 스터디 전체 목록 조회
    @GetMapping
    public String list(Model model){

        List<Study> studies = studyService.findStudies();
        model.addAttribute("studies", studies);
        return "studies/studyList";
    }

    // 스터디 상세 조회
    @GetMapping("/{studyId}")
    public String detail(@PathVariable Long studyId, Model model){

        model.addAttribute("study", studyService.findStudy(studyId));
        return "studies/studyDetail";
    }

    // 스터디 수정 폼 이동
    @GetMapping("/{studyId}/edit")
    public String editForm(@PathVariable Long studyId,
                           @Login Member loginMember,
                           Model model){

        Study study = studyService.findStudy(studyId);

        // 현재 로그인한 사용자가 스터디 작성자인지 검증 (URL 직접 접근 차단)
        if(!study.getMember().getId().equals(loginMember.getId())){
            log.warn("권한 없는 사용자의 스터디 수정 접근 memberId={}, studyId={}", loginMember.getId(), study.getId());
            // 해당 스터디 조회 폼으로 이동
            return "redirect:/studies/" + studyId;
        }

        EditStudyForm form = new EditStudyForm();

        form.setId(study.getId());
        form.setTitle(study.getTitle());
        form.setCapacity(study.getCapacity());
        form.setMethod(study.getMethod());
        form.setRegion(study.getRegion());
        form.setContent(study.getContent());

        model.addAttribute("editForm", form );
        return "studies/editStudyForm";
    }

    @PostMapping("/{studyId}/edit")
    public String edit(@Validated @ModelAttribute("editForm") EditStudyForm form,
                       BindingResult bindingResult,
                       @Login Member loginMember,
                       @PathVariable Long studyId){

        Study study = studyService.findStudy(studyId);

        // API(Postman 등)를 통한 비정상적인 POST 수정 요청 차단
        if (!study.getMember().getId().equals(loginMember.getId())) {
            return "redirect:/studies/" + studyId;
        }

        if(bindingResult.hasErrors()){
            return "studies/editStudyForm";
        }

        studyService.updateStudy(studyId, form.getTitle(), form.getContent(),
                form.getMethod(), form.getRegion(), form.getCapacity());

        return "redirect:/studies/" + studyId;
    }
}
