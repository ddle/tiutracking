/**
 *	@class	TTabPanel	The tab panel itself.
 *	@class	TTabNamePanel	The name panel.
 *	@class	TTabName	The name of a normal tab.
 *	@class	TSelectTabName	The name of the selected tab.
 *	@class	TTabContentPanel	The content of a tab.
 *	@author	Man Hoang
 *	@version	1.0
 */
function TTabPanel() {

/* private */

	var TTabName = 'TTabName';
	
	var self = this,
		fTabCount = 0,
		fSelectedIndex = -1,
		fElement = newElement('div', 'TTabPanel'),
		fNamePanel = newElement('ul', 'TTabNamePanel'),
		fContentPanel = newElement('div', 'TTabContentPanel');
	
	fElement.appendChild(fNamePanel);
	fElement.appendChild(fContentPanel);
	
	function tabNameClick(event) {
		console.log(this.__idx);
		self.selectTab(this.__idx);
		event.preventDefault();
	}
	
/* public */

	this.getElement = function () {
		return fElement;
	}
	
	/**
	 *	Adds a new tab page.
	 *
	 *	@param	name	{HTMLElement or String}	Name of the tab page.
	 *	@param	content	{HTMLElement or String}	Content of the tab page.
	 *	@return	Index of the added tab page.
	 */
	this.add = function (name, content) {
		var nameNode = newElement('li', TTabName);
		if (name instanceof HTMLElement)
			nameNode.appendChild(name);
		else
			nameNode.innerHTML = name;
		nameNode.__idx = fTabCount;
		nameNode.onclick = tabNameClick;
		console.log(nameNode.onclick);
		fNamePanel.appendChild(nameNode);
		
		if (content instanceof HTMLElement)
			fContentPanel.appendChild(content);
		else {
			var contentNode = newElement('div', 'TTabContent');
			contentNode.style.display = SNone;
			contentNode.innerHTML = content;
			fContentPanel.appendChild(contentNode);
		}
		
		fTabCount++;
		console.log('Tab Count = ' + fTabCount);
		return fTabCount - 1;
	}
	
	/**
	 *	Removes a tab page.
	 *
	 *	@param	index	{Integer}	Index of the tag page to be removed.
	 */
	this.remove = function (index) {
		if (isInRange(index, 0, fTabCount - 1)) {
			fNamePanel.removedChild(fNamePanel.childNodes[index]);
			fContentPanel.removedChild(fContentPanel.childNodes[index]);
			fTabCount--;
		} else
			throw  SIndexOutOfRange + index;
	}
	
	/**
	 *	Selects a tab page.
	 *
	 *	@param	index {Integer}	Index of the tab page to be selected.
	 */
	this.selectTab = function (index) {
		if (isInRange(index, 0, fTabCount - 1)) {
			if (fSelectedIndex != index) {
				if (fSelectedIndex > -1) {
					fNamePanel.childNodes[fSelectedIndex].className = TTabName;
					fContentPanel.childNodes[fSelectedIndex].style.display = SNone;
				}
				fNamePanel.childNodes[index].className = 'TSelectedTabName';
				fContentPanel.childNodes[index].style.display = SBlock;
				
				fSelectedIndex = index;
				
				if (self.onChanged)
					self.onChanged();
			}
		} else
			throw SIndexOutOfRange + index;
	}
	
	/**
	 *	Returns the index of currently selected tab page.
	 */
	this.getSelectedIndex = function () {
		return fSelectedIndex;
	}
	
	this.getTabCount = function () {
		return fTabCount;
	}
	
	/**
	 *	Event onChanged()
	 *
	 *	Happens when another tag page is selected.
	 */
	 this.onChanged = null;
}