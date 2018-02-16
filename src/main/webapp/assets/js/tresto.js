function alerttest(){
        alert('hi');
    }
	
function checkAll(fn){                    
            bx = fn();  //Do the ajax call
			//alert(bx);
			var sr = document.getElementById('selectallrow')
            for (var bxs=document.getElementById('selectabletable').getElementsByTagName('input'),j=bxs.length; j--; ) {
			if (j!= 0) {
                   if (bxs[j].type=='checkbox' ) {
                      bxs[j].checked = sr.checked;
				   }
				}
			}			
    }
	
