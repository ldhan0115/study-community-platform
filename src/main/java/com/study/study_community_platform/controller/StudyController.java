package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.controller.web.study.EditStudyForm;
import com.study.study_community_platform.controller.web.study.RegisterStudyForm;
import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.exception.BusinessRuleException;
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
        if(form.getMethod() != StudyMethod.ONLINE &&
                ((form.getRegion() == null || form.getRegion().trim().isBlank()))){
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

        Study study = studyService.findStudyForOwner(loginMember.id(), studyId);

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
                (form.getRegion() == null || form.getRegion().trim().isBlank())){
            bindingResult.rejectValue("region", "required", "지역을 입력해주세요.");
        }

        if(bindingResult.hasErrors()){
            return "studies/editStudyForm";
        }

        studyService.updateStudy(loginMember.id(), studyId, form.getTitle(),
                form.getContent(), form.getMethod(), form.getRegion(), form.getCapacity());

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
        }catch(BusinessRuleException e){
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

        Study study = studyService.findStudyForOwner(loginMember.id(), studyId);

        List<Application> applications = applicationService.findApplicationsByStudy(studyId);

        model.addAttribute("study", study);
        model.addAttribute("applications", applications);
        return "studies/applicantList";
    }

    // 스터디 삭제
    @PostMapping("/{studyId}/delete")
    public String deleteStudy(@Login LoginMemberSession loginMember, @PathVariable Long studyId){

        // 작성자가 아니라면 Service가 ForbiddenOperationException을 발생시키고 GlobalExceptionHandler가 403으로 처리
        studyService.deleteStudy(loginMember.id(), studyId);

        return "redirect:/studies";
    }
}
