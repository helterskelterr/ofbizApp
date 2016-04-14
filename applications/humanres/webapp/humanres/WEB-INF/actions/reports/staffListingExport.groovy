

action = request.getParameter("action");

stafflist = [];
 staff = delegator.findByAnd("EmployeeRoleView", [isSeparated:  "N" ], null, false);
 
 staff.eachWithIndex { staffItem, index ->
 
 payrollNo = staffItem.getString("employeeNumber");
 fname = staffItem.getString("firstName");
 lname = staffItem.getString("lastName");
 IdNo = staffItem.getString("nationalIDNumber");
 gender = staffItem.getString("gender");
 appointmentDate = staffItem.getString("appointmentdate");
 pin = staffItem.getString("pinNumber"); 
 nhif = staffItem.getString("nhifNumber");
 nssf = staffItem.getString("socialSecurityNumber");
 isManagement = staffItem.getString("isManagement");
 
 fullName = fname +"  "+lname
 
 
 stafflist.add([payrollNo :payrollNo, fname :fullName, lname : lname, IdNo : IdNo, gender : gender,
 pin : pin, nhif : nhif, nssf : nssf, appointmentDate : appointmentDate, isManagement : isManagement]);
 }
 
 
context.stafflist = stafflist;
