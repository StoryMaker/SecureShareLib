package io.scal.secureshareui.lib;

import io.scal.secureshareuilibrary.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class ChooseAccountFragment extends Fragment {

	public ChooseAccountFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		
		View view = inflater.inflate(R.layout.choose_account_fragment, container, false);
		
		String arList1[]={"Facebook","YouTube","SoundCloud"};
		String arList2[]={"Flickr","WordPress","GlobalLeaks", "SSH"};
		
		final ListView list1 = (ListView) view.findViewById(R.id.lv_accounts_connected);
		ListView list2 = (ListView) view.findViewById(R.id.lv_accounts_available);
		
		list1.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, arList1));
		list2.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, arList2));
		
		
		list1.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				// ListView Clicked item index
				int itemPosition = position;
				   
				// ListView Clicked item value
				String  itemValue = (String) list1.getItemAtPosition(position);
				      
				// Show Alert 
				Toast.makeText(getActivity(), "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_SHORT).show(); 
				
			}
			
		});
		
		
		
		return view;
	}

}
