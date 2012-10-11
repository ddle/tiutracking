/* TEntityType, also z-index */
var etDetector	= 0,
	etTag		= 1,
	EntityNames = ['detector', 'tag'],
	EntityClassNames = ['TDetector', 'TTag'];

/**
 *	Creates an AJAX object with 3 custom attributes (mMethod, mUrl, and doSend()).
 *
 *	@param	method	{String}	'GET' or 'POST'.
 *	@param	url		{String}	The request url.
 *	@param	callback	{Function}	The function to be called upon receiving the response.
 */
function AJAX(method, url, callback) {

	/**
	 *	Sends request with certain params.
	 *
	 *	@param	params	{String}	Params to send along, e.g. 'username=abc&password=123'
	 */
	function doSend(params) {
		try {
			if (this.mMethod == 'POST') {
				this.open(this.mMethod, this.mUrl, true);
				this.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
				this.send(params);
			} else {

				this.open(this.mMethod, this.mUrl + '?' + params, true);
				this.send();
			}
		} catch (e) {
			console.log('AJAX failed');
		}
	}
		
	var xhr = new XMLHttpRequest();
	xhr.mMethod = method;
	xhr.mUrl = url;
	xhr.onreadystatechange = callback;
	xhr.doSend = doSend;

	return xhr;
}

/* Response Status */
var rsOK			= 0,
	rsSessionEnd	= 1;

function processResponse(ajax, entityType) {

	var response,
		table = tagTable,
		entities = tags;
	if (entityType == etDetector) {
		table = detectorTable;
		entities = detectors;
	}
	
	if (ajax.readyState == 4 && ajax.status == 200) {
		//try {
			response = eval(ajax.responseText);
			
			if (!response)
				return;
			
			if (response.request == 'login') {
				loginButton.disabled = false;
				if (response.status == rsOK) {
					updateUI(true);
					showMessage(mtInfo, null, true);
				} else
					showMessage(mtError, response.data, true);
				return;
			}
			
			if (response.status != rsOK) {
				deleteRowIndex = -1;
				if (response.status == rsSessionEnd)
					updateUI(false);
				showMessage(mtError, response.data, true);
				return;
			}
			
			if (response.request.indexOf('get') > -1)
				doGet();
			else if (response.request.indexOf('add') > -1)
				doAdd();
			else
				doDelete();
		
		//} catch (e) {
		//	console.log(e);
		//}
	}
	
	function doDelete() {
		table.deleteRow(deleteRowIndex);
		map.removeEntity(entities.splice(deleteRowIndex, 1)[0]);
		deleteRowIndex = -1;
	}
	
	function doAdd() {
		var entity = null, row;
		var info = response.data;
		info.b = BatteryStatus[info.b];
		
		// Check if the entity already exists.
		for (var i = 0; i < entities.length; i++)
			if (entities[i].mId == info.i) {
				entity = entities[i];
				break;
			}
		
		if (entity) {
			// Modify the info of the existing entity.
			
			row = entity.mLinkedRow;
			if (entityType == etTag) {
				row.cells[1].innerHTML = info.a;
				row.cells[2].innerHTML = info.x.toFixed(1);
				row.cells[3].innerHTML = info.y.toFixed(1);
			} else {
				row.cells[1].innerHTML = info.x.toFixed(1);
				row.cells[2].innerHTML = info.y.toFixed(1);
			}
			entity.setPosition(info.x, info.y, map.getScale());
		} else {
			// Creates a new entity.
			
			row = table.insertRow(-1);
			entity = TMapEntity(entityType);

			
			var columnNames = ['Id', 'X', 'Y', 'Battery', 'More'];
			var cellValues  = [info.i, info.x.toFixed(1), info.y.toFixed(1), info.b, ''];
			
			// If entity is a tag, insert Asset ID column at column 1.
			if (entityType == etTag) {
				columnNames.splice(1, 0, 'AssetId');
				cellValues.splice(1, 0, info.a);
			}
			
			var j, cell;
			
			for (j = 0; j < cellValues.length; j++) {
				cell = row.insertCell(-1);
				cell.innerHTML = cellValues[j];
				cell.className = 'Col ' + columnNames[j];
			}
			// The last cell value is the delete button since the user is admin now.
			cell.appendChild(createDeleteButton(entityType, row, table));

			row.mLinkedEntity = entity;
			entity.set(info, row, table);
			
			entities.push(entity);
			map.addEntity(entity);
		}
		// Forces the info box to be repositioned.
		map.selectEntity(null);
		map.selectEntity(entity);				
	}

	function doGet() {
		var columnNames = ['Id', 'X', 'Y', 'Battery', 'More'];
		if (entityType == etTag)
			columnNames.splice(1, 0, 'AssetId');
		
		var newEntityCount = response.data.length - 1,
			curEntityCount = entities.length;
		
		// Someone else might have deleted some entities.
		if (newEntityCount < curEntityCount) {
			for (var i = curEntityCount - 1; i >= newEntityCount; i--) {
				map.removeEntity(entities[i]);
				table.deleteRow(-1);
				entities[i] = null;
			}
		}
		
		entities.length = newEntityCount;
		
		var row, entity, info, cellValues, cell;
		for (var i = 0; i < newEntityCount; i++) {
			
			info = response.data[i];
			info.b = BatteryStatus[info.b];
			cellValues = [info.i, info.x.toFixed(1), info.y.toFixed(1), info.b, '&nbsp;'];
			if (entityType == etTag)
				cellValues.splice(1, 0, info.a);
			
			// If the entity hasn't been created, we need to create it
			// and add it to the table and the map.
			if (!entities[i]) {
				entity = TMapEntity(entityType);
				entities[i] = entity;
				map.addEntity(entity);
				row = table.insertRow(-1);
				for (var j = 0; j < cellValues.length; j++) {
					cell = row.insertCell(-1);
					cell.innerHTML = cellValues[j];
					cell.className = 'Col ' + columnNames[j];
				}
				
				// If logged in, replace the last cell value with a delete button.
				if (isLoggedIn) {
					cell.innerHTML = '';
					cell.appendChild(createDeleteButton(entityType, row, table));
				}
			} else {
				entity = entities[i];
				row = table.getElement().rows[i];
				for (var j = 0; j < cellValues.length - 1; j++) {	// Ignore the last cell since it was updated by updateUI().
					cell = row.cells[j];
					cell.innerHTML = cellValues[j];
				}
			}
			
			row.mLinkedEntity = entity;
			entity.set(info, row, table);
			
			map.invalidate();
		}
	}
}


