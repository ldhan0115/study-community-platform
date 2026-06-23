package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.study.EditStudyForm;
import com.study.study_community_platform.controller.web.study.RegisterStudyForm;
import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.service.ApplicationService;
import com.study.study_community_platform.service.CommentService;
import com.study.study_community_platform.service.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;
    private final ApplicationService applicationService;
    private final CommentService commentService;

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
    public String detail(@PathVariable Long studyId,
                         @Login Member loginMember,
                         Model model){

        model.addAttribute("study", studyService.findStudy(studyId));
        // 신청 여부 모델에 담아서 전달
        model.addAttribute("isApplied", applicationService.isApplied(loginMember.getId(), studyId));

        // 해당 스터디의 댓글 조회 후 모델에 담아 전달
        List<Comment> comments = commentService.findCommentsByStudyId(studyId);
        model.addAttribute("comments", comments);

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

    // 스터디 정보 수정
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

    // 스터디 신청
    @PostMapping("{studyId}/apply")
    public String applyForm(@RequestParam String message,
                            @PathVariable Long studyId,
                            @Login Member loginMember,
                            RedirectAttributes redirectAttributes){


        // 신청 성공 여부에 따라 사용자에게 결과 보여줌
        try{
            applicationService.applyToStudy(loginMember.getId(), studyId, message);
            redirectAttributes.addFlashAttribute("successMessage", "스터디 신청이 완료되었습니다.");
        }catch(IllegalStateException e){
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/studies/" + studyId;
    }

    // 스터디 신청 관리 폼 이동
    @GetMapping("/{studyId}/applicants")
    public String applicantList(
            @PathVariable Long studyId,
            @Login Member loginMember,
            Model model){

        Study study = studyService.findStudy(studyId);

        // 현재 로그인한 사용자가 스터디장인지 검증
        if (!study.getMember().getId().equals(loginMember.getId())) {
            log.warn("권한 없는 사용자의 신청자 관리 접근 memberId={}, studyId={}", loginMember.getId(), study.getId());
            return "redirect:/studies/" + studyId;
        }

        List<Application> applications = applicationService.findApplicationsByStudy(studyId);

        model.addAttribute("study", study);
        model.addAttribute("applications", applications);
        return "studies/applicantList";
    }

    // 스터디 삭제
    @PostMapping("/{studyId}/delete")
    public String deleteStudy(@Login Member loginMember, @PathVariable Long studyId){

        Study study = studyService.findStudy(studyId);

        if(study.getMember().getId().equals(loginMember.getId())){
            studyService.deleteStudy(studyId);
        }else{
            log.warn("권한 없는 사용자의 스터디 삭제 시도. memberId={}, studyId={}", loginMember.getId(), studyId);
        }

        return "redirect:/studies";
    }
}
