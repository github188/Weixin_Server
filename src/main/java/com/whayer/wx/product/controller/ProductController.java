package com.whayer.wx.product.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageInfo;
import com.whayer.wx.common.X;
import com.whayer.wx.common.io.FileUtil;
import com.whayer.wx.common.mvc.BaseController;
import com.whayer.wx.common.mvc.Box;
import com.whayer.wx.common.mvc.ResponseCondition;
import com.whayer.wx.product.service.ProductService;
import com.whayer.wx.product.vo.Product;


@Controller
@RequestMapping("/product")
public class ProductController extends BaseController{

	private final static Logger log = LoggerFactory.getLogger(ProductController.class);
	
	@Resource
	ProductService productService;
	
	/**
	 * 获取所有产品 (PC)
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/getList", method = RequestMethod.GET)
	@ResponseBody
	public ResponseCondition getList(HttpServletRequest request, HttpServletResponse response){
		log.info("ProductController.getList()");
		Box box = loadNewBox(request);
		
		String name = box.$p("name");
		
		PageInfo<Product> pi = productService.getProductList(name, box.getPagination());
		
		return pagerResponse(pi);
	}
	
	/**
	 * 根据角色(用户编码)获取具有权限的产品列表 (wechat)
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/getListByType", method = RequestMethod.GET)
	@ResponseBody
	public ResponseCondition getListByType(HttpServletRequest request, HttpServletResponse response){
		log.info("ProductController.getListByType()");
		Box box = loadNewBox(request);
		
		/**
		 * code: 用户类型
		 * 1:  个人代理编码
		 * 2:  区域代理编码
		 * xxx:集团用户编码
		 */
		
		String code = box.$p("code");
		if(isNullOrEmpty(code)){
			return getResponse(X.FALSE);
		}
		
		PageInfo<Product> pi = productService.getProductListByUserType(code, box.getPagination());
		