var headerPanel, msgLabel, tabControl, tabPanel,
	
	welcomeTab, assetsTab, detectorsTab,
	
	tags = [], detectors = [],
	
	tagTable, detectorTable,

	map, mapImage, mapToolbar;

/**
 *	The table that has a row selected, either @tagTable or @detectorTable, exclusively.
 *	Initialize this variable to be either @tagTable or @detectorTable will help optimize
 *	map.onSelectChange().
 */
var activeTable;

var tagUpdateTimer;

var loginDialog, usernameTextBox, passwordTextBox, loginButton, loggedInDialog, isLoggedIn = false;

var buttonAnimationParams = {
	type: 'left',
	unit: 'px',
	to: 0,
	step: 24,
	delay: 20
};

var panelAnimationParams = {
	type: 'left',
	unit: 'px',
	to: 0,
	step: 25,
	delay: 20
};

var msgLabelAnimationParams = {
	type: 'opacity',
	to: 100,
	step: 10,
	delay: 25,
	fadeOut: true,
	fadeOutTimeOut: 4000,
	onfinish: function () {
		var p = msgLabelAnimationParams;
		if (p.to == 0)
			p.to = 100;
		else if (p.fadeOut) {
			p.to = 0;
			p.timerHandle = setTimeout(this.fxRun, p.fadeOutTimeOut);
		}
	}
};


var mtInfo = 0, mtError = 1,
	StatusColors = ['#04F', '#F40'];

/**
 *	@param	color	{String}	CSS color.
 *	@param	text	{String}	The message to be displayed. If null, hide the message label.
 *	@param	fadeOut	{Boolean}	true means keep the status label visible.
 *								false means hide the status label after 2secs.
 *								Default is false.
 */
