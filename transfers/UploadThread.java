package com.android.providers.transfers;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import android.content.Context;

import com.android.providers.transfers.StopRequestException;

public class UploadThread extends TransferThread {

	public UploadThread(Context context, SystemFacade systemFacade, TransferInfo info,
            StorageManager storageManager, TransferNotifier notifier) {
    	
    	super(context, systemFacade, info, storageManager, notifier);
    }

	@Override
	public void runInternal() {
	}
	
	@Override
    public  void executeTransfer(State state) throws StopRequestException {
    }

    @Override
    public void transferData(State state, HttpURLConnection conn) throws StopRequestException {
    }

    @Override
    public void transferData(State state, InputStream in, OutputStream out) throws StopRequestException {
    }
}
