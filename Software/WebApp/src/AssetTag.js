
/**
 *	Class TAssetTag
 */

var SNormal	= 'Normal';
var SLow	= 'Low!';

function TAssetTag() {
	this.tagId = 0;
	this.assetId = '';
	this.timestamp = '';
	this.x = 0;
	this.y = 0;
	this.battery = SNormal;
	this.element = newElement(SDiv, 'TAssetEntity');
	this.element.appendChild(newElement(SDiv, 'TMarkerIcon'));
}

TAssetTag.prototype = {
	draw: function(scale) {
		var el = this.element;
		el.style.left = (this.x * scale - 12) + SPixel;
		el.style.top  = (this.y * scale - el.offsetHeight) + SPixel;
	},
	
	toArray: function () {
		return [this.tagId, this.assetId, this.timestamp, this.x, this.y, this.battery];
	},
	
	toHtml: function () {
		return	'<table cellspacing="0" style="border-collapse: collapse;">' +
				'<tr><td>Tag ID</td><td>:</td><td class="TTagId">' + this.tagId + '</td></tr>' +
				'<tr><td>Asset ID</td><td>:</td><td>' + this.assetId + '</td></tr>' +
				'<tr><td>Battery</td><td>:</td><td>' + this.battery + '</td></tr></table>';
	}
};
