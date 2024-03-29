package com.purpleplatypus.transportion;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;

import ws.munday.slidingmenu.SlidingMenuActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.GestureDetector;

import android.support.v4.view.GestureDetectorCompat;

public class TransportionActivity extends SlidingMenuActivity implements GestureDetector.OnGestureListener{
	
    private static final String DEBUG_TAG = "Gestures";
    private static final int VERT_DIST_ERROR = 50;
    private static final int VELOCITY_X_THRESHOLD = 200;
    private static final int DIST_X_THRESHOLD = 200;
    private boolean isMenuOpen = false;

    private GestureDetectorCompat mDetector; 
    
	private Session.StatusCallback callback = new Session.StatusCallback() {
	    public void call(Session session, SessionState state, Exception exception) {
			System.out.println("session.statuscallback called");
	        onSessionStateChange(session, state, exception);
	    }
	};
	
	protected boolean isResumed = false;
	private UiLifecycleHelper uiHelper;
	
	public static Model m = ApplicationState.getModel();
	
	private String userIdentifier = "";
	
	private static int facebookConnectionAttempts = 5;
	
	private ArrayList<Item> sliding_menu_items;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setLayoutIds(R.layout.menu, R.layout.frame);
		setAnimationDuration(300);
		setAnimationType(MENU_TYPE_SLIDEOVER);
		super.onCreate(savedInstanceState);

