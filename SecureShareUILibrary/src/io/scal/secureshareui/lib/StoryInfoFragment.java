package io.scal.secureshareui.lib;

import io.scal.secureshareui.models.StoryTag;
import io.scal.secureshareuilibrary.R;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class StoryInfoFragment extends Fragment {

	private View mView;
	
	List<StoryTag> mALStoryTags = new ArrayList<StoryTag>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	
		
		mView = inflater.inflate(R.layout.story_info_fragment, container, false);
		
		getStoryInfo();
		addStoryInfo();
		
		return mView;
	}
	
	private void getStoryInfo() {
		
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
	
	private void addStoryInfo() { 
    	//ensure the fragment is attached to a context
    	if(getActivity() == null)
    		return;
    	
    	TextView tvStoryTitle = (TextView) mView.findViewById(R.id.tv_story_title);
    	TextView tvStoryDesc = (TextView) mView.findViewById(R.id.tv_story_desciption);
    	TextView tvStorySection = (TextView) mView.findViewById(R.id.tv_story_section);
    	TextView tvStoryLocation = (TextView) mView.findViewById(R.id.tv_story_location);
    	
    	tvStoryTitle.setText("Hello");
    	tvStoryDesc.setText("Description of the beautiful story.");
    	tvStorySection.setText("Politics");
    	tvStoryLocation.setText("Alabama");
    }
}