function showMessage(status, text, fadeOut) {
	clearTimeout(msgLabelAnimationParams.timerHandle);
	if (text != null) {
		if (msgLabelAnimationParams.to == 0)
			msgLabelAnimationParams.to = 100;
		msgLabelAnimationParams.fadeOut = fadeOut;
		msgLabel.style.color = StatusColors[status];
		msgLabel.innerHTML = text;
	} else {
		msgLabelAnimationParams.to = 0;
		msgLabelAnimationParams.fadeOut = false;	// Don't need to run again.
	}
	msgLabel.fxRun();
}

var BatteryStatus = ['Normal', 'Low!'];

var deleteRowIndex = -1;

/*
	Data returned are in JSON format.
{	
	request: String,			// 'login', 'get-tags', 'add-tag', 'del-tag', 'get-detectors', 'add-detector', 'del-detector'
	status:	Integer,			// 0 for no error
	data: RequestSpecificData	// The returned data, if status <> 0, this will be an error message.
}
*/

var loginRequestManager = AJAX('POST', 'Request.php', function () {
	processResponse(this, -1);
});

var tagRequestManager = AJAX('POST', 'Request.php', function () {
	processResponse(this, etTag);
});


var detectorRequestManager = AJAX('POST', 'Request.php', function () {
	processResponse(this, etDetector);
});

function login() {
	if (usernameTextBox.value == '') {
		showMessage(mtError, 'Please enter the username.', true);
		return;
	}
	if (passwordTextBox.value == '') {
		showMessage(mtError, 'Please enter the password.', true);
		return;
	}
	
	showMessage(mtInfo, null, false);
	loginButton.disabled = true;

	loginRequestManager.doSend('request=login&username=' + usernameTextBox.value + '&password=' + passwordTextBox.value);
}

function logout() {
	loginRequestManager.doSend('request=logout');
	updateUI(false);		
}


/**
 *	Creates a delete button. The button can be added to the tag table or the detector table.
 *
 *	@param	entityType	{TEntityType}
 *	@param	linkedTable	{HTMLTableElement}	The table that owns this button.
 *	@param	linkedRow	{HTMLRowElement}	The row that owns this button.
 */
function createDeleteButton(entityType, linkedRow, linkedTable) {
	
	function buttonClick(event) {
		if (deleteRowIndex == -1) {
			deleteRowIndex = this.mLinkedRow.rowIndex;
			var type = this.mEntityType;
			var name = EntityNames[type];
			if (confirm('Delete this ' + name + ' permanently?')) {
				var mngr = this.mEntityType == etTag ? tagRequestManager : detectorRequestManager;
				mngr.doSend('request=del-' + name + '&' + name + '-id=' + this.mLinkedRow.mLinkedEntity.mId);
			}
		} else
			showMessage(mtError, 'Please wait for the current request to finish.', true);
		event.stopPropagation();
	}
	
	var b = newElement(SDiv, 'DeleteButton');
	b.mEntityType = entityType;
	b.mLinkedTable = linkedTable;
	b.mLinkedRow = linkedRow;
	b.onclick = buttonClick;
	b.title = 'Delete';
	
	return b;
}

