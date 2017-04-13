package com.whayer.wx.login.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.whayer.wx.common.mvc.DAO;
import com.whayer.wx.login.vo.Company;

@Repository
public interface CompanyDao extends DAO{

	public List<Company> getCompanyList();
	
	public Company findById(@Param("id") String id);
	
	public int updateById(Company company);
	
	public int deleteById(@Param("id") String id);
	
	public Company save(Company company);
	
	public Company findByCode(@Param("code") String code);
}
