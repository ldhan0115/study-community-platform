package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.member.EditMemberForm;
import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.controller.web.member.LoginMemberForm;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.MemberService;
import com.study.study_community_platform.service.dto.MemberUpdateDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    // 회원가입 폼 이동
    @GetMapping("/join")
    public String joinForm(Model model){
        model.addAttribute("member", new JoinMemberForm());
        return "members/joinMemberForm";
    }

    // 회원가입
    @PostMapping("/join")
    public String join(@Validated @ModelAttribute("member") JoinMemberForm form, BindingResult bindingResult){

        // 필드 검증 실패 시 회원가입 화면 다시 이동
        if(bindingResult.hasErrors()){
            return "members/joinMemberForm";
        }

        // 중복 검사 예외 처리
        try{
            memberService.join(form);
        }catch(IllegalStateException e){
            // service 에서 던진 메시지를 잡아냄
            String errMessage = e.getMessage();

            // 에러 메시지 내용에 따라 해당하는 필드에 에러 매핑
            if(errMessage.contains("ID")){
                bindingResult.rejectValue("loginId", "duplicate", errMessage);
            }else if(errMessage.contains("EMAIL")){
                bindingResult.rejectValue("email", "duplicate", errMessage);
            }else if(errMessage.contains("NICKNAME")){
                bindingResult.rejectValue("nickname", "duplicate", errMessage);
            }else{
                // 그 외의 예외일 경우 글로벌 에러로 처리
                bindingResult.reject("joinFail", errMessage);
            }

            // 에러를 담고 다시 회원가입 폼으로 이동
            return "members/joinMemberForm";
        }

        return "redirect:/members/login";
    }

    // 로그인 폼 이동
    @GetMapping("/login")
    public String loginForm(Model model){
        model.addAttribute("member", new LoginMemberForm());
        return "members/loginMemberForm";
    }

    // 로그인
    @PostMapping("/login")
    public String login(@Validated @ModelAttribute("member") LoginMemberForm form,
                        BindingResult bindingResult,
                        @RequestParam(defaultValue = "/") String redirectURL,
                        HttpServletRequest request){

        // 필드 검증 실패 시 로그인 화면으로
        if(bindingResult.hasErrors()){
            return "members/loginMemberForm";
        }

        // 아이디/비밀번호 일치하는 회원 반환
        Member authenticatedMember = memberService.login(form.getLoginId(), form.getPassword());

        // 인증 실패 시 글로벌 에러 담아서 로그인 화면으로 이동
        if(authenticatedMember == null){
            bindingResult.reject("loginFail", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "members/loginMemberForm";
        }

        HttpSession session = request.getSession();

        // Member 엔티티 전체가 아닌 인증과 화면 표시에 필요한 최소 정보만 세션에 저장
        LoginMemberSession loginMemberSession = LoginMemberSession.from(authenticatedMember);
        session.setAttribute(SessionConst.LOGIN_MEMBER, loginMemberSession);

        // 로그인 하기 전 주소로 이동
        return "redirect:" + redirectURL;

    }

    // 회원 정보 수정 폼으로 이동
    @GetMapping("/edit")
    // @Login 애노테이션 활용
    // 기존 로그인된 Member 객체를 받는 것에서 필요한 데이터만 저장해놓은 DTO를 받아 활용하는 것으로 수정
    public String editForm(@Login LoginMemberSession loginMember, Model model){

        EditMemberForm form = new EditMemberForm();

        // 세션에는 최소 정보만 존재하므로 회원 수정 화면에 필요한 데이터는 DB에서 최신 상태로 다시 조회해서 대입
        Member member = memberService.findMember(loginMember.id());
        form.setLoginId(member.getLoginId());
        form.setEmail(member.getEmail());
        form.setNickname(member.getNickname());

        model.addAttribute("member", form);
        return "members/editMemberForm";
    }

    // 회원정보 수정
    // 대상 회원 ID를 브라우저가 보내는 값이 아닌 서버의 로그인 정보에서 가져오는 것으로 수정
    @PostMapping("/edit")
    public String edit(@Validated @ModelAttribute("member") EditMemberForm form,
                       BindingResult bindingResult,
                       @Login LoginMemberSession loginMember,
                       HttpServletRequest request){

        // 필드 검증 실패 시 수정 화면 다시 이동
        if(bindingResult.hasErrors()) {
            return "members/editMemberForm";
        }

        // 사용자의 입력을 받은 form을 직접 전달 x
        // 회원 수정에 필요한 데이터 DTO로 변환해서 전달
        MemberUpdateDto memberUpdateDto =
                new MemberUpdateDto(
                    form.getLoginId(), form.getPassword(),
                    form.getEmail(), form.getNickname()
                );

        Member updatedMember = memberService.editMember(loginMember.id(), memberUpdateDto);

        // 정보 수정 완료 후 변경된 정보가 화면에 반영되도록 세션 정보 갱신
        HttpSession session = request.getSession();
        if(session == null){
            throw new IllegalStateException("로그인 세션이 존재하지 않습니다.");
        }


        // 기존 비영속 객체를 세션에 저장한 것에서 DB에서 조회하여 수정한 영속 회원을 저장하여 id가 null이 되는 문제 해결
        session.setAttribute(SessionConst.LOGIN_MEMBER, LoginMemberSession.from(updatedMember));
        return "redirect:/";

    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {

        // 기존 세션만 가져오고 새로 생성하지 않음
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 세션 정보 완전 삭제 및 무효화
            session.invalidate();
        }

        return "redirect:/";
    }

    // 회원 탈퇴
    @PostMapping("/withdraw")
    public String withdraw(@Login LoginMemberSession loginMember, HttpServletRequest request){

        memberService.withdrawMember(loginMember.id());

        HttpSession session = request.getSession(false);
        if(session != null){
            session.invalidate();
        }

        return "redirect:/";
    }


}
