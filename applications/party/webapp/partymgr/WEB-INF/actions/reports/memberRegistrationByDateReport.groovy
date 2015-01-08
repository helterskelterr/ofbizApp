import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.EntityFindOptions;
import java.text.SimpleDateFormat;

import javolution.util.FastList;

//partyId = parameters.partyId
//loanStatusId = parameters.loanStatusId
//loanProductId = parameters.loanProductId
//LloanProductId = loanProductId.toLong();
//
//context.product_id = LloanProductId
//product = delegator.findOne("LoanProduct", [loanProductId : LloanProductId], false);
//context.product = product;

//if i have lloanProductId && oanStatusId && partyId
//if (loanStatusId && partyId) {
//  LloanStatusId = loanStatusId.toLong();
//  LpartyId = partyId.toLong();
//  context.loanDetailsList = delegator.findByAnd("LoanApplication", [loanStatusId : LloanStatusId, loanProductId : LloanProductId, partyId : LpartyId], null, false)
//  return
//}

//if i have loanStatusId && loanProductId
//if (loanStatusId) {
//  LloanStatusId = loanStatusId.toLong();
//  context.loanDetailsList = delegator.findByAnd("LoanApplication", [loanStatusId : LloanStatusId, loanProductId : LloanProductId], null, false)
//  return
//}
//
////if i have partyId && loanProductId
//if (partyId) {
//  LpartyId = partyId.toLong();
//  context.loanDetailsList = delegator.findByAnd("LoanApplication", [loanProductId : LloanProductId, partyId : LpartyId], null, false)
//  return
//}
//
//
////or else just query with the default passed loanProductId
//context.loanDetailsList = delegator.findByAnd("LoanApplication", [loanProductId : LloanProductId], null, false)
startDate = parameters.startDate
endDate = parameters.endDate
memberStatusId = parameters.memberStatusId
branchId = parameters.branchId

print " -------- Start Date"
println startDate

print " -------- End Date"
println endDate


//dateStartDate = Date.parse("yyyy-MM-dd hh:mm:ss", startDate).format("dd/MM/yyyy")
dateStartDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(startDate);
dateEndDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(endDate);

print "formatted Date"
println dateStartDate
println dateEndDate

java.sql.Date sqlStartDate = new java.sql.Date(dateStartDate.getTime());
java.sql.Date sqlEndDate = new java.sql.Date(dateEndDate.getTime());

exprBldr = new org.ofbiz.entity.condition.EntityConditionBuilder()

if (memberStatusId){

	if (branchId){
		expr = exprBldr.AND() {
			GREATER_THAN_EQUAL_TO(joinDate: sqlStartDate)
			LESS_THAN_EQUAL_TO(joinDate: sqlEndDate)
			EQUALS(memberStatusId: memberStatusId.toLong())
			EQUALS(branchId: branchId)
		}
	} else{

		expr = exprBldr.AND() {
			GREATER_THAN_EQUAL_TO(joinDate: sqlStartDate)
			LESS_THAN_EQUAL_TO(joinDate: sqlEndDate)
			EQUALS(memberStatusId: memberStatusId.toLong())

		}

	}
} else{
	if (branchId){
		expr = exprBldr.AND() {
			GREATER_THAN_EQUAL_TO(joinDate: sqlStartDate)
			LESS_THAN_EQUAL_TO(joinDate: sqlEndDate)
			EQUALS(branchId: branchId)
		}

	} else{
		expr = exprBldr.AND() {
			GREATER_THAN_EQUAL_TO(joinDate: sqlStartDate)
			LESS_THAN_EQUAL_TO(joinDate: sqlEndDate)
		}
	}


}

EntityFindOptions findOptions = new EntityFindOptions();
findOptions.setMaxRows(100);
membersList = delegator.findList("Member", expr, null, ["joinDate ASC"], findOptions, false)


////conditionList = FastList.newInstance()
//conditionList.add(EntityCondition.makeCondition("joinDate", EntityOperator.GREATER_THAN_EQUAL_TO, dateStartDate))
//conditionList.add(EntityCondition.makeCondition("joinDate", EntityOperator.LESS_THAN_EQUAL_TO, dateEndDate))
//
//condition = EntityCondition.makeCondition(conditionList, EntityOperator.AND)
////membersList = delegator.find("Member", condition, null, fieldsToSelect, null, efo);
//membersList = delegator.find("Member", condition, null, null, null, null)
////context.membersList =  = delegator.findByAnd("Member", [joinDate : LloanProductId, partyId : LpartyId], null, false)
////context.loanRepaymentList = delegator.findByAnd("LoanRepayment", null, null, false)
context.membersList = membersList

