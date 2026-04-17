package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.domain.Member;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    // argumentResolver를 통해 커스텀 애노테이션(@Login) 도입
    public String home(@Login Member loginMember, Model model){

        // 로그인 세션이 없으면 기본 홈 화면으로 이동
        if(loginMember == null){
            return "home";
        }

        // 로그인 세션이 있으면 모델에 담아 로그인 전용 홈 화면으로 이동
        model.addAttribute("member", loginMember);
        return "loginHome";
    }
}