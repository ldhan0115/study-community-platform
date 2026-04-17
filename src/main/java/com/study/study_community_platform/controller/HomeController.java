package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.domain.Member;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(
            // 세션에서 로그인 회원 정보 조회 (세션이 없어도 예외가 발생하지 않도록 required = false)
            @SessionAttribute(value = SessionConst.LOGIN_MEMBER, required = false) Member loginMember,
            Model model){

        // 로그인 세션이 없으면 기본 홈 화면으로 이동
        if(loginMember == null){
            return "home";
        }

        // 로그인 세션이 있으면 모델에 담아 로그인 전용 홈 화면으로 이동
        model.addAttribute("member", loginMember);
        return "loginHome";
    }
}