function updateUI(loggedIn) {
	if (isLoggedIn == loggedIn)
		return;
		
	var d = $('mapUploadFrame');
	d.style.display = loggedIn ? SBlock : SNone;
	if (d.contentWindow)
		d.contentWindow.location.reload();
	else
		frames['mapUploadFrame'].location.reload();
		
	var table, tables = [tagTable.getElement(), detectorTable.getElement()];
	
	if (loggedIn) {
		$('greetingLabel').innerHTML = 'Hi <strong style="color: blue;">' + getCookie('username') + '</strong>';
		loggedInDialog.style.display = SBlock;
		loginDialog.style.display = SNone;
		usernameTextBox.value = '';
		passwordTextBox.value = '';
		
		for (var i = 0; i < tables.length; i++) {
			table = tables[i];
			for (var j = 0, row; j < table.rows.length; j++) {
				row = table.rows[j];
				row.lastChild.innerHTML = '';
				row.lastChild.appendChild(createDeleteButton(i, row, table));	// i = entityType
			}
		}


		d = document.createElement(SDiv);
		d.id = 'addModifyTagDialog';
		d.setAttribute('style', 'float: left; width: 331px; background-color: #EEF; margin-top: 5px;');
		d.innerHTML =
			'<div style="float: left;">' +
				'<input type="textbox" id="tTextBox" class="Col Id Cell" placeholder="ID" />' +
				'<input type="textbox" id="aTextBox" class="Col AssetId Cell" placeholder="Asset ID" />' +
			'</div>' +
			'<button id="addTagButton" style="margin-left: 0.5em; width: 50px;">Add</button>';
		assetsTab.appendChild(d);
		
		$('addTagButton').onclick = function () {
			var t = $('tTextBox'),
				a = $('aTextBox');
			if (t.value == '')
				showMessage(mtError, 'Please enter tag ID', true);
			else if (a.value == '')
				showMessage(mtError, 'Please enter asset ID', true);
			else {
				tagRequestManager.doSend('request=add-tag&tag-id=' + t.value + '&asset-id=' + a.value);
				t.value = a.value = '';
			}
		}
		
		
		
		d = document.createElement(SDiv);
		d.id = 'addModifyDetectorDialog';
		d.title = 'Click on the map to place the detector';
		d.setAttribute('style', 'float: left; width: 205px; background-color: #EEF; margin-top: 5px;');
		d.innerHTML =
			'<div style="float: left;">' +
				'<input type="textbox" id="dTextBox" class="Col Id Cell" placeholder="ID" />' +
				'<input type="textbox" id="xTextBox" class="Col X Cell" placeholder="X" />' +
				'<input type="textbox" id="yTextBox" class="Col Y Cell" placeholder="Y" />' +
			'</div>' +
			'<button id="addDetectorButton" style="margin-left: 0.5em; width: 50px;">Add</button>';
		detectorsTab.appendChild(d);
		
		$('addDetectorButton').onclick = function () {
			var d = $('dTextBox'),
				x = $('xTextBox'),
				y = $('yTextBox');
			if (d.value == '')
				showMessage(mtError, 'Please enter detector ID', true);
			else if (x.value == '' || y.value == '')
				showMessage(mtError, 'Please enter detector\'s location', true);
			else {
				detectorRequestManager.doSend('request=add-detector&detector-id=' + d.value + '&x=' + x.value + '&y=' + y.value);
				d.value = x.value = y.value = '';
			}
		}
		
		map.onClick = function (x, y) {
			$('xTextBox').value = x.toFixed(1);
			$('yTextBox').value = y.toFixed(1);
		}
	} else {
		loginDialog.style.display = SBlock;
		loggedInDialog.style.display = SNone;
		
		for (var i = 0; i < tables.length; i++) {
			table = tables[i];
			for (var j = 0; j < table.rows.length; j++)
				table.rows[j].lastChild.innerHTML = '&nbsp;';
		}

		d = $('addModifyTagDialog');
		if (d)
			assetsTab.removeChild(d);
		
		d = $('addModifyDetectorDialog');
		if (d)
			detectorsTab.removeChild(d);
		
		map.onClick = null;
	}

	isLoggedIn = loggedIn;
}

