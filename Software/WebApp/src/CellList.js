
/**
 *	TCellList
 *
 *	@class	TCellList	The list itself.
 *	@class	TListItem	The items in the list.
 *	@class	TFocusedItem	The currently focused item.
 *	@class	TSelectedItem	The currently selected item.
 *	@class	TMatchedString	The string in any item that matches a given string.
 *	@author	Man Hoang
 *	@version	1.0
 */
function TCellList() {

/* private common */
	var self = this;
	var element = newElement('div', 'TCellList');

/* private */
	var TListItem = 'TListItem';
	var TFocusedItem = 'TFocusedItem';
	var TSelectedItem = 'TSelectedItem';
	var TMatchedString = 'TMatchedString';
	
	var items;				// An array of items contained in this list, the items must have a toString() function
	var matchString = '';	// If an item whose propertyName contains matchString, the item will be
							// displayed. Setting this to null will cause all items to be displayed
	var selectedIndex = -1;	// Index of the selected item
	var focusedIndex = -1;
	var attachedTextBox = null;	// HTMLInputElement box associated with this list.
	var isNavigationKey = false;	// True if key = up, down, enter, tab
	
/* public */
	this.autoHide = false;
	this.caseSensitive = false;
	this.showIndex = false;
	this.clearTextBoxOnExit = false;
	this.maxDisplayedItemCount = 5;
	
	/**	onChanged()
	 *	This event happens when another item is selected
	 *	Implement this handler to capture the event
	 */
	this.onChanged = null;

	/**
	 *	onHoverChanged()
	 *	Happens when the temporary selected item has been changed.
	 */
	this.onHoverChanged = null;
	
	this.getElement = function () {
		return element;
	}
	
	this.getSelectedIndex = function () {
		return selectedIndex;
	}
	
	this.getFocusedIndex = function () {
		return focusedIndex;
	}

	this.getItems = function () {
		return items;
	}
	
	/**
	 *	
	 *	@param	aItems	{Array}	An array of items to be displayed
	 *
	 *	@param	reverse	{Boolean}	If true, the order of the items is reversed
	 */
	this.setItems = function (aItems, reverse) {
		if (items != aItems) {
			items = aItems;
			
			// Rebuild the list
			var s = '';
			var high = -1;
			if (items) {
				high = items.length - 1;
				if (reverse) {
					for (var i = high; i >= 0; i--)
						s += newItemString(high - i, items[i].toString());
				} else {
					for (var i = 0; i <= high; i++)
						s += newItemString(i, items[i].toString());
				}
			}
			element.innerHTML = s;
			for (var i = high; i >= 0; i--) {
				s = element.childNodes[i];
				s.onclick = itemClick;
				s.onmouseout = itemMouseOut;
				s.onmouseover = itemMouseOver;
			}
			
			if (self.maxDisplayedItemCount > 0)
				element.style.height = s.offsetHeight * self.maxDisplayedItemCount + SPixel;
			else
				element.style.height = SAuto;
		}
	}
	
	function newItemString(idx, text) {
		var idxElement = '';
		if (self.showIndex)
			idxElement = '<div class="TListItemIndex">' + (idx + 1) + '</div>';
		return '<div id="' + idx + '" class="TListItem">' + idxElement + '<div class="TListItemText">' + text + '</div></div>';
	}
	
	this.attachTextBox = function (textBox) {
		if (attachedTextBox != textBox) {
			if (attachedTextBox) {
				attachedTextBox.removeEventListener(SFocus, show, false);
				attachedTextBox.removeEventListener(SKeyDown, textBoxKeyDown, false);
				attachedTextBox.removeEventListener(SKeyUp, textBoxKeyUp, false);
				document.addEventListener(SClick, documentClick, false);
			}
			if (textBox) {
				textBox.addEventListener(SFocus, show, false);
				textBox.addEventListener(SKeyDown, textBoxKeyDown, false);
				textBox.addEventListener(SKeyUp, textBoxKeyUp, false);
				document.addEventListener(SClick, documentClick, false);
				//TODO: Remove hard code
				element.style.width = textBox.offsetWidth - 10 + SPixel;
			}
			attachedTextBox = textBox;
		}
	}
	
	/**
	 *	Sets the match string. The list will be automatically invalidated
	 *	if the new value is different from the current value.
	 *
	 *	@param	value	{String}	The new match string.
	 */
	this.setMatchString = function (value) {
		if (matchString != value) {
			matchString = value;
			self.invalidate();
		}
	}

	/**
	 *	Scrolls the list to show an item if it is invisible.
	 *
	 *	@param	item	{HTMLElement}	The item to be shown.
	 */
	function showItem(item) {
		var t = item.offsetTop - element.clientTop;
		if (t < element.scrollTop)
			element.scrollTop = t;
		else {
			t += item.offsetHeight - element.clientHeight;
			if (t > element.scrollTop)
				element.scrollTop = t;
		}
	}
		
	this.setSelectedIndex = function (value) {
		if (selectedIndex != value) {
			if (selectedIndex > -1)
				element.childNodes[selectedIndex].className = TListItem;
			
			if (value > -1) {
				var item = element.childNodes[value];
				item.className = TSelectedItem;
				showItem(item);
			}
			
			selectedIndex = value;
			
			if (self.onChanged)
				self.onChanged();
		}
	}

	this.setFocusedIndex = function (value) {
		if (focusedIndex != value) {
			if (focusedIndex > -1) {
				var item = element.childNodes[focusedIndex];
				if (item.id != selectedIndex)
					item.className = TListItem;
				else
					item.className = TSelectedItem;
			}
			
			if (value > -1) {
				var item = element.childNodes[value];
				if (item.id != selectedIndex)
					item.className = TFocusedItem;
				showItem(item);
			}
			
			focusedIndex = value;
			if (self.onHoverChanged) {
				self.onHoverChanged();
			}
		}
	}
	
	// Refresh the list currently displayed
	this.invalidate = function () {
		self.setFocusedIndex(-1);
		
		var showAll = (matchString == null) || (matchString == '');
		var tagBegin = '<span class="' + TMatchedString + '">';
		var tagEnd = '</span>';
		
		var anyMatched = false;
		var modifier = self.caseSensitive ? '' : 'i';
		var regExp = new RegExp(matchString, modifier);
		var matches;
		
		var idx = 0;
		if (self.showIndex)
			idx = 1;
		
		for (var i = element.childNodes.length - 1; i >= 0; i--) {
			var text = items[i].toString();
			var node = element.childNodes[i];

			var textNode = node.childNodes[idx];
			textNode.innerHTML = text;
			
			if (showAll == false) {
				matches = text.match(regExp);
				if (matches) {
					textNode.innerHTML = text.replace(matches[0], tagBegin + matches[0] + tagEnd);
					node.style.display = SBlock;
					anyMatched = true;
				} else {
					node.style.display = SNone;
				}
			} else {
				node.style.display = SBlock;
				anyMatched = true;
			}
		}
		
		element.scrollTop = 0;
		if (!anyMatched && self.autoHide)
			element.style.visibility = SHidden;
	}
	
	/**
	 *	Focus the next displayed item.
	 *
	 *	@param	direction	> 0 means down, else up.
	 */
	this.focusNextItem = function (direction) {
		var nodes = element.childNodes;
		
		var i = -1;
		if (focusedIndex >= 0 && nodes[focusedIndex].style.display != SNone)
			i = focusedIndex;
							
		if (direction > 0) {
			do {
				i++;
			} while ((i < nodes.length) && (nodes[i].style.display == SNone));
			if (i == nodes.length)
				i = -1;
		} else {
			if (i == -1)
				i = nodes.length;
			do {
				i--;
			} while ((i > -1) && (nodes[i].style.display == SNone));
		}

		self.setFocusedIndex(i);
	}				
	
	function itemClick(event) {
		self.setSelectedIndex(this.id);
		event.preventDefault();
	}
	
	function itemMouseOut(event) {
		self.setFocusedIndex(-1);
		event.preventDefault();
	}
	
	function itemMouseOver(event) {
		self.setFocusedIndex(this.id);
		event.preventDefault();
	}
	
	function show() {
		element.style.visibility = SVisible;
	}
	
	function hide() {
		if (attachedTextBox) {
			if (self.clearTextBoxOnExit)
				attachedTextBox.value = '';
			else if (selectedIndex > -1)
				attachedTextBox.value = items[selectedIndex].toString();
		}
		if (self.autoHide)
			element.style.visibility = SHidden;
	}
	
	// Printable Keys: 32..126
	function textBoxKeyDown(event) {
		show();
		isNavigationKey = false;
		switch (event.keyCode) {
			case 13:	/* Enter */
				self.setSelectedIndex(focusedIndex);
				hide();
				break;
			case 38:	/* Up   */
			case 40:	/* Down */
				self.focusNextItem(event.keyCode - 39);
				if (focusedIndex > -1)
					this.value = items[focusedIndex].toString();
				else
					this.value = matchString;
				break;
			case 9:		/* Tab */
			case 27:	/* Esc */
				hide();
				break;
			default:
				self.setMatchString(this.value);
				return;
		}
		isNavigationKey = true;
	}
	
	function textBoxKeyUp(event) {
		if (!isNavigationKey)
			self.setMatchString(this.value);
	}
	
	function documentClick(event) {
		if (event.target == attachedTextBox) {
			self.setMatchString(attachedTextBox.value);
			show();
		} else if (event.target == element)
			show();
		else
			hide();
	}
}