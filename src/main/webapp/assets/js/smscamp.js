function checkCampaignName(){
	campname = document.getElementById('campname');
    ntxt = document.getElementById('smsCampNotice');	
	if (campname.value.length == 0 || campname.value.length > 64){
		ntxt.innerHTML = 'Err';
		return false;
	} else
		ntxt.innerHTML = '';
	return true;
}
function checkSource(){
	source = document.getElementById('source');
	ntxt = document.getElementById('smsSourceNotice');
	return v_checkSource(source,ntxt);
}
function checkLength() {
    sms = document.getElementById('smstext');
    ntxt = document.getElementById('smsMessageNotice');	
	return v_checkLength(sms,ntxt);
}
function checkDate(){
	ntxt = document.getElementById('scheduleNotice');		
	if ($('#scheduleddate').datepicker("getDate") < new Date()) {
		ntxt.innerHTML='Errd';
		return false;
	} else
		ntxt.innerHTML='';
	return true;
}
function checkRunnable(){
	run = document.getElementById('runnable');
    ntxt = document.getElementById('runnableNotice');	
	from = run.value.split("-")[0];
	to = run.value.split("-")[1];
	fromD = new Date(from.split(":")[0] * 3600000 + from.split(":")[1] * 60000);
	toD = new Date(to.split(":")[0] * 3600000 + to.split(":")[1] * 60000);
	if (fromD > toD){
		ntxt.innerHTML='Errd';	
		return false;
	} else
		ntxt.innerHTML='';
	return true;
}

function validation(){
	btn = document.getElementById('btnsend');	
	if (checkCampaignName() &&
		checkSource() &&
		checkLength() &&
		checkDate() &&
		checkRunnable()){
		btn.disabled = false;
	} else
		btn.disabled = true;
}