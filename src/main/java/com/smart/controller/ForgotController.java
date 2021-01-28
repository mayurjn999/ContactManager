package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;

@Controller
public class ForgotController {
	Random random=new Random(1000);
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private EmailService emailService;
	@Autowired
	private UserRepository userRepository;
	
	@RequestMapping("/forgot")
	public String openEmailForm()
	{
		return "forgot_email_form";
	}
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email,HttpSession session)
	{
		
		int otp=random.nextInt(9999);
		String subject="OTP from SCM";
		String message="OTP : "+otp;
						
						
		String to=email;
		boolean flag=this.emailService.sendEmail(subject, message, to);
		if(flag)
		{
			
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		}
		else
		{
			session.setAttribute("message", new Message("Check Your Email Id","danger"));
			return "forgot_email_form";
		}
		
	}
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp,HttpSession session)
	{
		int myotp=(int)session.getAttribute("myotp");
		String email=(String)session.getAttribute("email");
		if(myotp==otp)
		{
			User user=this.userRepository.getUserByUserName(email);
			if(user==null)
			{
				session.setAttribute("message", new Message("User does not exits with this email !!","danger"));
				return "forgot_email_form";
			}
			else
			{
				
			}
			return "password_change_form";
		}
		else
		{
			session.setAttribute("message", new Message("Entered wrong OTP !!","danger"));
			return "verify_otp";
		}
	}
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newpassword,HttpSession session)
	{
		String email=(String)session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
		this.userRepository.save(user);
		return "redirect:/signin?change=Password changed successfully..";	
	}
}
