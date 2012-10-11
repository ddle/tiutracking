/**
 *	@author	Man Hoang
 *	@version	1.0
 */
 
 /* Class Name Constants */
var cnTabName			= 'TTabName',
	cnTabNameSelected	= 'TTabName Selected',
	cnTabContent		= 'TTabContent';

function TTabControl(container) {

/* private */
	
	if (!container)
		return;
	
	var self = this;
	
	// Get all elements within the container.
	var elements = container.getElementsByTagName('*');
	
	// Get TTabName elements.
	var tabNames = extractElements(elements, cnTabName);
	if (tabNames.length == 0)
		return;
	
	// Get TTabContent elements.
	var tabContents = extractElements(elements, cnTabContent);
	if ((tabContents.length == 0) || (tabContents.length != tabNames.length))
		return;
	
	var selectedIndex = 0;

	// Initialize the tabs.
	for (var i = 0; i < tabNames.length; i++) {
		tabNames[i].__idx = i;
		tabNames[i].className = cnTabName;
		tabNames[i].onclick = tabNameClick;
		tabContents[i].style.display = SNone;
	}
	
	// Select tab 0 by default.
	tabNames[0].className = cnTabNameSelected;
	tabContents[0].style.display = SBlock;
	
	/**
	 *	Extracts elements whose class names match a given class name.
	 *
	 *	@param	elements	{Array of HTMLElement}	An array of elements.
	 *	@param	className	{String}	Class name of the elements to be extracted.
	 *	@return	A non-null array of matched elements.
	 */
	function extractElements(elements, className) {
		var result = [],
			pattern = new RegExp('\\b' + className + '\\b');
		if (elements) {
			for (var i = 0; i < elements.length; i++)
				if (elements[i].className.search(pattern) > -1)
					result.push(elements[i]);
		}
		return result;
	}
	
	/**
	 *	Click event handler for a tab name element.
	 */
	function tabNameClick(event) {
		self.selectTab(this.__idx);
		event.preventDefault();
	}
	
/* public */

	/**
	 *	Selects a tab page.
	 *
	 *	@param	index {Integer}	Index of the tab page to be selected.
	 */
	this.selectTab = function (index) {
		checkRange(index, 0, tabNames.length - 1);
		if (selectedIndex != index) {
			tabNames[selectedIndex].className = cnTabName;
			tabContents[selectedIndex].style.display = SNone;
			
			tabNames[index].className = cnTabNameSelected;
			tabNames[index].style.display = SInline;
			tabContents[index].style.display = SBlock;
			
			selectedIndex = index;
		}
	}
	
	/**
	 *	Sets the visibility of a tab.
	 *
	 *	If the tab is the selected tab or the number of tabs is
	 *	less than 2, nothing happens.
	 *
	 *	@param	index	{Integer}	Index of the tab whose visibility is to be changed.
	 *	@param	value	{Boolean}	True means visible, else invisible.
	 */
	this.setTabVisible = function (index, value) {
		checkRange(index, 0, tabNames.length - 1);
		if ((selectedIndex != index) && (tabNames.length > 1))
			tabNames[index].style.display = value ? SInline : SNone;
	}
	
	/**
	 *	Returns the index of currently selected tab page.
	 */
	this.getSelectedIndex = function () {
		return selectedIndex;
	}
}