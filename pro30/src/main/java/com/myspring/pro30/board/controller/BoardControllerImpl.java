package com.myspring.pro30.board.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myspring.pro30.board.service.BoardService;
import com.myspring.pro30.board.vo.ArticleVO;
import com.myspring.pro30.member.vo.MemberVO;


@Controller("boardController")
public class BoardControllerImpl  implements BoardController{
	
	private static String ARTICLE_IMAGE_REPO = "C:\\board\\article_image";
	
	@Autowired
	BoardService boardService;
	@Autowired
	ArticleVO articleVO;
	
	@Override
	@RequestMapping(value= "/board/listArticles.do", method = {RequestMethod.GET})
	public ModelAndView listArticles(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String viewName = (String)request.getAttribute("viewName");
		List articlesList = boardService.listArticles();
		ModelAndView mav = new ModelAndView(viewName);
		mav.addObject("articlesList", articlesList);
		return mav;
		
	}
	
	@RequestMapping(value = "/board/*Form.do", method =  {RequestMethod.GET, RequestMethod.POST})
	private ModelAndView form(HttpServletRequest request, 
							  RedirectAttributes rAttr, //리다이렉트시 매개변수를 전달한다.
						       HttpServletResponse response) throws Exception {
		String viewName = (String)request.getAttribute("viewName");
		ModelAndView mav = new ModelAndView(viewName);
		
		HttpSession session = request.getSession();
		MemberVO member = (MemberVO) session.getAttribute("member");
		if(member != null) {	// 로그인 확인 유무
				mav.addObject("member", member);
		} else {
			rAttr.addAttribute("result", "noLogin");
			mav.setViewName("redirect:/member/loginForm.do");
		}
		
		int parentNO = 0;
		if(request.getParameter("parentNO") != null &&
				!request.getParameter("parentNO").equals("")) {
			parentNO = Integer.parseInt(request.getParameter("parentNO"));
		}
		mav.addObject("parentNO", parentNO);
		return mav;
	}
	
	
	@RequestMapping(value = "/board/addNewArticle.do", method =  RequestMethod.POST)
	@ResponseBody
	private ResponseEntity addNewArticle(MultipartHttpServletRequest multipartRequest, 
						       HttpServletResponse response) throws Exception {
		multipartRequest.setCharacterEncoding("utf-8");

		Map<String, Object> articleMap = new HashMap<String, Object>();
		Enumeration enu =  multipartRequest.getParameterNames();
		  while(enu.hasMoreElements()) {
			  String name = (String) enu.nextElement();
			  String value = multipartRequest.getParameter(name);
			  articleMap.put(name, value);
		  }
		
		// 업로드한 이미지 파일 이름을 가져온다.
		String imageFileName = upload(multipartRequest);
		HttpSession session = multipartRequest.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("member");
		String id = memberVO.getId();
		articleMap.put("parentNo", 0);
		articleMap.put("id", id);
		articleMap.put("imageFileName", imageFileName);
		
		ResponseEntity resEnt = null;
		String message;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		
		try {
			// DB에 저장
			int articleNO = boardService.addNewArticle(articleMap);
			// 글번호로 된 폴더로 업로드한 이미지 파일을 이동한다.
			if(imageFileName != null && imageFileName.length() != 0) {
				File srcFile = new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName);
				File destDir = new File(ARTICLE_IMAGE_REPO +"\\"+ articleNO);
				
				FileUtils.moveFileToDirectory(srcFile, destDir, true);
			}
			message = "<script>";
			message += " alert('새글을 추가했습니다.');";
			message += " location.href='"
					  + multipartRequest.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
		}catch(Exception e) {
			File srcFile = new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName);
			srcFile.delete();
			