		return pagerResponse(pi);
	}
	
	/**
	 * 获取所有产品,并指明和当前角色是否有关联(标识哪些产品是已做关联)
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/getList2Role", method = RequestMethod.GET)
	@ResponseBody
	public ResponseCondition getList2Role(HttpServletRequest request, HttpServletResponse response){
		log.info("ProductController.getList2Role()");
		Box box = loadNewBox(request);
		
		/**
		 * code: 用户类型
		 * 1:  个人代理编码
		 * 2:  区域代理编码
		 * xxx:集团用户编码
		 */
		
		String code = box.$p("code");
		if(isNullOrEmpty(code)){
			return getResponse(X.FALSE);
		}
		
		PageInfo<Product> pi = productService.getProductList2Role(code, box.getPagination());
		
		return pagerResponse(pi);
	}
	
	/**
	 * 获取产品详情
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/findById", method = RequestMethod.GET)
	@ResponseBody
	public ResponseCondition findById(HttpServletRequest request, HttpServletResponse response){
		log.info("ProductController.findById()");
		Box box = loadNewBox(request);
		
		
		String id = box.$p("id");
		if(isNullOrEmpty(id)){
			return getResponse(false);
		}
		Product product = productService.getProductById(id);
		
		ResponseCondition res = getResponse(true);
		List<Product> list = new ArrayList<>();
		list.add(product);
		res.setList(list);
		return res;
	}
	
	@RequestMapping(value = "/deleteById", method = RequestMethod.POST)
	@ResponseBody
	public ResponseCondition deleteById(HttpServletRequest request, HttpServletResponse response){
		log.info("ProductController.deleteById()");
		Box box = loadNewBox(request);
		
		
		String id = box.$p("id");
		if(isNullOrEmpty(id)){
			return getResponse(false);
		}
		
		Product product = productService.getProductById(id);
		if(!isNullOrEmpty(product)){
			String imgUrl = product.getImgUrl();
			
			String uploadPath = X.getConfig("file.upload.dir");
			uploadPath += "/product";
			//X.makeDir(uploadPath);
			if(!isNullOrEmpty(imgUrl)){
				File file = new File(uploadPath, imgUrl);
				file.delete();
			}
		}
		
		int count = productService.deleteProductById(id);
		
		if(count > 0){
			return getResponse(X.TRUE);
		}else{
			ResponseCondition res = getResponse(X.FALSE);
			res.setErrorMsg("删除产品失败");
			log.error("删除产品失败");
			return res;
		}
	}
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public ResponseCondition save(
			@RequestParam(value = "file", required = false) MultipartFile file,
			HttpServletRequest request, HttpServletResponse response) throws IOException{
		log.info("ProductController.save()");
		Box box = loadNewBox(request);
		
		
		String id = X.uuidPure();
		String categoryId = box.$p("categoryId");
		String name = box.$p("name");
		//String imgUrl = box.$p("imgUrl");
		BigDecimal price = new BigDecimal(box.$p("price"));
		String description = box.$p("description");
		
		if(isNullOrEmpty(name) || isNullOrEmpty(price) || isNullOrEmpty(categoryId)){
			return getResponse(false);
		}
		Product product = new Product();
		product.setId(id);
		product.setCategoryId(categoryId);
		//product.setImgUrl(imgUrl);
		product.setName(name);
		product.setPrice(price);
		product.setDescription(description);
		
		if(!isNullOrEmpty(file)){
			String originFileName = file.getOriginalFilename();
			String extension = FileUtil.getExtension(originFileName);
			originFileName = FileUtil.getFileNameWithOutExtension(originFileName);
			Pattern pattern = Pattern.compile(X.REGEX);
			Matcher matcher = pattern.matcher(originFileName);
			if (matcher.find()) {
				originFileName = matcher.replaceAll("_").trim();
			}
			originFileName = X.uuidPure8Bit()/*originFileName*/ + X.DOT + extension;
			
			ResponseCondition res = getResponse(X.FALSE);
			if (file.getSize() == 0 || file.isEmpty()) {
				log.error("文件不能为空");
				res.setErrorMsg("文件不能为空");
				return res;
			}
			// check if too large
			int maxSize = X.string2int(X.getConfig("file.upload.max.size"));
			if (file.getSize() > maxSize) {
				log.error("文件太大");
				res.setErrorMsg("文件太大");
				return res;
			}
			
			String uploadPath = X.getConfig("file.upload.dir");
			uploadPath += "/product";
			X.makeDir(uploadPath);
			File targetFile = new File(uploadPath, originFileName);
		    file.transferTo(targetFile);
		    
		    //设置产品图片路径
		    product.setImgUrl(originFileName);
		}
		
		int count = productService.saveProduct(product);
		
		if(count > 0){
			return getResponse(X.TRUE);
		}else{
			ResponseCondition res = getResponse(X.FALSE);
			res.setErrorMsg("保存产品失败");
			log.error("保存产品失败");
			return res;
		}
	}
	
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	@ResponseBody
	public ResponseCondition update(
			@RequestParam(value = "file", required = false) MultipartFile file,
			HttpServletRequest request, HttpServletResponse response) throws IOException{
		log.info("ProductController.update()");
		Box box = loadNewBox(request);
		
		String id = box.$p("id");
		String categoryId = box.$p("categoryId");
		String name = box.$p("name");
		BigDecimal price = new BigDecimal(box.$p("price"));
		String description = box.$p("description");
		
		ResponseCondition res = getResponse(X.FALSE);
		
		if(isNullOrEmpty(id) || isNullOrEmpty(categoryId)){
			return res;
		}
		
		Product product = productService.getProductById(id);
		
		if(isNullOrEmpty(product)){
			res.setErrorMsg("没有此产品!");
			return res;
		}
		
		product.setCategoryId(categoryId);
		product.setName(name);
		product.setDescription(description);
		product.setPrice(price);
		
		if(!isNullOrEmpty(file) && file.getSize() > 0){
			String originFileName = file.getOriginalFilename();
			String extension = FileUtil.getExtension(originFileName);
			originFileName = FileUtil.getFileNameWithOutExtension(originFileName);
			Pattern pattern = Pattern.compile(X.REGEX);
			Matcher matcher = pattern.matcher(originFileName);
			if (matcher.find()) {
				originFileName = matcher.replaceAll("_").trim();
			}
			originFileName = X.uuidPure8Bit()/*originFileName*/ + X.DOT + extension;
			
			if (file.getSize() == 0 || file.isEmpty()) {
				log.error("文件不能为空");
				res.setErrorMsg("文件不能为空");
				return res;
			}
			// check if too large
			int maxSize = X.string2int(X.getConfig("file.upload.max.size"));
			if (file.getSize() > maxSize) {
				log.error("文件太大");
				res.setErrorMsg("文件太大");
				return res;
			}
			
			String uploadPath = X.getConfig("file.upload.dir");
			uploadPath += "/product";
			X.makeDir(uploadPath);
			File targetFile = new File(uploadPath, originFileName);
		    file.transferTo(targetFile);
		    
		    //删除旧图
		    if(!isNullOrEmpty(product.getImgUrl())){
		    	File oldFile = new File(uploadPath, product.getImgUrl());
		    	oldFile.delete();
		    }
		    
		    //设置产品图片路径
		    product.setImgUrl(originFileName);
		}
		
		int count = productService.updateProduct(product);
		if(count > 0){
			return getResponse(X.TRUE);
		}else{
			res.setErrorMsg("更新产品失败");
			log.error("更新产品失败");
			return res;
		}
	}
	
	/**
	 * 更新产品上下线状态
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/updateOnOrOff", method = RequestMethod.POST)
	@ResponseBody
	public ResponseCondition updateOnOrOff(HttpServletRequest request, HttpServletResponse response) {
		log.info("ProductController.updateOnOrOff()");
		Box box = loadNewBox(request);
		String id = box.$p("id");
		String value = box.$p("isOff");
		if(isNullOrEmpty(id) || isNullOrEmpty(value)){
			return getResponse(X.FALSE);
		}
		Integer isOff = X.string2int(value);
		
		int count = productService.updateOnOrOff(id, isOff);
		
		if(count > 0){
			return getResponse(X.TRUE);
		}else{
			ResponseCondition res = getResponse(X.FALSE);
			res.setErrorMsg("更新产品上下架失败");
			log.error("更新产品上下架失败");
			return res;
		}
	}
}
