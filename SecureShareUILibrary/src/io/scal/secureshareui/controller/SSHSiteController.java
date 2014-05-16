package io.scal.secureshareui.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import io.scal.secureshareui.login.SSHLoginActivity;
import io.scal.secureshareui.model.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class SSHSiteController extends SiteController {
    private static final String TAG = "SSHSiteController";
    public static final String SITE_NAME = "Private Server (SSH)"; 
    public static final String SITE_KEY = "ssh"; 

    public SSHSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void startAuthentication(Account account) {
        Intent intent = new Intent(mContext, SSHLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service
    }

    @Override
    public void upload(String title, String body, String mediaPath, Account account) {
        String host = null;
        String remotePath = null;
        JSONObject obj = null;
        try {
            obj = new JSONObject(account.getData());
            host = obj.getString(SSHLoginActivity.DATA_KEY_SERVER_URL);
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        jobFailed(267321323, "No Hostname or IP Address specified for SSH server");
        
        try {
            if (obj != null) remotePath = obj.getString(SSHLoginActivity.DATA_KEY_REMOTE_PATH);
        } catch (JSONException e) {
            // ignore this, its optional
        }
        
        String[] chunks = mediaPath.split("/");
        String fileName = chunks[chunks.length-1];
        String remoteFile = null;
        if ((remotePath != null) && (!remotePath.isEmpty())) {
            // FIXME check for trailing /
            remoteFile = remotePath + "/" + fileName;
        } else {
            remoteFile = fileName;
        }
        
        if (SSH.scpTo(mediaPath, account.getUserName(), account.getCredentials(), host, remoteFile, this)) {
            String result = account.getUserName() + "@" + host + ":" + remoteFile;
            jobSucceeded(result);
        } else {
            jobFailed(2767234, "SSH upload failed.");
        }
    }

    public static class SSH {
        public static boolean checkCredentials(String username, String password) {
            return true; // FIXME implement SSH credentials check
        }
//        public static boolean scpTo(String filePath, String target, final String password, SiteController controller) {
        public static boolean scpTo(String filePath, String username, final String password, String host, String remoteFile, SiteController controller) {
//            if (arg.length != 2) {
//                System.err.println("usage: java ScpTo file1 user@remotehost:file2");
//                System.exit(-1);
//            }

            FileInputStream fis = null;
            try {

//                String user = target.substring(0, target.indexOf('@'));
//                target = target.substring(target.indexOf('@') + 1);
//                String host = target.substring(0, target.indexOf(':'));
//                String remoteFile = target.substring(target.indexOf(':') + 1);

                JSch jsch = new JSch();
                Session session = jsch.getSession(username, host, 22);
                session.setConfig("StrictHostKeyChecking", "no"); // FIXME disabling host ssh checking for now  

                // username and password will be given via UserInfo interface.
                UserInfo ui = new UserInfo() {

                    @Override
                    public String getPassphrase() {
                        // TODO Auto-generated method stub
                        return password;
                    }

                    @Override
                    public String getPassword() {
                        // TODO Auto-generated method stub
                        return password;
                    }

                    @Override
                    public boolean promptPassphrase(String arg0) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public boolean promptPassword(String arg0) {
                        // TODO Auto-generated method stub
                        return true;
                    }

                    @Override
                    public boolean promptYesNo(String arg0) {
                        // TODO Auto-generated method stub
                        return true;
                    }

                    @Override
                    public void showMessage(String arg0) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                }; //new MyUserInfo();
                
                session.setUserInfo(ui);
                session.connect();

                boolean ptimestamp = true;

                // exec 'scp -t rfile' remotely
                String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFile;
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);

                // get I/O streams for remote scp
                OutputStream out = channel.getOutputStream();
                InputStream in = channel.getInputStream();

                channel.connect();

                if (checkAck(in) != 0) {
                    return false; // FIXME report the error
                }

                File _lfile = new File(filePath);

                if (ptimestamp) {
                    command = "T " + (_lfile.lastModified() / 1000) + " 0";
                    // The access time should be sent here,
                    // but it is not accessible with JavaAPI ;-<
                    command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                    out.write(command.getBytes());
                    out.flush();
                    if (checkAck(in) != 0) {
                        return false; // FIXME report the error
                    }
                }

                // send "C0644 filesize filename", where filename should not
                // include '/'
                long filesize = _lfile.length();
                command = "C0644 " + filesize + " ";
                if (filePath.lastIndexOf('/') > 0) {
                    command += filePath.substring(filePath.lastIndexOf('/') + 1);
                }
                else {
                    command += filePath;
                }
                command += "\n";
                out.write(command.getBytes());
                out.flush();
                if (checkAck(in) != 0) {
                    return false; // FIXME report the error
                }

                // send a content of lfile
                fis = new FileInputStream(filePath);
                byte[] buf = new byte[1024];
                int bytesTransfered = 0;
                float progress = 0;
                while (true) {
                    int len = fis.read(buf, 0, buf.length);
                    if (len <= 0) {
                        break;
                    }
                    bytesTransfered += len;
                    progress = ((float) bytesTransfered) / ((float) _lfile.length());
                    controller.jobProgress(progress, "SSH upload in progress..."); // FIXME move to strings
                    out.write(buf, 0, len); // out.flush();
                }
                fis.close();
                fis = null;
                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();
                if (checkAck(in) != 0) {
                    return false; // FIXME report the error
                }
                out.close();

                channel.disconnect();
                session.disconnect();

                return true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    if (fis != null)
                        fis.close();
                } catch (Exception ee) {
                }
            }
            return false;
        }

        static int checkAck(InputStream in) throws IOException {
            int b = in.read();
            // b may be 0 for success,
            // 1 for error,
            // 2 for fatal error,
            // -1
            if (b == 0)
                return b;
            if (b == -1)
                return b;

            if (b == 1 || b == 2) {
                StringBuffer sb = new StringBuffer();
                int c;
                do {
                    c = in.read();
                    sb.append((char) c);
                } while (c != '\n');
                if (b == 1) { // error
                    System.out.print(sb.toString());
                }
                if (b == 2) { // fatal error
                    System.out.print(sb.toString());
                }
            }
            return b;
        }

//        public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
//            public String getPassword() {
//                return passwd;
//            }
//
//            public boolean promptYesNo(String str) {
//                Object[] options = {
//                        "yes", "no"
//                };
//                int foo = JOptionPane.showOptionDialog(null,
//                        str,
//                        "Warning",
//                        JOptionPane.DEFAULT_OPTION,
//                        JOptionPane.WARNING_MESSAGE,
//                        null, options, options[0]);
//                return foo == 0;
//            }
//
//            String passwd;
//            JTextField passwordField = (JTextField) new JPasswordField(20);
//
//            public String getPassphrase() {
//                return null;
//            }
//
//            public boolean promptPassphrase(String message) {
//                return true;
//            }
//
//            public boolean promptPassword(String message) {
//                Object[] ob = {
//                    passwordField
//                };
//                int result =
//                        JOptionPane.showConfirmDialog(null, ob, message,
//                                JOptionPane.OK_CANCEL_OPTION);
//                if (result == JOptionPane.OK_OPTION) {
//                    passwd = passwordField.getText();
//                    return true;
//                }
//                else {
//                    return false;
//                }
//            }
//
//            public void showMessage(String message) {
//                JOptionPane.showMessageDialog(null, message);
//            }
//
//            final GridBagConstraints gbc =
//                    new GridBagConstraints(0, 0, 1, 1, 1, 1,
//                            GridBagConstraints.NORTHWEST,
//                            GridBagConstraints.NONE,
//                            new Insets(0, 0, 0, 0), 0, 0);
//            private Container panel;
//
//            public String[] promptKeyboardInteractive(String destination,
//                    String name,
//                    String instruction,
//                    String[] prompt,
//                    boolean[] echo) {
//                panel = new JPanel();
//                panel.setLayout(new GridBagLayout());
//
//                gbc.weightx = 1.0;
//                gbc.gridwidth = GridBagConstraints.REMAINDER;
//                gbc.gridx = 0;
//                panel.add(new JLabel(instruction), gbc);
//                gbc.gridy++;
//
//                gbc.gridwidth = GridBagConstraints.RELATIVE;
//
//                JTextField[] texts = new JTextField[prompt.length];
//                for (int i = 0; i < prompt.length; i++) {
//                    gbc.fill = GridBagConstraints.NONE;
//                    gbc.gridx = 0;
//                    gbc.weightx = 1;
//                    panel.add(new JLabel(prompt[i]), gbc);
//
//                    gbc.gridx = 1;
//                    gbc.fill = GridBagConstraints.HORIZONTAL;
//                    gbc.weighty = 1;
//                    if (echo[i]) {
//                        texts[i] = new JTextField(20);
//                    }
//                    else {
//                        texts[i] = new JPasswordField(20);
//                    }
//                    panel.add(texts[i], gbc);
//                    gbc.gridy++;
//                }
//
//                if (JOptionPane.showConfirmDialog(null, panel,
//                        destination + ": " + name,
//                        JOptionPane.OK_CANCEL_OPTION,
//                        JOptionPane.QUESTION_MESSAGE)
//                        == JOptionPane.OK_OPTION) {
//                    String[] response = new String[prompt.length];
//                    for (int i = 0; i < prompt.length; i++) {
//                        response[i] = texts[i].getText();
//                    }
//                    return response;
//                }
//                else {
//                    return null; // cancel
//                }
//            }
//        }
    }
}