function TMapEntity(entityType) {

	function set(info, linkedRow, linkedTable) {
		this.mId = info.i;
		this.mX	 = info.x;
		this.mY	 = info.y;
		this.mBattery		= info.b;
		this.mLinkedRow		= linkedRow;
		this.mLinkedTable	= linkedTable;
		// For tag only
		if (info.a) {
			this.mTimestamp = info.s;
			this.mAssetId	= info.a;
		}
	}
	
	function setPosition(x, y, scale) {
		this.mX = x;
		this.mY = y;
		this.onScaleChange(scale);
	}
	
	function scaleChange(scale) {
		// First child is the marker icon.
		this.style.left = (this.mX * scale - this.firstChild.offsetWidth * 0.5) + SPixel;
		this.style.top  = (this.mY * scale - this.offsetHeight) + SPixel;
	}
	
	function tagInfo() {
		return	'<table cellspacing="0">' +
				'<caption>Tag</caption>' +
				'<tr style="border-top: 1px solid #DFDFDF; border-bottom: none;"><td>ID</td><td>&nbsp;:&nbsp;</td><td style="font-weight: bold;">' + this.mId + '</td></tr>' +
				'<tr style="border: none;"><td>Asset ID</td><td>&nbsp;:&nbsp;</td><td style="font-weight: bold;">' + this.mAssetId + '</td></tr>' +
				'<tr style="border: none"><td>Battery</td><td>&nbsp;:&nbsp;</td><td style="font-weight: bold;">' + this.mBattery + '</td></tr></table>';
	}
	
	function detectorInfo() {
		return	'<table cellspacing="0">' +
				'<caption>Detector</caption>' +
				'<tr style="border-top: 1px solid #DFDFDF; border-bottom: none;"><td>ID</td><td>&nbsp;:&nbsp;</td><td style="font-weight: bold;">' + this.mId + '</td></tr>' +
				//'<tr><td>Asset ID</td><td>&nbsp;:&nbsp;</td><td style="font-weight: bold;">' + this.mAssetId + '</td></tr>' +
				'<tr style="border: none"><td>Battery</td><td>&nbsp;:&nbsp;</td><td style="font-weight: bold;">' + this.mBattery + '</td></tr></table>';
	}
	
	var entity = newElement(SDiv, 'TMapEntity');
	entity.style.zIndex = entityType;
	
	// Add the marker icon.
	entity.appendChild(newElement(SDiv, EntityClassNames[entityType]));
	
	entity.set = set;
	entity.setPosition = setPosition;
	entity.onScaleChange = scaleChange;
	
	if (entityType == etTag)
		entity.getInfo = tagInfo;
	else
		entity.getInfo = detectorInfo;
	
	return entity;
}

function requestTagsInfo() {
	tagRequestManager.doSend('request=get-tags');
}

function adjusMapRect() {
	map.setRect(tabPanel.offsetLeft + tabPanel.offsetWidth, headerPanel.offsetHeight, window.innerWidth, window.innerHeight);
}

function getCookie(aName) {
	var i, j, name, value, pairs = document.cookie.split(';');
	for (i = 0; i < pairs.length; i++) {
		j = pairs[i].indexOf('=');
		name = pairs[i].substr(0, j);
		value = pairs[i].substr(j + 1);
		name = name.replace(/^\s+|\s+$/g,'');
		if (name == aName)
			return unescape(value);
	}
	return null;
}

onresize = function () {
	adjusMapRect();
	mapToolbar.style.left = (window.innerWidth - mapToolbar.offsetWidth - 10) + SPixel;
	mapToolbar.style.top  = (window.innerHeight - mapToolbar.offsetHeight - 10) + SPixel;
	tabPanel.style.height = (window.innerHeight - headerPanel.offsetHeight) + SPixel;
}

