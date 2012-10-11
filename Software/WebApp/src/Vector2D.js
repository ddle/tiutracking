/**
 *	TVector2D
 *
 *	@author	Man Hoang
 *	@version	1.0
 */
function TVector2D() {
	this.x = 0;
	this.y = 0;
}

TVector2D.prototype = {
	assign: function (other) {
		this.x = other.x;
		this.y = other.y;
	},
	
	multAddSet: function (v1, v2, s) {
		this.x = v1.x + v2.x * s;
		this.y = v1.y + v2.y * s;
	},
	
	multSubSet: function (v1, v2, s) {
		this.x = v1.x - v2.x * s;
		this.y = v1.y - v2.y * s;
	},
	
	equals: function (other, epsilon) {
		return	isZero(this.x - other.x, epsilon) &&
				isZero(this.y - other.y, epsilon);
	},
	
	length: function () {
		return Math.sqrt(this.x*this.x + this.y*this.y);
	},

	squaredLength: function () {
		return this.x*this.x + this.y*this.y;
	},

	distanceTo: function (other) {
		var dx = this.x - other.x,
			dy = this.y - other.y;
		return Math.sqrt(dx*dx + dy*dy);
	},

	lerp: function (v1, v2, factor) {
		this.x = v1.x + factor * (v2.x - v1.x);
		this.y = v1.y + factor * (v2.y - v1.y);
	},
	
	add: function (other) {
		this.x += other.x;
		this.y += other.y;
	},
	
	sub: function (other) {
		this.x -= other.x;
		this.y -= other.y;
	},
	
	mult: function (c) {
		this.x *= c;
		this.y *= c;
	},
	
	dotProduct: function (other) {
		return this.x * other.x + this.y * other.y;
	},
	
	toString: function () {
		return '(X: ' + this.x.toFixed(1) + ', Y: ' + this.y.toFixed(1) + ')';
	}
};

/* Global constants */

var ZeroVector2D = new TVector2D();
