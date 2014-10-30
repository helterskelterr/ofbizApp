package org.ofbiz.humanres;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.ofbiz.accountholdertransactions.AccHolderTransactionServices;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.webapp.event.EventHandlerException;
import org.ofbiz.entity.jdbc.ConnectionFactory;

import com.google.gson.Gson;

public class HumanResServices {
	public static Logger log = Logger.getLogger(LeaveServices.class);
	
	// ============================================================== 
public static String getLeaveBalance(HttpServletRequest request,
			HttpServletResponse response) {
		Map<String, Object> result = FastMap.newInstance();
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		Date appointmentdate = null;
		try {
			appointmentdate = (Date)(new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("appointmentdate")));
		} catch (ParseException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String leaveTypeId = new String((request.getParameter("leaveTypeId")).toString());
		String partyId = new String(request.getParameter("partyId")).toString();
		
		//   get current leave balance  //
		
		List<GenericValue> getApprovedLeaveSumELI = null;
		GenericValue carryOverLeaveGV = null;
	      try {
	    	  carryOverLeaveGV = delegator.findOne("EmplCarryOverLost", 
	             	UtilMisc.toMap("partyId", partyId), false);
	           	log.info("++++++++++++++carryOverLeaveGV++++++++++++++++" +carryOverLeaveGV);
	             }
	       catch (GenericEntityException e) {
	            e.printStackTrace();;
	       }  
	       double carryOverLeaveDays = carryOverLeaveGV.getDouble("carryOverLeaveDays");
		EntityConditionList<EntityExpr> leaveConditions = EntityCondition
				.makeCondition(UtilMisc.toList(
					EntityCondition.makeCondition(
						"partyId", EntityOperator.EQUALS, partyId),
					EntityCondition.makeCondition("leaveTypeId",EntityOperator.EQUALS, leaveTypeId),
					EntityCondition.makeCondition("applicationStatus", EntityOperator.EQUALS, "LEAVE_APPROVED")),
						EntityOperator.AND);

		try {
			getApprovedLeaveSumELI = delegator.findList("EmplLeave",
					leaveConditions, null, null, null, false);
		} catch (GenericEntityException e2) {
			//e2.printStackTrace();
			return "Cannot Get approved leaves";
		}
		log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++"+getApprovedLeaveSumELI);
	double approvedLeaveSum = 0;
	double  usedLeaveDays = 0;
	double lostLeaveDays = 0;
		for (GenericValue genericValue : getApprovedLeaveSumELI) {
			 approvedLeaveSum += genericValue.getDouble("leaveDuration");
		}
		log.info("============================================================" +approvedLeaveSum);
		
		// ============ get accrual rate ================ //
	double accrualRate = 0; 
	GenericValue accrualRates = null;
		try {
			 accrualRates = delegator.findOne("EmplLeaveType",
					UtilMisc.toMap("leaveTypeId", leaveTypeId), false);
		} catch (GenericEntityException e) {
			return "Cannot Get Accrual Rate";
		}
		if (accrualRates != null) {

			accrualRate = accrualRates.getDouble("accrualRate");
			
		} else {
			System.out.println("######## Accrual Rate not found #### ");
		}
		
		//========= ==============================//
	
		LocalDateTime stappointmentdate = new LocalDateTime(appointmentdate);
		LocalDateTime stCurrentDate = new LocalDateTime(Calendar.getInstance()
				.getTimeInMillis());
		
		PeriodType monthDay = PeriodType.months();

		Period difference = new Period(stappointmentdate, stCurrentDate, monthDay);

		int months = difference.getMonths();
		String approvedLeaveSumed = Double.toString(approvedLeaveSum);
		double accruedLeaveDay = months * accrualRate;
		double leaveBalances =  accruedLeaveDay + carryOverLeaveDays - approvedLeaveSum; 
		String accruedLeaveDays = Double.toString(accruedLeaveDay);
		String leaveBalance = Double.toString(leaveBalances);

		//return leaveBalance;
		result.put("approvedLeaveSumed",approvedLeaveSumed );
		result.put("accruedLeaveDays", accruedLeaveDays);
		result.put("leaveBalance" , leaveBalance);

		Gson gson = new Gson();
		String json = gson.toJson(result);

		// set the X-JSON content type
		response.setContentType("application/x-json");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(json.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			try {
				throw new EventHandlerException("Problems with Json encoding",
						e);
			} catch (EventHandlerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// return the JSON String
		Writer out;
		try {
			out = response.getWriter();
			out.write(json);
			out.flush();
		} catch (IOException e) {
			try {
				throw new EventHandlerException(
						"Unable to get response writer", e);
			} catch (EventHandlerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		return json;

	}
// ==============================================================


	public static String getLeaveDuration(HttpServletRequest request,
			HttpServletResponse response) {
		Map<String, Object> result = FastMap.newInstance();

		Date fromDate = null;
		try {
			fromDate = (Date)(new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("fromDate")));
		} catch (ParseException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		Date thruDate = null;
		try {
			thruDate = (Date)(new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("thruDate")));
		} catch (ParseException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		Logger log = Logger.getLogger(HumanResServices.class);
		log.info("LLLLLLLLL FROM : "+fromDate);
		log.info("LLLLLLLLL TO : "+thruDate);

		int leaveDuration = AccHolderTransactionServices.calculateWorkingDaysBetweenDates(fromDate, thruDate);
		
		result.put("leaveDuration", leaveDuration);

		Gson gson = new Gson();
		String json = gson.toJson(result);

		// set the X-JSON content type
		response.setContentType("application/x-json");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(json.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			try {
				throw new EventHandlerException("Problems with Json encoding",
						e);
			} catch (EventHandlerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// return the JSON String
		Writer out;
		try {
			out = response.getWriter();
			out.write(json);
			out.flush();
		} catch (IOException e) {
			try {
				throw new EventHandlerException(
						"Unable to get response writer", e);
			} catch (EventHandlerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		return json;

	}
	
	public static String  getLeaveEnd(HttpServletRequest request,
			HttpServletResponse response) {
		
		Map<String, Object> result = FastMap.newInstance();
		Date fromDate = null;
		
		try {
			fromDate = (Date)(new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("fromDate")));
		} catch (ParseException e2) {
			e2.printStackTrace();
		}
		
		int leaveDuration = new Integer(request.getParameter("leaveDuration")).intValue();

		LocalDateTime dateFromDate = new LocalDateTime(fromDate.getTime());

		Date endDate = AccHolderTransactionServices.calculateEndWorkingDay(fromDate, leaveDuration);
		
		int leaveTillResumption = leaveDuration+1;
		Date resumeDate = AccHolderTransactionServices.calculateEndWorkingDay(fromDate, leaveTillResumption);
		
		
		SimpleDateFormat sdfDisplayDate = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		
		String i18ThruDate = sdfDisplayDate.format(endDate);
	    String thruDate = sdfDate.format(endDate);
	    
		String i18resumptionDate = sdfDisplayDate.format(resumeDate);
	    String resumptionDate = sdfDate.format(resumeDate);
	    
	    
	    result.put("resumptionDate_i18n", i18resumptionDate);
	    result.put("resumptionDate", resumptionDate);
	    
		
	    result.put("thruDate_i18n", i18ThruDate);
	    result.put("thruDate", thruDate);
	    
	    
	    Gson gson = new Gson();
		String json = gson.toJson(result);

		// set the X-JSON content type
		response.setContentType("application/x-json");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(json.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			try {
				throw new EventHandlerException("Problems with Json encoding",
						e);
			} catch (EventHandlerException e1) {
				e1.printStackTrace();
			}
		}

		// return the JSON String
		Writer out;
		try {
			out = response.getWriter();
			out.write(json);
			out.flush();
		} catch (IOException e) {
			try {
				throw new EventHandlerException(
						"Unable to get response writer", e);
			} catch (EventHandlerException e1) {
				e1.printStackTrace();
			}
		}

		return json;


	}
	
	
	
	public static String getBranches(HttpServletRequest request, HttpServletResponse response){
		Map<String, Object> result = FastMap.newInstance();
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		String bankDetailsId = (String) request.getParameter("bankDetailsId");
		//GenericValue saccoProduct = null;
		//EntityListIterator branchesELI;// = delegator.findListIteratorByCondition("BankBranch", new EntityExpr("bankDetailsId", EntityOperator.EQUALS,  bankDetailsId), null, UtilMisc.toList("bankBranchId", "branchName"), "branchName", null);
		//branchesELI = delegator.findListIteratorByCondition(dynamicViewEntity, whereEntityCondition, havingEntityCondition, fieldsToSelect, orderBy, findOptions)
		//branchesELI = delegator.findListIteratorByCondition("BankBranch", new EntityExpr("productId", EntityOperator.NOT_EQUAL, null), UtilMisc.toList("productId"), null);
		List<GenericValue> branchesELI = null;
		
		//branchesELI = delegator.findList("BankBranch", new EntityExpr(), UtilMisc.toList("bankBranchId", "branchName"), null, null, null);
		try {
			//branchesELI = delegator.findList("BankBranch", EntityCondition.makeConditionWhere("(bankDetailsId = "+bankDetailsId+")"), null, null, null, false);
			branchesELI = delegator.findList("BankBranch", EntityCondition.makeCondition("bankDetailsId", bankDetailsId), null, null, null, false);
		
		} catch (GenericEntityException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		//SaccoProduct
	
		//Add Branches to a list
		
		if (branchesELI == null){
			result.put("", "No Braches");
		}
		
		for (GenericValue genericValue : branchesELI) {
			result.put(genericValue.get("bankBranchId").toString(), genericValue.get("branchName"));
		}
		
		Gson gson = new Gson();
		String json = gson.toJson(result);

		// set the X-JSON content type
		response.setContentType("application/x-json");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(json.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			try {
				throw new EventHandlerException("Problems with Json encoding",
						e);
			} catch (EventHandlerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// return the JSON String
		Writer out;
		try {
			out = response.getWriter();
			out.write(json);
			out.flush();
		} catch (IOException e) {
			try {
				throw new EventHandlerException(
						"Unable to get response writer", e);
			} catch (EventHandlerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		return json;
	}
	
	public static LocalDate calculateConfirmationDate(Date appointmentdate) {
		LocalDate confirmationdate = null;
		LocalDate localDateStartDate = new LocalDate(appointmentdate);
		

			localDateStartDate = localDateStartDate.plusMonths(6);
		
		return confirmationdate;
	}
	
	public static String  getConfirmationDate(HttpServletRequest request,
			HttpServletResponse response) {
		
		Map<String, Object> result = FastMap.newInstance();
		Date appointmentdate = null;
		
		try {
			appointmentdate = (Date)(new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("appointmentdate")));
		} catch (ParseException e2) {
			e2.printStackTrace();
		}
		LocalDate dateAppointmentDate = new LocalDate(appointmentdate);

		LocalDate confirmDate = dateAppointmentDate.plusMonths(6);
		
	
		SimpleDateFormat sdfDisplayDate = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		
		String i18confirmationdate = sdfDisplayDate.format(confirmDate.toDate());
	    String confirmationdate = sdfDate.format(confirmDate.toDate());
	    
	    result.put("confirmationdate_i18n", i18confirmationdate);
	    result.put("confirmationdate", confirmationdate);
	   
	    Gson gson = new Gson();
		String json = gson.toJson(result);

		// set the X-JSON content type
		response.setContentType("application/x-json");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(json.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			try {
				throw new EventHandlerException("Problems with Json encoding",
						e);
			} catch (EventHandlerException e1) {
				e1.printStackTrace();
			}
		}

		// return the JSON String
		Writer out;
		try {
			out = response.getWriter();
			out.write(json);
			out.flush();
		} catch (IOException e) {
			try {
				throw new EventHandlerException(
						"Unable to get response writer", e);
			} catch (EventHandlerException e1) {
				e1.printStackTrace();
			}
		}

		return json;


	}
	
	
	public static String  getRetirementDate(HttpServletRequest request,
			HttpServletResponse response) {
		
		Map<String, Object> result = FastMap.newInstance();
		Date birthDate = null;
		
		try {
			birthDate = (Date)(new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("birthDate")));
		} catch (ParseException e2) {
			e2.printStackTrace();
		}
		LocalDate datebirthDate = new LocalDate(birthDate);

		LocalDate bodDate = datebirthDate.plusYears(55);
		
	
		SimpleDateFormat sdfDisplayDate = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		
		String i18retirementdate = sdfDisplayDate.format(bodDate.toDate());
	    String retirementdate = sdfDate.format(bodDate.toDate());
	    
	    result.put("retirementdate_i18n", i18retirementdate);
	    result.put("retirementdate", retirementdate);
	   
	    Gson gson = new Gson();
		String json = gson.toJson(result);

		// set the X-JSON content type
		response.setContentType("application/x-json");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(json.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			try {
				throw new EventHandlerException("Problems with Json encoding",
						e);
			} catch (EventHandlerException e1) {
				e1.printStackTrace();
			}
		}

		// return the JSON String
		Writer out;
		try {
			out = response.getWriter();
			out.write(json);
			out.flush();
		} catch (IOException e) {
			try {
				throw new EventHandlerException(
						"Unable to get response writer", e);
			} catch (EventHandlerException e1) {
				e1.printStackTrace();
			}
		}

		return json;


	}
	
	
	
	
public static String NextPayrollNumber(Delegator delegator) {
	String newPayrollNo=null;
	
	try {

		String helperNam = delegator.getGroupHelperName("org.ofbiz");    // gets the helper (localderby, localmysql, localpostgres, etc.) for your entity group org.ofbiz
		Connection conn = ConnectionFactory.getConnection(helperNam); 
		Statement statement = conn.createStatement();
		statement.execute("SELECT party_id,employee_number FROM Person a where a.employee_number!='' and created_stamp=(select max(created_stamp) from Person b where employee_number!='')");
		ResultSet results = statement.getResultSet();
			String emplNo=results.getString("employee_number");
			 String trancatemplNo= StringUtils.substring(emplNo, 3);
			 int newEmplNo=Integer.parseInt(trancatemplNo)+1;
			 String h=String.valueOf(newEmplNo);
			 newPayrollNo="HCS".concat(h);
			 
			 log.info("++++++++++++++newPayrollNo++++++++++++++++" +newPayrollNo);
	} catch (Exception e) {
		// TODO: handle exception
	}
	
	
	return newPayrollNo;

	
}
	
}

