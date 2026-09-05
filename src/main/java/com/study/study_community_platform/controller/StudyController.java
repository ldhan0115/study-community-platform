package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.controller.web.study.EditStudyForm;
import com.study.study_community_platform.controller.web.study.RegisterStudyForm;
import com.study.study_community_platform.domain.*;
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
    public String register(@Login LoginMemberSession loginMember,
                           @Validated @ModelAttribute("studyForm") RegisterStudyForm form,
                           BindingResult bindingResult){

        // 오프라인일 때만 지역 정보가 필수이므로 폼 에러를 동적으로 제어
        if(form.getMethod() != StudyMethod.ONLINE && (form.getRegion() == null || form.getRegion().trim().isEmpty())){
            bindingResult.rejectValue("region", "required", "지역을 입력해주세요.");
        }

        if(bindingResult.hasErrors()){
            return "studies/registerStudyForm";
        }

        studyService.registerStudy(loginMember.id(), form.getTitle(), form.getContent(),
                form.getMethod(), form.getRegion(), form.getCapacity());

        return "redirect:/studies";
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
                         @Login LoginMemberSession loginMember,
                         Model model){

        model.addAttribute("study", studyService.findStudy(studyId));

        // 비로그인 사용자 NPE 방어
        boolean isApplied = false;
        if(loginMember != null){
            isApplied = applicationService.isApplied(loginMember.id(), studyId);
        }

        // 신청 여부 모델에 담아서 전달
        model.addAttribute("isApplied", isApplied);

        // 해당 스터디의 댓글 조회 후 모델에 담아 전달
        List<Comment> comments = commentService.findCommentsByStudyId(studyId);
        model.addAttribute("comments", comments);

        return "studies/studyDetail";
    }

    // 스터디 수정 폼 이동
    @GetMapping("/{studyId}/edit")
    public String editForm(@PathVariable Long studyId,
                           @Login LoginMemberSession loginMember,
                           Model model){

        Study study;

        try{
            // 기존 controller가 하던 조회와 작성자 검증을 Service가 수행하도록 수정
            study = studyService.findStudyForOwner(loginMember.id(), studyId);
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 스터디 수정 접근 memberId={}, studyId={}",  loginMember.id(), studyId);

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
                       @Login LoginMemberSession loginMember,
                       @PathVariable Long studyId){

        if(form.getMethod() != StudyMethod.ONLINE &&
                form.getRegion() == null || form.getRegion().trim().isEmpty()){
            bindingResult.rejectValue("region", "required", "지역을 입력해주세요.");
        }

        if(bindingResult.hasErrors()){
            return "studies/editStudyForm";
        }

        try{
            // 로그인 회원 ID를 전달하면 Service가 스터디 작성자인지 검증한 후 수정
            studyService.updateStudy(
                    loginMember.id(),
                    studyId,
                    form.getTitle(),
                    form.getContent(),
                    form.getMethod(),
                    form.getRegion(),
                    form.getCapacity()
            );
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 스터디 수정 시도 memberId={}, studyId={}", loginMember.id(), studyId);

            return "redirect:/studies/" + studyId;
        }


        return "redirect:/studies/" + studyId;
    }

    // 스터디 신청
    @PostMapping("{studyId}/apply")
    public String applyForm(@RequestParam String message,
                            @PathVariable Long studyId,
                            @Login LoginMemberSession loginMember,
                            RedirectAttributes redirectAttributes){


        // 신청 성공 여부에 따라 사용자에게 결과 보여줌
        try{
            applicationService.applyToStudy(loginMember.id(), studyId, message);
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
            @Login LoginMemberSession loginMember,
            Model model){

        Study study;

        try{
            // 신청자 관리 화면은 스터디 작성자만 조회 가능
            study = studyService.findStudyForOwner(loginMember.id(), studyId);
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 신청자 관리 접근 memberId={}, studyId={}", loginMember.id(), studyId);

            return "redirect:/studies/" + studyId;
        }

        List<Application> applications = applicationService.findApplicationsByStudy(studyId);

        model.addAttribute("study", study);
        model.addAttribute("applications", applications);
        return "studies/applicantList";
    }

    // 스터디 삭제
    @PostMapping("/{studyId}/delete")
    public String deleteStudy(@Login LoginMemberSession loginMember, @PathVariable Long studyId){

        try{
            // controller가 로그인 회원 ID와 삭제 대상 ID만 전달, 실제 작성자 검증과 삭제는 Service가 수행
            studyService.deleteStudy(loginMember.id(), studyId);
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 스터디 삭제 시도 memberId={}, studyId={}", loginMember.id(), studyId);
        }

        return "redirect:/studies";
    }
}
