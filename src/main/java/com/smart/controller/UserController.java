package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	@ModelAttribute
	public void addCommonData(Model model,Principal principal)
	{
		String userName=principal.getName();
		System.out.println(userName);
		
		User user=userRepository.getUserByUserName(userName);
		System.out.println(user);
		
		model.addAttribute("user",user);
	}
	
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	{
		model.addAttribute("title","Dashboard");
		return "normal/user_dashboard";
	}
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Principal principle,
			HttpSession session)
	{
		try {
		String name=principle.getName();
		User user=this.userRepository.getUserByUserName(name);
		
		if(file.isEmpty())
		{
			contact.setImage("contact.png");
		}
		else
		{
			contact.setImage(file.getOriginalFilename());
			File saveFile=new ClassPathResource("static/img").getFile();
			Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
			
		}
		user.getContacts().add(contact);
		contact.setUser(user);
		this.userRepository.save(user);
		System.out.println(contact);
		session.setAttribute("message", new Message("your contact is added !! Add more..","success"));
		}catch(Exception e)
		{
			System.out.println("ERROR: "+e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong !! Try again..","danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	//show contacts handler
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal)
	{
		m.addAttribute("title","Show Contacts");
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		Pageable pageable= PageRequest.of(page, 5);
		Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(),pageable);
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		return "normal/show_contacts";
	}
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal)
	{
		
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		if(user.getId()==contact.getUser().getId())
		{
		model.addAttribute("contact",contact);
		}
		return "normal/contact_detail";
	}
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,HttpSession session,Principal principal)
	{
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		
		Contact contact=contactOptional.get();
		//contact.setUser(null);
		//this.contactRepository.delete(contact);
		User user=this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		session.setAttribute("message", new Message("Contact Deleted Successfully","success"));
		return "redirect:/user/show-contacts/0";
	}
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable Integer cid,Model m)
	{
		m.addAttribute("title","Update Contact");
		Contact contact=this.contactRepository.findById(cid).get();
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	@RequestMapping(value="/process-update",method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model m,HttpSession session,Principal principal)
	{
		Contact oldContactDetail=this.contactRepository.findById(contact.getcId()).get();
		try {
			if(!file.isEmpty())
			{
				File saveFile=new ClassPathResource("static/img").getFile();
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
			}else
			{
				contact.setImage(oldContactDetail.getImage());
			}
			User user=this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Contact Updated","success"));
		}catch(Exception e)
		{
		e.printStackTrace();
		}
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	@GetMapping("/profile")
	public String yourProfile(Model m)
	{
		m.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	@GetMapping("/settings")
	public String openSettings(Model m)
	{
		m.addAttribute("title","Setting");
		return "normal/settings";
	}
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword")String oldPassword,@RequestParam("newPassword")String newPassword, Model m,Principal principal,HttpSession session)
	{
		String userName=principal.getName();
		User currentUser=this.userRepository.getUserByUserName(userName);
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Password changed successfully","success"));
		}
		else
		{
			session.setAttribute("message", new Message("Wrong old password","warning"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
	
}
