/**
 *	$fx() animation library.
 *
 *	@source	http://fx.inetcat.com
 *	@modify	Man Hoang
 */
function $fx(elementRefOrIdStr) {
	
	if (elementRefOrIdStr.nodeType && elementRefOrIdStr.nodeType == 1)
		var elm = elementRefOrIdStr;
	else if (String(elementRefOrIdStr).match(/^#([^$]+)$/i)) {
		var elm = document.getElementById(RegExp.$1);
		if (!elm)
			return null;
	} else 
		return null;
	
	if (typeof(elm.fx) != 'undefined' && elm.fx)
		return elm;
	
	elm.fxVersion = 0.1;
	elm.fx = {};
	elm.fx.sets = [];
	elm.fx.currentSetIndex = 0;
	
	var units = {
		'left|top|right|bottom|width|height|margin|padding|spacing|backgroundx|backgroundy': 'px',
		'font': 'pt',
		'opacity': ''
	};
	
	var requiredParams = {delay: 100, step: 5, unit: ''};
	
	var specialHandlers = {
		opacity: function (value, unit) {
			if (value != null) {
				value = Math.min(100, Math.max(0, value));
				elm.style.opacity = value * 0.01;
			} else
				return Math.round(parseFloat(elm.style.opacity) * 100);
		},
		
		backgroundx: function (value, unit) {
			var x = 0, y = 0;
			var matches = (new RegExp('^(-?\\d+)[^\\d\\-]+(-?\\d+)')).exec(elm.style.backgroundPosition);
			if (matches) {
				x = parseInt(matches[1]);
				y = parseInt(matches[2]);
			}
			if (value != null)
				elm.style.backgroundPosition = value + unit + ' ' + y + unit;
			else
				return x;
		},
		
		backgroundy: function (value, unit) {
			var x = 0, y = 0, matches = (new RegExp('^(-?\\d+)[^\\d\\-]+(-?\\d+)')).exec(elm.style.backgroundPosition);
			if (matches) {
				x = parseInt(matches[1]);
				y = parseInt(matches[2]);
			}
			if (value != null)
				elm.style.backgroundPosition = x + unit + ' ' + value + unit;  
			else
				return y;
		}
	}

	elm.fxAddSet = function () {
		var currSetIndex = this.fx.sets.length;
		this.fx.currentSetIndex = currSetIndex;
		this.fx.sets[currSetIndex] = {
			loopCount: 1,
			loopsDone: 0,
			effects: [],
			effectsDone: 0,
			holdTime: 0,
			isRunning: false
		}
		return this;
	}
	
	elm.fxHold = function (time, setIndex) {
		if (!elm.fx.sets[this.fx.currentSetIndex].isRunning)
			this.fx.sets[isNaN(setIndex) ? this.fx.currentSetIndex : setIndex].holdTime = time;
		return this; 
	}
	
	elm.fxAdd = function (params) {
		var set = this.fx.sets[this.fx.currentSetIndex];
		if (set.isRunning)
			return this;
		
		for (var p in requiredParams) {
			if (!params[p])
				params[p] = requiredParams[p]
		}
		
		if (!params.unit) {
			for (var mask in units)
				if ((new RegExp(mask, 'i').test(params.type))) {
					params.unit = units[mask];
					break;
				}
		};
		
		if (!this.fx[params.type]) {
			if (specialHandlers[params.type])
				this.fx[params.type] = specialHandlers[params.type];
			else
				this.fx[params.type] = function (value, unit) {
					if (value != null)
						elm.style[params.type] = value + unit;
					else
						return parseInt(elm.style[params.type]);
				}
		}

		if (params.from == null)
			params.from = this.fx[params.type](null, null);
		params.initial = params.from;
		set.effects.push(params);
		return this;
	}
	
	elm.fxRun = function (finalCallback, loops, loopCallback) {
		var set = elm.fx.sets[elm.fx.currentSetIndex];		
		
		if (set.isRunning) {
			return this;
		}
		
		setTimeout(function () {

			if (set.isRunning)
				return elm;
			
			set.isRunning = true;
			
			if (set.effectsDone > 0)
				return elm;

			if(!isNaN(loops))
				set.loopCount = loops;
			 		
			for (var i = 0, effect; i < set.effects.length; i++) {
				effect = set.effects[i];
				if (effect.onstart)
					effect.onstart.call(elm);
				elm.fx.animate(elm.fx.currentSetIndex, i);
			}
		}, set.holdTime);
		
		return this;
	}
	
	elm.fxStop = function (setNum) {
		this.fx.sets[!isNaN(setNum) ? setNum : this.fx.currentSetIndex].isRunning = false;
		return this;
	}
	
	elm.fxReset = function () {
		var sets = this.fx.sets;
		for (var i = 0; i < sets.length; i++) {
			for (var j = 0; j < sets[i].effects.length; j++) {
				var params = sets[i].effects[j];
				this.fx[params.type](params.initial, params.unit);
			}
		}
		var del = ['fx','fxHold','fxAdd','fxAddSet','fxRun','fxPause','fxStop','fxReset'];
		for (var i = 0; i < del.length; i++)
			try {
				delete this[del[i]];
			} catch (e) {
				this[del[i]] = null;
			}
	}
	
	elm.fx.animate = function (setIndex, effectIndex) {
		var set = this.sets[setIndex];
		if(!set || !set.isRunning)
			return;
		
		var effect = set.effects[effectIndex],
			param = this[effect.type](null, null),
			step = Math.abs(effect.step),
			h = param + step,
			l = param - step;
		
		if ((h < effect.to) || (l > effect.to)) {
			if (h < effect.to)
				this[effect.type](h, effect.unit);
			else
				this[effect.type](l, effect.unit);
			var self = this;
			setTimeout(function () {if (self.animate) self.animate(setIndex, effectIndex)}, effect.delay);
		} else {
			this[effect.type](effect.to, effect.unit);
			set.effectsDone++;
			if (effect.onfinish != null)
				effect.onfinish.call(elm);
			
			if (set.effects.length == set.effectsDone) {
				set.effectsDone = 0;
				set.loopsDone++;
				if (set.onloop != null)
					set.onloop.call(elm, set.loopsDone);
				if (set.loopCount == -1 || set.loopsDone < set.loopCount) {
					for (var i = 0; i < set.effects.length; i++) {
						this[effect.type](effect.from, set.effects[i].unit);
						set.effects[i].onstart.call(elm, set.loopsDone);
						this.animate(setIndex, i);
					}
				} else {
					if (set.onfinal != null)
						set.onfinal.call(elm);
					set.isRunning = false;
				}
			}
		}
	}
	
	elm.fxAddSet();
	
	return elm;
}