		ImageButton menuButton = (ImageButton) findViewById(R.id.menu_button);
		menuButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleMenu();
			}
		});
		
		 sliding_menu_items = new ArrayList<Item>();
		 sliding_menu_items.add(new EntryItem(0, "Home", R.drawable.menu_home));
		 sliding_menu_items.add(new EntryItem(1, "Logout", R.drawable.menu_logout));
		 sliding_menu_items.add(new SectionItem(2, "Details"));
		 sliding_menu_items.add(new EntryItem(3, "Car", R.drawable.menu_car));
		 sliding_menu_items.add(new EntryItem(4, "Bus", R.drawable.menu_bus));
		 sliding_menu_items.add(new EntryItem(5, "Bike", R.drawable.menu_bike));
		 sliding_menu_items.add(new EntryItem(6, "Walk", R.drawable.menu_walk));
		 sliding_menu_items.add(new SectionItem(7, "Compare"));
		 sliding_menu_items.add(new EntryItem(8, "Friends", R.drawable.menu_friends));
		 sliding_menu_items.add(new EntryItem(9, "Leaderboard", R.drawable.menu_leaderboard));
		 
		
		ListView menuListView = (ListView) findViewById(R.id.menuListView);
		menuListView.setAdapter(new EntryAdapter(this, sliding_menu_items));
		
		menuListView.setOnItemClickListener(new OnMenuItemClickListener());
		
		//facebook stuff
		
	    uiHelper = new UiLifecycleHelper(this, callback);
	    uiHelper.onCreate(savedInstanceState);
	    
        mDetector = new GestureDetectorCompat(this, this);

	}
	
	public void setFrameView(int viewID) {
		LayoutInflater factory = LayoutInflater.from(this);
		View myView = factory.inflate(viewID, null);
		
		RelativeLayout frameView = (RelativeLayout) findViewById(R.id.frameView);
		frameView.addView(myView);
	}
	
	public class OnMenuItemClickListener implements OnItemClickListener {
		
		HashMap<String, Class> activityClasses;
		HashMap<String, ArrayList<String[]>> activityPutExtras;
		
		public OnMenuItemClickListener() {
			super();
			activityClasses = new HashMap<String, Class>();
			activityClasses.put("Home", MainActivity.class);
			activityClasses.put("Friends", FriendsActivity.class);
			activityClasses.put("Car", CarDetails.class);
			activityClasses.put("Walk", WalkDetails.class);
			activityClasses.put("Bike", BikeDetails.class);
			activityClasses.put("Bus", BusDetails.class);
			activityClasses.put("Leaderboard", LeaderboardActivity.class);
			
			activityPutExtras = new HashMap<String, ArrayList<String[]>>();
			ArrayList<String[]> putExtra = new ArrayList<String[]>();
				putExtra.add(new String[]{"Mode", "Car"});
			activityPutExtras.put("Car", putExtra);
			
			putExtra = new ArrayList<String[]>();
				putExtra.add(new String[]{"Mode", "Walk"});
			activityPutExtras.put("Walk", putExtra);
			
			putExtra = new ArrayList<String[]>();
				putExtra.add(new String[]{"Mode", "Bike"});
			activityPutExtras.put("Bike", putExtra);
			
			putExtra = new ArrayList<String[]>();
				putExtra.add(new String[]{"Mode", "Bus"});
			activityPutExtras.put("Bus", putExtra);
		}
		
		@Override
		
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) { // CHANGE THIS FOR CATEGORIES
			
			String textClicked = ((TextView) view.findViewById(R.id.list_item_entry_title)).getText().toString();		
			toggleMenu();
			if (textClicked == "Logout") {
				onClickLogout();
			} else {
				beginNewActivity(textClicked);		
			}
		}
		
		public void beginNewActivity(String activityClass){
			// Launching new Activity on selecting single List Item
			Intent i = new Intent(getApplicationContext(), getActivityClass(activityClass));
			// sending data to new activity
			ArrayList<String[]> puts = activityPutExtras.get(activityClass);
			if (puts != null) {
				for (int j = 0; j < puts.size(); j++) {
					String[] data = puts.get(j);
					i.putExtra(data[0], data[1]);
				}
			}
			// bring activity to front if one already exists
			i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
		}
		
		public Class getActivityClass(String name) {
			if (activityClasses.containsKey(name)) {
				return activityClasses.get(name);
			}
			return FriendsActivity.class;
		}
	}
	
	protected void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    System.out.println("transportion screen: session state changed to " + state.toString());
	    if (isResumed) {
            SharedPreferences savedSession = getApplicationContext().getSharedPreferences("facebook-session",Context.MODE_PRIVATE);
            String id = savedSession.getString("id", null);
            String name = savedSession.getString("name", null);
            
	        if (state.isOpened()) {
	            
	            System.out.println("transportion screen: saved facebook id is " + id);
	            System.out.println("transportion screeN: saved facebook name is " + name);
	            
	            if (id == null || name == null) {
	            	System.out.println("transportion screen: making me request for facebook id");
	            	makeMeRequest(Session.getActiveSession());
	            }
	            if (id != null && name != null) {
	            	ApplicationState.getModel().sendStats();
	            }
	        }
	        else if (id == null){
	        	System.out.println("transportion screen: id is null and session state is not open, going to login page");
	        	onClickLogout();
	        }
	    }
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
		isResumed = true;
		
        SharedPreferences savedSession = getApplicationContext().getSharedPreferences("facebook-session",Context.MODE_PRIVATE);
        System.out.println("transportion screen: saved facebook id was " + savedSession.getString("id", null));
        
        onSessionStateChange(Session.getActiveSession(), Session.getActiveSession().getState(), null);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
		isResumed = false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}
	
	@Override
	public void toggleMenu() {
		super.toggleMenu();
		Log.d(DEBUG_TAG, "menu open? " + isMenuOpen);
		isMenuOpen = !isMenuOpen;
	}
	
	
	private void onClickLogout() {
		Session session = Session.getActiveSession();
		System.out.println("onClickLogout activated with state " + session.getState().toString());
		if (!session.isClosed()) {
			session.closeAndClearTokenInformation();
		}
		
		// Stop Tracking:
		Intent intent = new Intent(this, LocationService.class);
		stopService(intent);
		
    	Editor editor = getApplicationContext().getSharedPreferences("facebook-session", Context.MODE_PRIVATE).edit();
    	editor.remove("id");
    	editor.remove("name");
		editor.commit();
    	
		Intent i = new Intent(getApplicationContext(), LoginActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(i);
	}
	
	protected void makeUIDRequest(final Session session) {
		System.out.println("making facebook graphPathRequest with access token as " + session.getAccessToken());
		new AsyncTask<Session, Void, String>() {
			public String doInBackground(Session... arg) {
				Session session = arg[0];
				HttpResponse response = null;
				try {        
				        HttpClient client = new DefaultHttpClient();
				        HttpGet request = new HttpGet();
				        request.setURI(new URI("https://graph.facebook.com/me?fields=third_party_id&access_token="+session.getAccessToken()));
				        response = client.execute(request);
				} catch (Exception e) {
				        e.printStackTrace();
				        return null;
				}
				String jsonString = null;
				try {
					jsonString = convertStreamToString(response.getEntity().getContent());
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				
				JSONObject json = null;
				String result = null;
				try {
					json = new JSONObject(jsonString);
					result = json.getString("third_party_id");
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}

				return result;
			} 
			public void onPostExecute(String thirdPartyId) {
				if (thirdPartyId == null) {
					System.out.println("third_party_id is null!");
					return;
				}
				System.out.println("third_party_id is " + thirdPartyId);
				userIdentifier = thirdPartyId;
			}
			
			public String convertStreamToString(InputStream inputStream) throws IOException {
		        if (inputStream != null) {
		            Writer writer = new StringWriter();

		            char[] buffer = new char[1024];
		            try {
		                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
		                int n;
		                while ((n = reader.read(buffer)) != -1) {
		                    writer.write(buffer, 0, n);
		                }
		            } finally {
		                inputStream.close();
		            }
		            return writer.toString();
		        } else {
		            return "";
		        }
		    }
			
		}.execute(session);
	} 


	protected void makeMeRequest(final Session session) {
		if (facebookConnectionAttempts <= 0) {
			System.out.println("exhausted all attempts, logging out");
			onClickLogout();
		}
	    // Make an API call to get user data and define a 
	    // new callback to handle the response.
	    Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
	        @Override
	        public void onCompleted(GraphUser user, Response response) {
	            // If the response is successful
	        	SharedPreferences savedSession = getApplicationContext().getSharedPreferences("facebook-session", Context.MODE_PRIVATE);
	        	
	            if (session == Session.getActiveSession()) {
	                if (user != null) {
	                	System.out.println("me request returned with user as: ");
	                	System.out.println(user.getId() + " : " + user.getName());
	                	
	                    Editor editor = savedSession.edit();
	                    editor.putString("id", user.getId());
	                    editor.putString("name", user.getName());
	                    editor.commit();
	                    
	                    ApplicationState.getModel().sendStats();
	                    
	                    facebookConnectionAttempts = 5;
	                }
	                else {
	                	System.out.println("me request returned empty user");
		                facebookConnectionAttempts = facebookConnectionAttempts - 1;
		                System.out.println("retrying me request. attempts left is " + facebookConnectionAttempts);
		                makeMeRequest(Session.getActiveSession());
	                }
	            }
	            if (response.getError() != null) {
	                System.out.println("me request failed with error " + response.getError());
	                facebookConnectionAttempts = facebookConnectionAttempts - 1;
	                System.out.println("retrying me request. attempts left is " + facebookConnectionAttempts);
	                makeMeRequest(Session.getActiveSession());
	            }
	        }
	    });
	    request.executeAsync();
	} 

	

	public class EntryAdapter extends ArrayAdapter {
		
		ArrayList<Item> items;
		Context context;
		LayoutInflater li;
		
		public EntryAdapter(Context context, ArrayList<Item> items) {
			super(context,0, items);
	        this.context = context;
	        this.items = items;
	        li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);    		
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
	        View v = convertView;
	 
	        final Item i = (Item) items.get(position);
	        if (i != null) {
	            if (i.isSection()) {
	                SectionItem si = (SectionItem)i;
	                v = li.inflate(R.layout.menu_section, null);
	 
	                v.setOnClickListener(null);
	                v.setOnLongClickListener(null);
	                v.setLongClickable(false);
	 
	                final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
	                sectionView.setText(si.name);
	            } else {
	                EntryItem ei = (EntryItem)i;
	                v = li.inflate(R.layout.menu_text, null);
	                
	                // put text
	                TextView title = (TextView)v.findViewById(R.id.list_item_entry_title);
	                if (title != null) title.setText(ei.name);
	                
	                // put icon
	                ImageView icon = (ImageView) v.findViewById(R.id.icon);
	                if (icon != null) icon.setImageResource(ei.iconId);
	            }
	        }
	        return v;
	    }
		
		

		/*
		 * 0 Home, 1 Logout, 2 Details (Section), 3 Car, 4 Bus, 5 Bike, 6 Walk, 7 Compare (Section), 8 Friends, 9 Leaderboard
		 */
		
	}
	public class SectionItem implements Item {
		int position;
		String name;
		
		public SectionItem(int position, String name) {
			this.position = position;
			this.name = name;
		}
		
		public boolean isSection() {
			if (position == 2 || position == 7) {
				return true;
			} else {
				return false;
			}
		}
	}

	public class EntryItem implements Item {
		int position;
		String name;
		int iconId;
		
		public EntryItem(int position, String name, int iconId) {
			this.position = position;
			this.name = name;
			this.iconId = iconId;
		}
		public boolean isSection() {
			if (position == 2 || position == 7) {
				return true;
			} else {
				return false;
			}
			
		}
	}
	
	public interface Item {
		boolean isSection();
	}

	// touch events
	@Override 
	public boolean onTouchEvent(MotionEvent event){ 
		this.mDetector.onTouchEvent(event);
		// Be sure to call the superclass implementation
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		if(isMenuOpen) {
			toggleMenu();
			return true;
		}
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Log.d(DEBUG_TAG, "onFling: " + e1.toString()+e2.toString());
		Log.d(DEBUG_TAG, "speed: " + "vx: " + velocityX + "vy: " + velocityY);
		
		try {
			// if sliding menu not open
			if (!isMenuOpen) {
				float diffY = e2.getY() - e1.getY();
				float diffX = e2.getX() - e1.getX();
				Log.d(DEBUG_TAG, "diff: " + "dx: " + diffX + " dy: " + diffY);
	
				//if little y movement,
				if(Math.abs(diffY) < VERT_DIST_ERROR) {
					// if slide left to right with enough distance, and with enough speed
					if(diffX > DIST_X_THRESHOLD && Math.abs(velocityX) > VELOCITY_X_THRESHOLD) {
						toggleMenu();
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
        return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
}
