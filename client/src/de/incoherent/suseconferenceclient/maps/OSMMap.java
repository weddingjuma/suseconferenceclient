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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.GEMFFileArchive;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GEMFFile;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import de.incoherent.suseconferenceclient.R;
import de.incoherent.suseconferenceclient.models.Venue;
import de.incoherent.suseconferenceclient.models.Venue.MapPoint;
import de.incoherent.suseconferenceclient.models.Venue.MapPolygon;

public class OSMMap implements MapInterface, OSMEventsInterface, OnItemGestureListener<OSMOverlayItem> {
	private Context mContext;
	private OSMMapView mMapView = null;
	private OSMMapOverlay mMapOverlay = null;
	private File mOfflineMap;
	private MyLocationOverlay mLocationOverlay;
	private ArrayList<OSMOverlayItem> mOverlays = new ArrayList<OSMOverlayItem>();
	private BoundingBoxE6 mBoundingBox = null;
	
	public OSMMap(Context context, File offlineMap) {
		mContext = context;
		mOfflineMap = offlineMap;
	}

	@Override
	public View getView() {
		return mMapView;
	}

	@Override
	public void setupMap(Venue venue) {
		if (mMapView != null)
			return;
		
		if (mOfflineMap == null) {
			mMapView = new OSMMapView(mContext);
			mMapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
		} else {
			GEMFFileArchive archive;
			int minZoom = 13;
			int maxZoom = 18;
			try {
				archive = GEMFFileArchive.getGEMFFileArchive(mOfflineMap);
				GEMFFile file = new GEMFFile(mOfflineMap);
				Set<Integer> levels = file.getZoomLevels();
				minZoom = Collections.min(levels);
				maxZoom = Collections.max(levels);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				mMapView = null;
				return;
			} catch (IOException e) {
				e.printStackTrace();
				mMapView = null;
				return;
			}
			
			IArchiveFile[] archiveFiles = new IArchiveFile[1];
			archiveFiles[0] = archive;
	        OSMBitmapTileSourceBase bitmapTileSourceBase = new OSMBitmapTileSourceBase("MapQuest", null, minZoom, maxZoom, 256, ".png");
	        MapTileModuleProviderBase[] mapTileProviders = new MapTileModuleProviderBase[1];
	        mapTileProviders[0] = new MapTileFileArchiveProvider(new SimpleRegisterReceiver(mContext),
	        													 bitmapTileSourceBase,
	        													 archiveFiles);
	        MapTileProviderArray gemfTileProvider = new MapTileProviderArray(bitmapTileSourceBase,
	        																 null,
	        																 mapTileProviders);
	        gemfTileProvider.setUseDataConnection(false);

	        TilesOverlay tilesOverlay = new TilesOverlay(gemfTileProvider, mContext);	
	        mMapView = new OSMMapView(mContext, 
											 256, new DefaultResourceProxyImpl(mContext), 
											 gemfTileProvider);
			mMapView.getOverlays().add(tilesOverlay);
		}
		
		mMapView.setBuiltInZoomControls(true);
		mMapView.setClickable(true);
		mMapView.setMultiTouchControls(true); 
		mMapView.getController().setZoom(15);
		if (venue == null)
			return;
	
		Drawable venueDrawable = mContext.getResources().getDrawable(R.drawable.venue_marker);
		Drawable foodDrawable = mContext.getResources().getDrawable(R.drawable.food_marker);
		Drawable drinkDrawable = mContext.getResources().getDrawable(R.drawable.drink_marker);
		Drawable elecDrawable = mContext.getResources().getDrawable(R.drawable.electronics_marker);
		Drawable partyDrawable = mContext.getResources().getDrawable(R.drawable.party_marker);
		Drawable hotelDrawable = mContext.getResources().getDrawable(R.drawable.hotel_marker);
		MapController controller =  mMapView.getController();
		for (MapPoint point : venue.getPoints()) {
			GeoPoint mapPoint = new GeoPoint(point.getLat(), point.getLon());
			OSMOverlayItem overlay = null;
			overlay = new OSMOverlayItem(point.getName(), point.getDescription(), point.getAddress(), mapPoint);
			
			switch (point.getType()) {
			case MapPoint.TYPE_VENUE:
				overlay.setMarker(venueDrawable);
				controller.setCenter(mapPoint);
				controller.setZoom(18);
				break;
			case MapPoint.TYPE_FOOD:
				overlay.setMarker(foodDrawable);
				break;
			case MapPoint.TYPE_DRINK:
				overlay.setMarker(drinkDrawable);
				break;
			case MapPoint.TYPE_PARTY:
				overlay.setMarker(partyDrawable);
				break;
			case MapPoint.TYPE_ELECTRONICS:
				overlay.setMarker(elecDrawable);
				break;
			case MapPoint.TYPE_HOTEL:
				overlay.setMarker(hotelDrawable);
				break;
			}

			mOverlays.add(overlay);
		}
		
		ResourceProxy proxy = new DefaultResourceProxyImpl(mContext);
		mMapOverlay = new OSMMapOverlay(mOverlays, this, proxy, mMapView);
		
        OSMEventsOverlay eventsOverlay = new OSMEventsOverlay(mContext, this);
        mMapView.getOverlays().add(eventsOverlay);
		mMapView.getOverlays().add(mMapOverlay);
		mMapOverlay.doPopulate();

		mLocationOverlay = new MyLocationOverlay(mContext, mMapView);
		mLocationOverlay.enableMyLocation();
		mLocationOverlay.enableCompass();
		mMapView.getOverlays().add(mLocationOverlay);
		if (mBoundingBox != null)
			mMapView.setScrollableAreaLimit(mBoundingBox);
		
		mMapView.postInvalidate();
	}

	public void setBoundingBox(BoundingBoxE6 box) {
		mBoundingBox = box;
	}
	
	@Override
	public void enableLocation() {
		Log.d("SUSEConferences", "OSMMap: enableLocation");
		mLocationOverlay.enableMyLocation();
		mLocationOverlay.enableCompass();
	}

	@Override
	public void disableLocation() {
		Log.d("SUSEConferences", "OSMMap: disableLocation");
		mLocationOverlay.disableMyLocation();
		mLocationOverlay.disableCompass();
	}

	@Override
	public boolean onItemSingleTapUp(int index, OSMOverlayItem item) {
		mMapOverlay.showBubbleOnItem(index, mMapView);
		return true;
	}

	@Override
	public boolean onItemLongPress(int arg0, OSMOverlayItem arg1) {
		return false;
	}

	@Override
	public boolean singleTapUpHelper(IGeoPoint p) {
		mMapOverlay.closeBubble();
		return false;
	}
}