onload = function () {
	headerPanel = $('headerPanel');
	
	tabPanel = $('tabPanel');
	$fx(tabPanel).fxAdd(panelAnimationParams);
	tabControl = new TTabControl(tabPanel);
	welcomeTab = $('welcomeTab');
	assetsTab = $('assetsTab');
	detectorsTab = $('detectorsTab');

	loginDialog = $('loginDialog');
	usernameTextBox = $('usernameTextBox');
	passwordTextBox = $('passwordTextBox');
	loginButton = $('loginButton');
	loggedInDialog = $('loggedInDialog');
	msgLabel = $('msgLabel');
	$fx(msgLabel).fxAdd(msgLabelAnimationParams);

	var browserName = null;
	if (browser.isIE)
		browserName = 'Internet Explorer';
	else if (browser.isFirefox)
		browserName = 'Mozilla Firefox';
	if (browserName != null)
		showMessage(mtInfo, 'You are using <strong>' + browserName + '</strong>.<br />For the best experience, please use <a href="http://www.google.com/chrome/intl/en/make/download.html?brand=CHKZ" target="_blank" style="font-weight: bold;">Google Chrome</a>', true);

	function tableSelectChange() {
		var row = this.getSelectedRow();
		if (this == activeTable || row != null) {
			var entity = row ? row.mLinkedEntity : null;
			map.selectEntity(entity);
		}
	}
	
	tagTable = new TFlexTable($('assetsTab'));
	tagTable.attachTextBox($('assetSearchBox'), 1);
	tagTable.onSelectChange = tableSelectChange;
	
	detectorTable = new TFlexTable($('detectorsTab'));
	detectorTable.attachTextBox($('detectorSearchBox'), 0);
	detectorTable.onSelectChange = tableSelectChange;
	
	// Imporant!
	activeTable = tagTable;
	
	mapToolbar = $('mapToolbar');
	
	map = new TMap();
	map.onLoad = function () {
		mapToolbar.style.visibility = SVisible;
	}
	map.onSelectChange = function () {
		var idx = -1;
		var e = this.getSelectedEntity();
		if (e) {
			var n = e.mLinkedTable;
			if (activeTable != n) {
				var c = activeTable;
				activeTable = n;
				c.setSelectedIndex(-1);
			}
			idx = e.mLinkedRow.rowIndex;
		}
		activeTable.setSelectedIndex(idx);
	}

	// The resolution is stored in the 'resolution' label :D
	changeMapImage( {status: 0, data: MapResolution} );

	tagUpdateTimer = new TTimer(UpdateIntervalSecs * 1000, requestTagsInfo);
	tagUpdateTimer.setEnabled(true);
	
	requestTagsInfo();
	detectorRequestManager.doSend('request=get-detectors');

	
	var button = $('showHideTabPanelButton');
	button.style.left = (tabPanel.offsetWidth - button.offsetWidth - 3) + SPixel;
	button.style.top  = (tabPanel.offsetTop + 3) + SPixel;
	button.onclick = function () {
		if (this.offsetLeft > 0) {
			this.innerHTML = '»';
			this.className = 'TextIcon TCastShadow';
			this.title = 'Show panel';

			panelAnimationParams.to = -tabPanel.offsetWidth;
			panelAnimationParams.onfinish = null;

			buttonAnimationParams.to = 0;

			map.setRect(0, headerPanel.offsetHeight, window.innerWidth, window.innerHeight);
		} else {
			this.innerHTML = '«';
			this.className = 'TextIcon';
			this.title = 'Hide panel';
			
			panelAnimationParams.to = 0;
			panelAnimationParams.onfinish = adjusMapRect;
		
			buttonAnimationParams.to = tabPanel.offsetWidth - this.offsetWidth - 3;
		}

		this.fxRun();
		tabPanel.fxRun();
	}
	$fx(button).fxAdd(buttonAnimationParams);
	
	function showEntityGroup(className, entities, visible) {
		// Hides the info box if the selected entity is in this group.
		var e = map.getSelectedEntity();
		if (e && e.firstChild.className == className)
			map.selectEntity(null);
		
		var v = visible ? SVisible : SHidden;
		var i = className == 'TTag' ? 1 : 2;
		tabControl.setTabVisible(i, visible);
		for (var i = 0; i < entities.length; i++)
			entities[i].style.visibility = v;				
	}
	
	
	$('zoomOutButton').onclick = function() {
		map.zoom(null, 0.5);
	}
	$('zoomFitButton').onclick = function() {
		map.zoom(null, 0);
	}
	$('zoomInButton').onclick = function() {
		map.zoom(null, 2);
	}

	$('loginButton').onclick = login;
	
	$('showAssetsCheckBox').onchange = function () {
		showEntityGroup('TTag', tags, this.checked);
	}
	
	$('showDetectorsCheckBox').onchange = function () {
		showEntityGroup('TDetector', detectors, this.checked);
	}

	onresize();
	
	if (getCookie('username'))
		updateUI(true);
}

function changeMapImage(response) {
	if (response)
		if (response.status == 0) {
			// Forces the map image to be reloaded.
			mapImage = new Image();
			mapImage.src = 'images/' + MapFileName + '?' + (new Date()).getTime();
			mapImage.onload = function() {
				map.setMapImage(this, response.data);
			}
		} else
			showMessage(mtError, response.data, true);
}