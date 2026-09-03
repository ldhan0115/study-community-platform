package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.service.ApplicationService;
import com.study.study_community_platform.service.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final StudyService studyService;

    // 신청 내역 폼 이동
    @GetMapping
    public String listForm(@Login LoginMemberSession loginMember, Model model){

        model.addAttribute("applications", applicationService.findApplicationsByMember(loginMember.id()));
        return "applications/applicationList";
    }

    // 신청 취소
    @PostMapping("/{applicationId}/cancel")
    public String cancel(@PathVariable Long applicationId,
                         @Login LoginMemberSession loginMember){

        Application application = applicationService.findApplication(applicationId);
        // 현재 로그인한 회원의 신청 내역이 맞는지 확인
        if(!application.getMember().getId().equals(loginMember.id())){
            log.warn("권한 없는 사용자의 신청 취소 접근 memberId={}, applicationId={}", loginMember.id(), applicationId);
            return "redirect:/applications";
        }

        // 신청 상태를 CANCELED로 변경
        applicationService.cancelApplication(applicationId);
        return "redirect:/applications";
    }

    // 신청 승인
    @PostMapping("/{applicationId}/approve")
    public String approve(@PathVariable Long applicationId,
                          @Login LoginMemberSession loginMember,
                          @RequestParam Long studyId,
                          RedirectAttributes redirectAttributes){

        Study study = studyService.findStudy(studyId);
        if(!study.getMember().getId().equals(loginMember.id())){
            log.warn("권한 없는 사용자의 신청 승인 접근 memberId={}, studyId={}", loginMember.id(), studyId);
            return "redirect:/studies/" + studyId + "/applicants";
        }

        try{
            applicationService.approveApplication(applicationId);
        }catch (IllegalStateException e){
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/studies/" + studyId + "/applicants";
    }

    // 신청 거절
    @PostMapping("/{applicationId}/reject")
    public String reject(@PathVariable Long applicationId,
                          @Login LoginMemberSession loginMember,
                          @RequestParam Long studyId){

        Study study = studyService.findStudy(studyId);
        if(!study.getMember().getId().equals(loginMember.id())){
            log.warn("권한 없는 사용자의 신청 거절 접근 memberId={}, studyId={}", loginMember.id(), studyId);
            return "redirect:/studies/" + studyId + "/applicants";
        }

        applicationService.rejectApplication(applicationId);
        return "redirect:/studies/" + studyId + "/applicants";
    }
}
