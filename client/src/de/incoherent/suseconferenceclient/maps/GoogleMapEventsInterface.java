/*******************************************************************************
 * Copyright (c) 2012 Matt Barringer <matt@incoherent.de>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Matt Barringer <matt@incoherent.de> - initial API and implementation
 ******************************************************************************/

package de.incoherent.suseconferenceclient.maps;

import com.google.android.maps.GeoPoint;

/*
 * Interface used by the GoogleMapEventsOverlay and the GoogleMap.
 */
public interface GoogleMapEventsInterface {
	boolean singleTapUpHelper(GeoPoint p);
}