			message = "<script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='"
					  + multipartRequest.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
			e.printStackTrace();
		}
		
		return resEnt;
	}
	
	
	@RequestMapping(value= "board/viewArticle.do", method = {RequestMethod.GET})
	public ModelAndView viewArticle(@RequestParam("articleNO") int articleNO,
						HttpServletRequest request, 
						HttpServletResponse response) throws Exception {
		
		String viewName = (String)request.getAttribute("viewName");
		
		articleVO = boardService.viewArticle(articleNO);
		
		ModelAndView mav = new ModelAndView(viewName);
		mav.addObject("article", articleVO);
		
		return mav;
		
	}
	
	@RequestMapping(value = "/board/modArticle.do", method =  RequestMethod.POST)
	@ResponseBody
	private ResponseEntity modArticle(MultipartHttpServletRequest multipartRequest, 
						       HttpServletResponse response) throws Exception {
		multipartRequest.setCharacterEncoding("utf-8");

		Map<String, Object> articleMap = new HashMap<String, Object>();
		Enumeration enu =  multipartRequest.getParameterNames();
		  while(enu.hasMoreElements()) {
			  String name = (String) enu.nextElement();
			  String value = multipartRequest.getParameter(name);
			  articleMap.put(name, value);
		  }
		
		// 업로드한 이미지 파일 이름을 가져온다.
		String imageFileName = upload(multipartRequest);
		HttpSession session = multipartRequest.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("member");
		String id = memberVO.getId();
		articleMap.put("parentNo", 0);
		articleMap.put("id", id);
		articleMap.put("imageFileName", imageFileName);
		
		ResponseEntity resEnt = null;
		String message;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		
		try {
			// DB에 저장
			boardService.modArticle(articleMap);
			
			String articleNO = (String) articleMap.get("articleNO");
			
			if(imageFileName != null && imageFileName.length() != 0) {
				// 글번호로 된 폴더로 업로드한 이미지 파일을 이동한다.
				File srcFile = new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName);
				File destDir = new File(ARTICLE_IMAGE_REPO +"\\"+ articleNO);
				FileUtils.moveFileToDirectory(srcFile, destDir, true);
				
				// 기존 파일 삭제
				String orignalFileName = (String) articleMap.get("orignalFileName");
				File oldFile = new File(ARTICLE_IMAGE_REPO +"\\"+ articleNO +"\\"+ orignalFileName);
				oldFile.delete();
				
			}
			message = "<script>";
			message += " alert('새글을 추가했습니다.');";
			message += " location.href='"
					  + multipartRequest.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
		}catch(Exception e) {
			File srcFile = new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName);
			srcFile.delete();
			
			message = "<script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='"
					  + multipartRequest.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
			e.printStackTrace();
		}
		
		return resEnt;
	}
	
	@RequestMapping(value = "/board/removeArticle.do", method =  RequestMethod.POST)
	@ResponseBody
	private ResponseEntity removeArticle(@RequestParam("articleNO") int articleNO,
							   HttpServletRequest Request, 
						       HttpServletResponse response) throws Exception {
		Request.setCharacterEncoding("utf-8");
		
		ResponseEntity resEnt = null;
		String message;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		
		
		
		try {
			boardService.removeArticle(articleNO);
			File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
			FileUtils.deleteDirectory(destDir);
			
			message = "<script>";
			message += " alert('삭제했습니다.');";
			message += " location.href='"
					  + Request.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
		}catch(Exception e) {
		
			
			message = "<script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='"
					  + Request.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
			e.printStackTrace();
		}
		
		
		
		return  resEnt;
	}
	
		
	@RequestMapping(value = "/board/addReply.do", method =  RequestMethod.POST)
	@ResponseBody
	private ResponseEntity addReply(MultipartHttpServletRequest multipartRequest, 
						       HttpServletResponse response) throws Exception {
		multipartRequest.setCharacterEncoding("utf-8");

		Map<String, Object> articleMap = new HashMap<String, Object>();
		Enumeration enu =  multipartRequest.getParameterNames();
		  while(enu.hasMoreElements()) {
			  String name = (String) enu.nextElement();
			  String value = multipartRequest.getParameter(name);
			  articleMap.put(name, value);
		  }
		
		// 업로드한 이미지 파일 이름을 가져온다.
		String imageFileName = upload(multipartRequest);
		HttpSession session = multipartRequest.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("member");
		String id = memberVO.getId();
		articleMap.put("parentNO", multipartRequest.getParameter("parentNO"));
		System.out.println("multipartRequest.getParameter(\"parentNO\") == "+ multipartRequest.getParameter("parentNO"));
		articleMap.put("id", id);
		articleMap.put("imageFileName", imageFileName);
		
		ResponseEntity resEnt = null;
		String message;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		
		try {
			// DB에 저장
			int articleNO = boardService.addNewArticle(articleMap);
			// 글번호로 된 폴더로 업로드한 이미지 파일을 이동한다.
			if(imageFileName != null && imageFileName.length() != 0) {
				File srcFile = new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName);
				File destDir = new File(ARTICLE_IMAGE_REPO +"\\"+ articleNO);
				
				FileUtils.moveFileToDirectory(srcFile, destDir, true);
			}
			message = "<script>";
			message += " alert('새글을 추가했습니다.');";
			message += " location.href='"
					  + multipartRequest.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
		}catch(Exception e) {
			File srcFile = new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName);
			srcFile.delete();
			
			message = "<script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='"
					  + multipartRequest.getContextPath() +"/board/listArticles.do'; ";
			message += "</script>";
			
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			
			e.printStackTrace();
		}
		
		return resEnt;
	}	
	
	private String upload(MultipartHttpServletRequest multipartRequest) 
			throws ServletException, IOException {

			String imageFileName = null;
		
			Map<String, String> articleMap = new HashMap<String, String>();
			Iterator fileNames = multipartRequest.getFileNames();
			while(fileNames.hasNext()) {
				String fileName = (String) fileNames.next();
				MultipartFile mFile = multipartRequest.getFile(fileName);
				imageFileName = mFile.getOriginalFilename();
				
				File file = new File(ARTICLE_IMAGE_REPO +"\\"+ fileName);
				if(mFile.getSize() != 0) { // File Null Check
					
					if(! file.exists()) { // 경로에 파일이 없으면 그 경로에 해당하는 디렉토리를 만든 후 파일 생성
						if(file.getParentFile().mkdirs()) {
							file.createNewFile();
						}
					}
					mFile.transferTo(new File(ARTICLE_IMAGE_REPO +"\\"+ "temp" +"\\"+ imageFileName));
					
				}
			}
			
		return imageFileName;
	}
	
	
}
