/* Global constants */

// Http request methods
var SPost		= 'POST',
	SGet		= 'GET',

	SDiv		= 'div',

	SUndefined	= 'undefined',
	SIndexOutOfRange = 'Index out of range: ',

	SAuto		= 'auto',
	SPixel		= 'px',

	SNone		= 'none',
	SBlock		= 'block',
	SInline		= 'inline',

	SHidden		= 'hidden',
	SVisible	= 'visible',

	SFocus		= 'focus',
	SBlur		= 'blur',
	SKeyDown	= 'keydown',
	SKeyUp		= 'keyup',

	SClick		= 'click',
	SMouseDown	= 'mousedown',
	SMouseMove	= 'mousemove',
	SMouseOut	= 'mouseout',
	SMouseOver	= 'mouseover',
	SMouseUp	= 'mouseup',
	SMouseWheel	= 'mousewheel';

/* TComponentState */

var csLoading	= 0,
	csLoaded	= 1;

/* Browser info */

var browser = {
	isIE:		navigator.userAgent.indexOf('MSIE') > -1,
	isChrome:	navigator.userAgent.indexOf('Chrome') > -1,
	isFirefox:	navigator.userAgent.indexOf('Firefox') > -1
};

/* Global functions */

function newElement(tagName, className) {
	var result = document.createElement(tagName);
	if (result)
		result.className = className;
	return result;
}

/** A nice alias for document.getElementById() */
function $(idStr) {
	return document.getElementById(idStr);
}

function isZero(x, epsilon) {
	return Math.abs(x) < epsilon;
}

function isInRange(x, min, max) {
	return (x >= min && x <= max);
}

function checkRange(x, min, max) {
	if (!isInRange(x, min, max))
		throw SIndexOutOfRange + x;
}

function ensureRange(x, min, max) {
	if (x < min) return min;
	if (x > max) return max;
	return x;
}
