
/**
 *	Class TTimer
 *
 *	@param	interval	{Double}	Timer interval in ms.
 *	@param	callBack	{Function}	Timer callback function.
 */
function TTimer(interval, callBack) {
	var fHandle,
		fEnabled = false,
		fOnTimer = callBack,
		fInterval = interval;
	
	/**
	 *	Sets timer interval.
	 *
	 *	@param	value	{Double}	The interval in ms.
	 */
	this.setInterval = function (value) {
		if (fInterval != value) {
			fInterval = value;
			if (fEnabled) {
				clearInterval(fHandle);
				fHandle = setInterval(fOnTimer, fInterval);
			}
		}
	}
	
	/**
	 *	Enable/disable timer.
	 *
	 *	@param	value	{Boolean}	True = enabled, False = disabled.
	 */
	this.setEnabled = function (value) {
		if (fEnabled != value) {
			if (value)
				fHandle = setInterval(fOnTimer, fInterval);
			else
				clearInterval(fHandle);
			fEnabled = value;
		}
	}
}