package io.scal.secureshareui.lib;


import java.util.ArrayList;
import java.util.List;

import io.scal.secureshareui.model.PublishAccount;
import io.scal.secureshareui.model.StoryTag;
import io.scal.secureshareuilibrary.R;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

public class StoryInfoEditFragment extends Fragment {

	private View mView;
	private ViewGroup mContainerStoryTagsView;
	
	List<StoryTag> mALStoryTags = new ArrayList<StoryTag>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	
		
		mView = inflater.inflate(R.layout.story_info_edit_fragment, container, false);
			
		mContainerStoryTagsView = (ViewGroup) mView.findViewById(R.id.story_tag_container);
		
		initialize();
		getStoryTags();
		addStoryTags();
		
		return mView;
	}
	
	private void initialize() {
		
		final AutoCompleteTextView tvStoryTag = (AutoCompleteTextView) mView.findViewById(R.id.act_story_info_tag);
		String[] autocompleteTags = getResources().getStringArray(R.array.array_autocomplete_tags);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, autocompleteTags);
		tvStoryTag.setAdapter(adapter);
		
		Button btnAddTag = (Button) mView.findViewById(R.id.btn_add_tag);
		btnAddTag.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	
		    	String tagText = tvStoryTag.getText().toString();
		    	
		    	if (!tagText.equals("")) {
		    		
		    		StoryTag stTemp = new StoryTag(232, tagText);
		    		
		    		mALStoryTags.add(stTemp);
		    		
		    		addStoryTag(stTemp);
		    		
		    	}
		    	
		    	tvStoryTag.setText(null);
		    }
		});
		
	}
	
	private void getStoryTags() {
		
		int i = 0;
		
		mALStoryTags.add(new StoryTag(i++, "story"));
		mALStoryTags.add(new StoryTag(i++, "news"));
		mALStoryTags.add(new StoryTag(i++, "morocco"));
		mALStoryTags.add(new StoryTag(i++, "desert"));
		mALStoryTags.add(new StoryTag(i++, "market"));
		mALStoryTags.add(new StoryTag(i++, "camel"));
		mALStoryTags.add(new StoryTag(i++, "politics"));
		mALStoryTags.add(new StoryTag(i++, "arabic"));
	}
	
	private void addStoryTags() { 
    	//ensure the fragment is attached to a context
    	if(getActivity() == null)
    		return;
	
        for(StoryTag tag: mALStoryTags) {        	
        		addStoryTag(tag);
        }      
    }
	
	private void addStoryTag(StoryTag tag) {
    	
		final StoryTag currentTag = tag;
		
		Button btnTag = new Button(getActivity());
		btnTag.setText(currentTag.getName());
		btnTag.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		mContainerStoryTagsView.addView(btnTag, 0);
		
		//remove button when clicked
		btnTag.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) { 	
		    	mContainerStoryTagsView.removeView(v);
		    }
		});
    }	
}
