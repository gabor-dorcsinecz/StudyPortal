function isGSMString(check) {
	for(i = 0; i < check.length; ++i) {
	  found = false;
	  for(c = 0; c < bytes.length; ++c) {
		if (bytes.charAt(c) == check.charAt(i)) {
		  found = true;
		  break;
		}
	  }
	  if (!found)
		return false;
	}
	return true;
}
function normalizePhone(phone) {
	if (phone.indexOf("+") == 0)
		 phone = phone.substring(1);
	if (phone.indexOf("00") == 0)
		 phone = phone.substring(3);
	if (phone.indexOf("00") == 06)
		 phone = phone.substring(3);
	return phone;
}
function validatePhoneNumber(phone) {
	return (phone.indexOf("3620") == 0 || phone.indexOf("3630") == 0 || phone.indexOf("3670") == 0) && phone.length == 11;
}

bytes_ = '@L$YeéuioÇOoAa?_?????????^{}\[~]|€AaßÉ !"#%&\'()*+,-./0123456789:;<=>?!ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖNÜ§?abcdefghijklmnopqrstuvwx';
bytes = '@? 0!P?pL_!1AQaq$?"2BRbrY?#3CScse?¤4DTdté?%5EUeuu?&6FVfvi?\'7GWgwo?(8HXhxÇ?)9IYiy?*:JZjzO+;KÄkäoA,<LÖlöa-=MNmnAß.>NÜnüaÉ/?O§oa^|}{€[~]\\';

function v_checkLength(sms, ntxt) {
	if (sms == null || ntxt == null || sms.value.len == 0)
		return false;
	len = sms.value.length;
	maxlen = 160;
	isGSM = isGSMString(sms.value);
	if (isGSM){
		if (len <= 160)
			ntxt.innerHTML = STR_REMAINING_CHARS.replace("%1", (160 - len).toString() );
		else
			if (len <= 306)
				ntxt.innerHTML = STR_REMAINING_CHARS_2SMS.replace("%1", (306 - len).toString() );
			else
				if (len <= 459)
					ntxt.innerHTML = STR_REMAINING_CHARS_3SMS.replace("%1", (459 - len).toString() );
				else
					ntxt.innerHTML = STR_REMAINING_CHARS_OVER3SMS.replace("%1",  (len - 459).toString() );
	} else {
		if (len <= 70)
			ntxt.innerHTML = STR_REMAINING_CHARS.replace("%1", (70 - len).toString() );
		else
			if (len <= 126)
				ntxt.innerHTML = STR_REMAINING_CHARS_2SMS.replace("%1", (126 - len).toString() );
			else
				if (len <= 189)
					ntxt.innerHTML = STR_REMAINING_CHARS_3SMS.replace("%1", (189 - len).toString() );
				else {
					ntxt.innerHTML = STR_REMAINING_CHARS_OVER3SMS.replace("%1",  (len - 189).toString() );
					return false
				}
	}
	return true;
}

function v_checkTarget(targets, ntxt) {
    phones = "";
    phonesCount = 0;
    invalidPhones = "";
    invalidPhonesCount = 0;
    values = targets.value.split(",");    
    for(i = 0; i < values.length; ++i)
    {
        var p = normalizePhone(values[i].replace(/^\s\s*/, '').replace(/\s\s*$/, ''));
        if (p.length > 0) {
            // allow only 3620,3630,3670
            if (validatePhoneNumber(p)) {
                phones += p;
                phonesCount += 1;
            } else {
                invalidPhones += p+",";
                invalidPhonesCount += 1;
            } 
        }
    }
    var res = "";
    if (invalidPhonesCount > 0) {
        res = STR_INVALID_PHONENUMBERS_COUNT.replace("%1", invalidPhonesCount.toString() );
    }
    if (phonesCount > 0) {
        res += STR_VALID_PHONENUMBERS_COUNT.replace("%1", phonesCount.toString() );
    }
    ntxt.innerHTML = res;
	if (invalidPhonesCount > 0 || phonesCount == 0)
		return false;
	return true;	
}
function v_checkSource(source, ntxt) {	
	src = source.value;
	if (src.charAt(0) == '+') 
		src = src.substring(1);
	if (src.match("^\\d+$")){
		if (src.length > 16){
			ntxt.innerHTML = STR_SOURCE_NUM_TOOLONG;
			return false;
		} else {
			ntxt.innerHTML = '';
			return true;
		}
	} else {
		if (src.length > 11) {
			ntxt.innerHTML = STR_SOURCE_ALPHANUM_TOOLONG;
			return false;
		} else
			if (!src.match(/^[a-z0-9]+$/i)) {
				ntxt.innerHTML = STR_SOURCE_INVALID;
				return false;
			} else {
				ntxt.innerHTML = '';
				return true;
			}
	}
}