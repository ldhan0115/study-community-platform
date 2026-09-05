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

        try{
            // 신청자 검증을 ApplicationService가 수행하도록 변경
            applicationService.cancelApplication(loginMember.id(), applicationId);
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 신청 취소 시도 memberId={}, applicationId={}", loginMember.id(), applicationId);
        }

        return "redirect:/applications";
    }

    // 신청 승인
    @PostMapping("/{applicationId}/approve")
    public String approve(@PathVariable Long applicationId,
                          @Login LoginMemberSession loginMember,
                          RedirectAttributes redirectAttributes){

        // 브라우저의 studyId를 사용하지 않고 applicationId가 가리키는 실제 신청의 스터디 ID 사용하는 것으로 수정
        Application application = applicationService.findApplication(applicationId);
        Long studyId = application.getStudy().getId();

        try{
            applicationService.approveApplication(loginMember.id(), applicationId);
        }catch(IllegalStateException e){
            log.warn("신청 승인 실패 memberId={}, applicationId={}", loginMember.id(), applicationId);

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/studies/" + studyId + "/applicants";
    }

    // 신청 거절
    @PostMapping("/{applicationId}/reject")
    public String reject(@PathVariable Long applicationId,
                         @Login LoginMemberSession loginMember,
                         RedirectAttributes redirectAttributes){

        Application application = applicationService.findApplication(applicationId);
        Long studyId = application.getStudy().getId();

        try{
            applicationService.rejectApplication(loginMember.id(), applicationId);
        }catch(IllegalStateException e){
            log.warn("신청 거절 실패 memberId={}, applicationId={}", loginMember.id(), applicationId);

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/studies/" + studyId + "/applicants";
    }
}
