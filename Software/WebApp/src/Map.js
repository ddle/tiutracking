/**
 *	@author	Man Hoang
 *	@version	1.0
 */
function TMap() {

/* private */

/* const - IE doen't support const keyword yet. It sucks. */
	var ScaleEpsilon = 0.002;
	
	var VelocityEpsilon = 5;	// If the magnitues of both the components of the mouse velocity
								// are less than this value, the mouse is considered not moving.
	/**
		Upon left mouse up, if the mouse's velocity is not zero ((|x| > VelocityEpsilon) or (|y| > VelocityEpsilon)),
		The mouse velocity will be multiplied with this value to determine the target position of the map.
	 */
	var VelocityScale = 8;

	
	var FrameInterval		= 40;	// ms

	var PositionLerpFactor	= FrameInterval * 0.005,
		ScaleLerpFactor		= FrameInterval * 0.006,
		LerpFactorFactor	= 1.05;
	
	var InfoBoxMargin		= 40;
	
	var PanCursor = 'url(images/closedhand_8_8.cur), move';
	
	// A little tweak for Firefox
	if (browser.isFirefox) {
		VelocityScale = 2;
		PositionLerpFactor	= FrameInterval * 0.004;
		LerpFactorFactor	= 1.06;
	} else if (browser.isChrome)
		PanCursor = 'move';
	
/* fields */
	var self = this;
	var fComponentState = csLoading;
	
	var fMapImage = null;
	var fMapImageCenter = new TVector2D();
	var fPixelsPerUnitLength = 0;
	
	var fMapCenter = new TVector2D();
	
	var fMinScale = 0;			// The scale at which the scene fits perfectly inside the mapCanvas
	var fMaxScale = 0;		
	
	/**
		The current lerp factor controls how fast the map can move or scale in the current frame.
		When the animation timer is enabled, this variable is set to @PositionLerpFactor or @ScaleLerpFactor.
		Then on every frame, this variable is scaled by @LerpFactorFactor.
		Think about @PositionLerpFactor as speed and @LerpFactorFactor as acceleration.
	*/
	var fCurrentLerpFactor = PositionLerpFactor;
	
	var fMouse = {
		position: new TVector2D(),
		velocity: new TVector2D(),
		isLeftButtonDown: false
	};
	
	var fMapTransform = {
		position: new TVector2D(),
		targetPosition: new TVector2D(),
		scale: 1,						// Current scale of the scene
		targetScale: 1,					// Target scale of the scene
		totalScale: 1					// Scale * METTER_TO_PIXEL
	};
	
	// Timer used for scene animation
	var fTimer = new TTimer(FrameInterval, function () {
		updateMapTransform();
		fCurrentLerpFactor *= LerpFactorFactor;
	});

	var fEntities = [];
	var fSelectedEntity = null;
	
	// Create a map layer on top of the map image to capture events and
	// prevent user from selecting the map image.
	var fMap = document.createElement(SDiv);
	// For Firefox and IE, the background-color must explicitly be set for the map to receive events. A value of (0, 0, 0, 0) won't work :D
	fMap.setAttribute('style', 'position: absolute; z-index: 0; overflow: hidden; background-color: rgba(255, 255, 255, 0);');
	document.body.appendChild(fMap);
	
	var fContainer = document.createElement(SDiv);
	fContainer.style.position = 'absolute';
	fMap.appendChild(fContainer);

	var FadeInFadeOutAnimationParams = {
		type: 'opacity',
		unit: '',
		to: 100,
		step: 10,
		delay: 25,
		onfinish: function () {
			if (parseFloat(this.style.opacity) == 0)
				this.style.visibility = SHidden;	// Hide the box to prevent it from receiving events
		}
	};
		
	var fFocusedInfoBox = TInfoBox();
	fFocusedInfoBox.style.zIndex	= 101;
	fFocusedInfoBox.style.opacity	= 0;
	fContainer.appendChild(fFocusedInfoBox);
	$fx(fFocusedInfoBox).fxAdd(FadeInFadeOutAnimationParams);
	
	var fSelectedInfoBox = TInfoBox();
	fSelectedInfoBox.style.zIndex = 100;
	fContainer.appendChild(fSelectedInfoBox);
	
	fMapCenter.x = fMap.offsetWidth * 0.5;
	fMapCenter.y = fMap.offsetHeight * 0.5;
	
	function mapMouseDown(event) {
		if (event.button == 0) {
			fTimer.setEnabled(false);
			fMouse.isLeftButtonDown = true;
			updateMouseInfo(event, false);
			event.preventDefault();
		}
	}
	
	function mapMouseMove(event) {
		updateMouseInfo(event, true);
		
		if (fMouse.isLeftButtonDown) {
			this.style.cursor = PanCursor; //'url(images/closedhand_8_8.cur), move';
			fMapTransform.targetPosition.add(fMouse.velocity);
			fMapTransform.position.assign(fMapTransform.targetPosition);
			//self.invalidate();
			moveMap();
			event.preventDefault();
		}
	}
	
	function mapMouseUp(event) {
		if (event.button == 0) {
			this.style.cursor = 'default';
			fMouse.isLeftButtonDown = false;
			updateMouseInfo(event, false);
			
			if ( !fMouse.velocity.equals(ZeroVector2D, VelocityEpsilon) ) {
				fMapTransform.targetPosition.multAddSet(fMapTransform.position, fMouse.velocity, VelocityScale);
				fCurrentLerpFactor = PositionLerpFactor;
				fTimer.setEnabled(true);
			} else {
				self.selectEntity(null);
			
				if (self.onClick) {
					var s = 1 / fMapTransform.totalScale;
					var x = (fMouse.position.x - fMapTransform.position.x) * s;
					var y = (fMouse.position.y - fMapTransform.position.y) * s;
					self.onClick(x, y);
				}
			}
		}
	}
	
	function mapMouseWheel(event) {
		/*var delta = event.detail ? -event.detail * 0.1 : event.wheelDelta * 0.0025;
		var factor = 1 + delta;
		if (delta < 0)
			factor = 1 / (1 - delta);
		*/
		var delta = event.detail ? -event.detail : event.wheelDelta;
		var factor = 0.5;
		if (delta > 0)
			factor = 2;
		
		self.zoom(event, factor);
		event.preventDefault();
	}

	function mapDoubleClick(event) {
		self.zoom(event, 2);
		event.preventDefault();
	}
	
	/* Touch event handlers */
	
	function simulateMouse(event) {
		var touch = event.touches[0];
		event.pageX = touch.pageX;
		event.pageY = touch.pageY;
		event.button = 0;	
	}
	
	function mapTouchStart(event) {
		simulateMouse(event);
		mapMouseDown(event);
	}
	
	function mapTouchEnd(event) {
		simulateMouse(event);
		mapMouseUp(event);
	}
	
	function mapTouchMove(event) {		
		simulateMouse(event);
		mapMouseMove(event);
	}
	
	/* Entity event handlers */
	
	function entityMouseOver() {
		if (this != fSelectedEntity) {
			fFocusedInfoBox.setContent(this.getInfo());
			fFocusedInfoBox.setPosition(this.mX, this.mY, 0, -this.offsetHeight, fMapTransform.totalScale);
			fFocusedInfoBox.style.visibility = SVisible;
			FadeInFadeOutAnimationParams.to = 100;
			fFocusedInfoBox.fxRun();
		}
	}
	
	function entityMouseOut() {
		FadeInFadeOutAnimationParams.to = 0;
		fFocusedInfoBox.fxRun();
	}
	
	function entityClick(event) {
		self.selectEntity(this);
		// Prevent mapMouseUp from being called.
		event.stopPropagation();
	}
	
	
	function recalculateScales() {
		if (fMapImage) {
			var sw = fMap.offsetWidth  / fMapImageCenter.x;
			var sh = fMap.offsetHeight / fMapImageCenter.y;
			fMinScale = Math.min(sw, sh) * 0.4;	// 80% of the actual fit scale.
		} else
			fMinScale = 1;

		fMaxScale = 4 * fMinScale;
	}
	
	function show() {
		fMapTransform.scale = 0.001;
		fMapTransform.targetScale = fMinScale;
		fMapTransform.totalScale = fMapTransform.scale * fPixelsPerUnitLength;

		fMapTransform.position.multSubSet(fMapCenter, fMapImageCenter, fMapTransform.scale);
		fMapTransform.targetPosition.multSubSet(fMapCenter, fMapImageCenter, fMapTransform.targetScale);
		fCurrentLerpFactor = PositionLerpFactor;
		fTimer.setEnabled(true);
	}
	
	function sceneToCanvas(pos) {
		var result = new TVector2D();
		result.multAddSet(fMapTransform.position, pos, fMapTransform.totalScale);
		return result;
	}

	function updateMouseInfo(event, updateVelocity) {
		if (event) {
			// Mouse position w.r.t. the map.
			var x  = event.pageX - fMap.offsetLeft;
			var y  = event.pageY - fMap.offsetTop;
			if (updateVelocity) {
				fMouse.velocity.x = x - fMouse.position.x;
				fMouse.velocity.y = y - fMouse.position.y;
			}
			fMouse.position.x = x;
			fMouse.position.y = y;
		} else {
			fMouse.position.assign(fMapCenter);
		}
	}
	
	function updateMapTransform() {
		var animationDone = true;
		if ( !fMapTransform.position.equals(fMapTransform.targetPosition, 1) ) {
			animationDone = false;
			fMapTransform.position.lerp(fMapTransform.position, fMapTransform.targetPosition, fCurrentLerpFactor);
			moveMap();
		}
		
		var ds = fMapTransform.targetScale - fMapTransform.scale;
		if ( !isZero(ds, ScaleEpsilon) ) {
			animationDone = false;
			fMapTransform.scale += ds * fCurrentLerpFactor;
			fMapTransform.totalScale = fMapTransform.scale * fPixelsPerUnitLength;
			scaleMap();
			fFocusedInfoBox.onScaleChange(fMapTransform.totalScale);
			fSelectedInfoBox.onScaleChange(fMapTransform.totalScale);
			for (var i = 0; i < fEntities.length; i++)
				fEntities[i].onScaleChange(fMapTransform.totalScale);
		}
		
		if (animationDone) {
			fTimer.setEnabled(false);
			if (fComponentState == csLoading) {
				fComponentState = csLoaded;
										
				fMap.ondblclick = mapDoubleClick;
				fMap.onmousedown = mapMouseDown;
				fMap.onmousemove = mapMouseMove;
				fMap.onmouseup = mapMouseUp;
				fMap.onmousewheel = mapMouseWheel;
				fMap.addEventListener('DOMMouseScroll', mapMouseWheel, false);	// Firefox shit
				
				if (typeof TouchEvent != SUndefined) {
					fMap.ontouchstart = mapTouchStart;
					fMap.ontouchmove = mapTouchMove;
					fMap.ontouchend = mapTouchEnd;
				}
				
				if (self.onLoad)
					self.onLoad();
			}
		}
	}
	
/* public */

	/**
	 *	Sets the map image.
	 *
	 *	@param	img	{HTMLImageElement}	The map image.
	 *	@param	pixelsPerUnitLength	{Double}	Number of pixels equivalent to 1 unit length.		
	 */
	this.setMapImage = function (img, pixelsPerUnitLength) {
		if ((fMapImage != img) && img) {
			if (fMapImage)
				fContainer.removeChild(fMapImage);
			fPixelsPerUnitLength = pixelsPerUnitLength;
			fMapImageCenter.x = img.width  * 0.5;
			fMapImageCenter.y = img.height * 0.5;
			img.style.position = 'absolute';
			img.style.boxShadow = 'rgba(0,0,0,0.3) 0 0 4px 4px';
			fContainer.insertBefore(img, fContainer.firstChild);
			fMapImage = img;
			recalculateScales();
			show();
		}
	}
	
	/**
	 *	Sets the grid.
	 *
	 *	@param	visible	{Boolean}	true = grid on, false = grid off.
	 *	@param	spacing	{Double}	Spacing between two parallel grid lines.
	 */
	this.setGrid = function (visible, spacing) {
	
	}
	
	function moveMap() {
		fContainer.style.left = fMapTransform.position.x + SPixel;
		fContainer.style.top  = fMapTransform.position.y + SPixel;
	}
	
	function scaleMap() {
		fMapImage.style.width  = 2 * fMapImageCenter.x * fMapTransform.scale + SPixel;
		fMapImage.style.height = 2 * fMapImageCenter.y * fMapTransform.scale + SPixel;
	}
	
	/**
	 *	Refreshes the map and all the entities.
	 */
	this.invalidate = function () {
		moveMap();
		if (fMapImage)
			scaleMap();
		for (var i = 0; i < fEntities.length; i++)
			fEntities[i].onScaleChange(fMapTransform.totalScale);
		if (fSelectedEntity)
			fSelectedInfoBox.setPosition(fSelectedEntity.mX , fSelectedEntity.mY, 0, -fSelectedEntity.offsetHeight, fMapTransform.totalScale);
	}

	/**
	 *	Zooms the fMapImage.
	 *
	 *	@param	event	{HTMLEvent}	An object containing layerX and layerY, i.e. mouse's position w.r.t. the screen.
	 *								null means mapCanvas's center
	 *	@param	factor	{Double}	Zoom factor compared to the current scale.
	 *								> 1 means zoom in
	 *								< 1 means zoom out
	 *								= 0 means zoom fit
	 *								< 0 means error (ignored for now)
	 */
	this.zoom = function (event, factor) {
		if (factor < 0)
			return;

		updateMouseInfo(event, false);
		if (factor > 0) {
			fMapTransform.targetScale = ensureRange(fMapTransform.scale * factor, fMinScale, fMaxScale);
			fMapTransform.targetPosition.lerp(fMouse.position, fMapTransform.position, fMapTransform.targetScale / fMapTransform.scale);
		} else {
			fMapTransform.targetScale = fMinScale;
			fMapTransform.targetPosition.multSubSet(fMouse.position, fMapImageCenter, fMapTransform.targetScale);
		}
		
		if ( !(fMapTransform.position.equals(fMapTransform.targetPosition, VelocityEpsilon) &&
			  isZero(fMapTransform.scale - fMapTransform.targetScale, ScaleEpsilon)) ) {
			fCurrentLerpFactor = ScaleLerpFactor;
			fTimer.setEnabled(true);
		}
	}

	/**
	 *	Sets the position and dimension of the bounding box (i.e. the window/the canvas) of the map.
	 *
	 *	@params	l, t, r, b	{Double}	Left, top, right, bottom in pixels.
	 */
	this.setRect = function (l, t, r, b) {
		var w = r - l;
		var h = b - t;

		fMapCenter.x = w * 0.5;
		fMapCenter.y = h * 0.5;
		
		fMapTransform.position.x -= l - fMap.offsetLeft;
		fMapTransform.position.y -= t - fMap.offsetTop;
		fMapTransform.targetPosition.assign(fMapTransform.position);
		
		fMap.style.left	= l + SPixel;
		fMap.style.top	= t + SPixel;
		fMap.style.width  = w + SPixel;
		fMap.style.height = h + SPixel;

		recalculateScales();
		
		if (fComponentState == csLoaded)
			//self.invalidate();
			moveMap();
	}

	/**
	 *	Moves an entity to the center of the map if it is out of bounds.
	 *
	 *	@param	entity	{HTMLElement.TMapEntity}	The entity to be moved.
	 */
	this.bringToCenter = function (entity) {
		if (entity && (entity.parentNode == fContainer)) {
			// Don't rely on entity.offsetLeft and entity.offsetTop
			fMapTransform.targetPosition.x = fMapCenter.x - entity.mX * fMapTransform.totalScale;
			fMapTransform.targetPosition.y = fMapCenter.y - entity.mY * fMapTransform.totalScale;
			fCurrentLerpFactor = PositionLerpFactor;
			fTimer.setEnabled(true);
		}
	}
	
	/**
	 *	Adds a new entity to the map.
	 *
	 *	@param	entity	{HTMLElement TMapEntity}	A DOM element that has mX, mY, getInfo(), and onScaleChange() attributes.
	 */
	this.addEntity = function (entity) {
		if (entity && (entity.parentNode != fContainer)) {
			entity.addEventListener(SClick, entityClick, false);
			entity.addEventListener(SMouseOut, entityMouseOut, false);
			entity.addEventListener(SMouseOver, entityMouseOver, false);
			fEntities.push(entity);
			fContainer.appendChild(entity);
			entity.onScaleChange(fMapTransform.totalScale);
		}
	}
	
	/**
	 *	Selects an entity and brings it the the center of the map if out of bounds.
	 *
	 *	@param	entity	{HTMLElement TMapEntity}	The entity to be selected or null.
	 */
	this.selectEntity = function (entity) {
		if (entity != fSelectedEntity) {
			if (entity) {
				fFocusedInfoBox.style.visibility = SHidden;
				fSelectedInfoBox.setContent(entity.getInfo());
				fSelectedInfoBox.setPosition(entity.mX , entity.mY, 0, -entity.offsetHeight, fMapTransform.totalScale);
				fSelectedInfoBox.style.visibility = SVisible;
				
				var el = entity.offsetLeft,
					et = entity.offsetTop,
					er = entity.offsetWidth + el,
					eb = entity.offsetHeight + et,
					
					// 
					bl = fSelectedInfoBox.offsetLeft,
					bt = fSelectedInfoBox.offsetTop,
					br = fSelectedInfoBox.offsetWidth + bl,
					bb = fSelectedInfoBox.offsetHeight + bt,
					
					// The rect that contains both the entity and the info box.
					l = Math.min(el, bl) + fContainer.offsetLeft,
					t = Math.min(et, bt) + fContainer.offsetTop,
					r = Math.max(er, br) + fContainer.offsetLeft,
					b = Math.max(eb, bb) + fContainer.offsetTop;

				var dx = 0, dy = 0;
				
				if (l < 0)
					dx = InfoBoxMargin - l;
				else if (r > fMap.offsetWidth)
					dx = fMap.offsetWidth - r - InfoBoxMargin;
				
				if (t < 0)
					dy = InfoBoxMargin - t;
				else if (b > fMap.offsetHeight)
					dy = fMap.offsetHeight - b - InfoBoxMargin;

				// If part of the rect is out of range, move the map so that the entire
				// rect can be seen.
				if (dx || dy) {
					fMapTransform.targetPosition.x = fMapTransform.position.x + dx;
					fMapTransform.targetPosition.y = fMapTransform.position.y + dy;
					fCurrentLerpFactor = PositionLerpFactor;
					fTimer.setEnabled(true);
				}
			} else
				fSelectedInfoBox.style.visibility = SHidden;
			fSelectedEntity = entity;
			if (self.onSelectChange)
				self.onSelectChange();
		}
	}

	function doRemoveEntity(entity) {
		entity.removeEventListener(SClick, entityClick, false);
		entity.removeEventListener(SMouseOut, entityMouseOut, false);
		entity.removeEventListener(SMouseOver, entityMouseOver, false);
		fContainer.removeChild(entity);	
	}
	
	/**
	 *	Removes an entity out of the map, given an entity index.
	 *
	 *	@param	index	{Integer}	Index of the entity to be removed.
	 *	@throws	SIndexOutOfRange
	 */
	this.deleteEntity = function (index) {
		checkRange(index, 0, fEntities.length - 1);
		doRemoveEntity(fEntities.splice(index, 1)[0]);
	}
	
	this.removeEntity = function (entity) {
		this.deleteEntity(fEntities.indexOf(entity));
	}
	
	/**
	 *	Removes all entities.
	 */
	this.removeAll = function () {
		for (var i = 0, entity; i < fEntities.length; i++)
			doRemoveEntity(entity);
		fEntities.length = 0;
	}
	
	this.getEntity = function (index) {
		checkRange(index, 0, fEntities.length - 1);
		return fEntities[index];
	}
	
	this.getEntityCount = function () {
		return fEntities.length;
	}
	
	this.getSelectedEntity = function () {
		return fSelectedEntity;
	}
	
	this.getScale = function () {
		return fMapTransform.totalScale;
	}
	
	/**
	 *	onLoad() event. Gets called after the entrance zoom-in animation.
	 */
	this.onLoad = null;
	
	/**
	 *	onSelectChange() event. Gets called when a new entity is selected.
	 */
	this.onSelectChange = null;
	
	/**
	 *	onClick(x, y) event. x, y are in logical unit, not in pixels.
	 */
	this.onClick = null;
}