package org.ssssssss.magicapi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.ssssssss.script.annotation.Comment;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SshUtil {
	
	@Comment("执行linux服务器命令")
	public static void sshExecCmd(
			@Comment(name = "hostname", value = "hostname") String hostname,
			@Comment(name = "ip", value = "ip") String ip,
			@Comment(name = "sshPort", value = "sshPort") int sshPort,
			@Comment(name = "user", value = "user") String user,
			@Comment(name = "password", value = "password") String password,
			@Comment(name = "command", value = "command") String command
			
			) throws Exception {

		Host host = new Host(hostname, ip, ip, sshPort, user, password);
	    execCmd(host,command,System.out);
	}
	
	@Comment("下载linux服务器文件")
	public static void sshScpToLocal(
			@Comment(name = "hostname", value = "hostname") String hostname,
			@Comment(name = "ip", value = "ip") String ip,
			@Comment(name = "sshPort", value = "sshPort") int sshPort,
			@Comment(name = "user", value = "user") String user,
			@Comment(name = "password", value = "password") String password,
			@Comment(name = "remotePath", value = "remotePath") String remotePath,
			@Comment(name = "localPath", value = "localPath") String localPath
			
			) throws Exception {
		
		Host host = new Host(hostname, ip, ip, sshPort, user, password);
		scpToLocal(remotePath, localPath, host);
	}
	
	@Comment("上传文件到linux服务器")
	public static void sshScpToRemote(
			@Comment(name = "hostname", value = "hostname") String hostname,
			@Comment(name = "ip", value = "ip") String ip,
			@Comment(name = "sshPort", value = "sshPort") int sshPort,
			@Comment(name = "user", value = "user") String user,
			@Comment(name = "password", value = "password") String password,
			@Comment(name = "remotePath", value = "remotePath") String remotePath,
			@Comment(name = "localPath", value = "localPath") String localPath
			
			) throws Exception {
		
		Host host = new Host(hostname, ip, ip, sshPort, user, password);
		scpToRemote(localPath,remotePath, host);
	}

  private static  void scpToRemote(String localFile,String remotePath,Host host) throws Exception  {
    JSch jsch = new JSch();
    Session session = jsch.getSession(host.getUser(),host.getIp(), host.getSshPort());
    session.setPassword(host.getPassword());
    session.setConfig("StrictHostKeyChecking","no");
    session.connect();
    boolean ptimestamp =false;
    String rfile = remotePath;
    String command="scp " + (ptimestamp ? "-p" :"") +" -t "+rfile;
    Channel channel=session.openChannel("exec");
    ((ChannelExec)channel).setCommand(command);

    // get I/O streams for remote scp
    OutputStream out=channel.getOutputStream();
    InputStream in=channel.getInputStream();

    channel.connect();

    if(checkAck(in)!=0){
      System.exit(0);
    }
    String lfile = localFile;
    File _lfile = new File(lfile);

    if(ptimestamp){
      command="T "+(_lfile.lastModified()/1000)+" 0";
      // The access time should be sent here,
      // but it is not accessible with JavaAPI ;-<
      command+=(" "+(_lfile.lastModified()/1000)+" 0\n");
      out.write(command.getBytes()); out.flush();
      if(checkAck(in)!=0){
        System.exit(0);
      }
    }

    // send "C0644 filesize filename", where filename should not include '/'
    long filesize=_lfile.length();
    command="C0644 "+filesize+" ";
    if(lfile.lastIndexOf('/')>0){
      command+=lfile.substring(lfile.lastIndexOf('/')+1);
    }
    else{
      command+=lfile;
    }
    command+="\n";
    out.write(command.getBytes()); out.flush();
    if(checkAck(in)!=0){
      System.exit(0);
    }

    // send a content of lfile
    FileInputStream fis=new FileInputStream(lfile);
    byte[] buf=new byte[1024];
    while(true){
      int len=fis.read(buf, 0, buf.length);
      if(len<=0) break;
      out.write(buf, 0, len); //out.flush();
    }
    fis.close();
    fis=null;
    // send '\0'
    buf[0]=0; out.write(buf, 0, 1); out.flush();
    if(checkAck(in)!=0){
      System.exit(0);
    }
    out.close();

    channel.disconnect();
    session.disconnect();
  }

  private static void scpToLocal(String remotePath,String localPath,Host host) throws Exception {
    JSch jsch = new JSch();
    Session session = jsch.getSession(host.getUser(),host.getIp(), host.getSshPort());
    session.setPassword(host.getPassword());
    session.setConfig("StrictHostKeyChecking","no");
    session.connect();
    String cmd = "scp  -f " + remotePath;
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    channel.setCommand(cmd);
    InputStream  in = channel.getInputStream();
    OutputStream out=channel.getOutputStream();
    channel.connect();
    byte[] buf=new byte[1024];

    // send '\0'
    buf[0]=0; out.write(buf, 0, 1); out.flush();

    while(true){
      int c=checkAck(in);
      if(c!='C'){
        break;
      }
      // read '0644 '
      in.read(buf, 0, 5);

      long filesize=0L;
      while(true){
        if(in.read(buf, 0, 1)<0){
          // error
          break;
        }
        if(buf[0]==' ')break;
        filesize=filesize*10L+(long)(buf[0]-'0');
      }
      String file = null;
      for(int i=0;;i++){
        in.read(buf, i, 1);
        if(buf[i]==(byte)0x0a){
          file=new String(buf, 0, i);
          break;
        }
      }
      System.out.println("下载文件 "+file+"("+filesize+")");

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      // read a content of lfile
      OutputStream fos = new FileOutputStream(localPath);
      int foo;
      while(true){
        if(buf.length<filesize) foo=buf.length;
        else foo=(int)filesize;
        foo=in.read(buf, 0, foo);
        if(foo<0){
          // error
          break;
        }
        fos.write(buf, 0, foo);
        filesize-=foo;
        if(filesize==0L) break;
      }
      fos.close();
      fos=null;

      if(checkAck(in)!=0){
        System.exit(0);
      }
      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();
    }
    channel.disconnect();
    session.disconnect();
  }

  private static boolean execCmd(Host host,String cmd,OutputStream os) throws Exception {
    JSch jsch = new JSch();
    Session session = jsch.getSession(host.getUser(),host.getIp(), host.getSshPort());
    session.setPassword(host.getPassword());
    session.setConfig("StrictHostKeyChecking","no");
    session.connect();
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    InputStream in = channel.getInputStream();
    channel.setCommand(cmd);
    channel.setErrStream(System.err);
    channel.connect();
    IOUtils.copy(in,os);
    channel.disconnect();
    while (!channel.isClosed()) {

    }
    System.out.println(String.format("服务器:%s 执行命令:%s 结果:%s", host.getHostName(), cmd, channel.getExitStatus()));
    session.disconnect();
    return  true;
  }



 private static int checkAck(InputStream in) throws IOException {
    int b=in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if(b==0) return b;
    if(b==-1) return b;

    if(b==1 || b==2){
      StringBuffer sb=new StringBuffer();
      int c;
      do {
        c=in.read();
        sb.append((char)c);
      }
      while(c!='\n');
      if(b==1){ // error
        System.out.print(sb.toString());
      }
      if(b==2){ // fatal error
        System.out.print(sb.toString());
      }
    }
    return b;
  }
 
 @Data
 @AllArgsConstructor
 @NoArgsConstructor
 static class Host {

     private String hostName;
     private String privateIp;
     private String publicIp;

     private int sshPort;
     private String user;
     private String password;


     public String getIp() {
         if (System.getProperty("NETWORK") != null && "PRIVATE".equals(System.getProperty("NETWORK")))
             return privateIp;
         return publicIp;
     }

     @Override
     public Host clone() {
         Host host = new Host();
         host.setPrivateIp(this.privateIp);
         host.setPublicIp(this.publicIp);
         host.setSshPort(this.sshPort);
         host.setPassword(this.password);
         host.setUser(user);
         host.setHostName(hostName);
         return host;
     }
 }

}
