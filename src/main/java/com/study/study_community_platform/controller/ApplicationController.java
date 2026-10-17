package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

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

        applicationService.cancelApplication(loginMember.id(), applicationId);

        return "redirect:/applications";
    }

    // 신청 승인
    @PostMapping("/{applicationId}/approve")
    public String approve(@PathVariable Long applicationId,
                          @Login LoginMemberSession loginMember){

        // 신청 조회/잠금/권한 검증/변경을 서비스에서 함께 수행
        Long studyId = applicationService.approveApplication(
                loginMember.id(),
                applicationId
        );

        return "redirect:/studies/" + studyId + "/applicants";
    }

    // 신청 거절
    @PostMapping("/{applicationId}/reject")
    public String reject(@PathVariable Long applicationId,
                         @Login LoginMemberSession loginMember){

        Long studyId = applicationService.rejectApplication(
                loginMember.id(),
                applicationId
        );

        return "redirect:/studies/" + studyId + "/applicants";
    }
}
