/*
 * MainFrame.java
 *
 * Created on 2016.8.19
 */

package com.yang.serialport.ui;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.yang.serialport.exception.NoSuchPort;
import com.yang.serialport.exception.NotASerialPort;
import com.yang.serialport.exception.PortInUse;
import com.yang.serialport.exception.ReadDataFromSerialPortFailure;
import com.yang.serialport.exception.SendDataToSerialPortFailure;
import com.yang.serialport.exception.SerialPortInputStreamCloseFailure;
import com.yang.serialport.exception.SerialPortOutputStreamCloseFailure;
import com.yang.serialport.exception.SerialPortParameterFailure;
import com.yang.serialport.exception.TooManyListeners;
import com.yang.serialport.manage.SerialPortManager;
import com.yang.serialport.utils.ByteUtils;
import com.yang.serialport.utils.ShowUtils;

import org.sqlite.*;

import com.yang.serialport.ui.*;

public class MainFrame extends JFrame {
	 
	Connection c = null;
    Statement stmt = null;
    // 输出流对象
    OutputStream outputStream;
    // Socket变量
    private Socket socket=null;
    private Socket client=null;
    private Socket websocketlink=null;
    private ServerSocket serverSocket = null;
    public String IP;
    public Server server;
    public Otcserver otcserver;
    private static final int SERVERPORT = 5555;
    public String str = "";
    public String responstr="";
    public String datesend = "";
    public String msg;

	/**
	 * 程序界面宽度
	 */
	public static final int WIDTH = 500;

	/**
	 * 程序界面高度
	 */
	public static final int HEIGHT = 360;

	public JTextArea dataView = new JTextArea();
	private JScrollPane scrollDataView = new JScrollPane(dataView);

	// 串口设置面板
	private JPanel serialPortPanel = new JPanel();
	private JLabel serialPortLabel = new JLabel("串口");
	private JLabel baudrateLabel = new JLabel("波特率");
	private JComboBox commChoice = new JComboBox();
	private JComboBox baudrateChoice = new JComboBox();

	// 操作面板
	private JPanel operatePanel = new JPanel();
	private JTextField dataInput = new JTextField();
	private JButton serialPortOperate = new JButton("停止接收");
	private JButton sendData = new JButton("开始接收");
   
	byte[] data;
	public int socketnortype = 0;
	public int sockettetype = 0;
	public int responsetype = 0;
	private List<String> commList = null;
	private SerialPort serialport;
	private SocketChannel socketChannel = null;
	public HashMap<String, Socket> clientList = new HashMap<>();
    public int clientcount=0;
    public boolean Firsttime=true;
	protected String fitemid;
    
	public MainFrame() {
		initView();
		initComponents();
		actionListener();
		initData();	
		
		//创建数据库
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:Sqlite.db");
	      System.out.println("Opened database successfully");

	      stmt = c.createStatement();
	      
	      String sql = "create table Tenghan (" +
	  			"electricity test, " +
				"voltage test, " +
				"sensor_Num test, " +
				"machine_id test, " +
				"welder_id test, " +
				"code test, " +
				"year test, " +
				"month test, " +
				"day test, " +
				"hour test, " +
				"minute test, " +
				"second test, " +
				"status test)"; 
	      stmt.executeUpdate(sql);
	      System.out.println("create table successfully");
	    }catch ( Exception e ) {
	    	System.out.println("The table has exist");
		    }
	}

	public void initView() {
		// 关闭程序
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		// 禁止窗口最大化
		setResizable(false);

		// 设置程序窗口居中显示
		Point p = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getCenterPoint();
		setBounds(p.x - WIDTH / 2, p.y - HEIGHT / 2, WIDTH, HEIGHT);
		this.setLayout(null);

		setTitle("欧地希转发器");
	}

	public void initComponents() {
		// 数据显示
		dataView.setFocusable(false);
		scrollDataView.setBounds(10, 10, 475, 200);
		add(scrollDataView);

		// 串口设置
		/*serialPortPanel.setBorder(BorderFactory.createTitledBorder("串口设置"));
		serialPortPanel.setBounds(10, 220, 170, 100);
		serialPortPanel.setLayout(null);
		add(serialPortPanel);

		serialPortLabel.setForeground(Color.gray);
		serialPortLabel.setBounds(10, 25, 40, 20);
		serialPortPanel.add(serialPortLabel);*/

		commChoice.setFocusable(false);
		commChoice.setBounds(60, 25, 100, 20);
		serialPortPanel.add(commChoice);

		baudrateLabel.setForeground(Color.gray);
		baudrateLabel.setBounds(10, 60, 40, 20);
		serialPortPanel.add(baudrateLabel);

		baudrateChoice.setFocusable(false);
		baudrateChoice.setBounds(60, 60, 100, 20);
		serialPortPanel.add(baudrateChoice);

		// 操作
		operatePanel.setBorder(BorderFactory.createTitledBorder("操作"));
		operatePanel.setBounds(70, 220, 375, 100);
		operatePanel.setLayout(null);
		add(operatePanel);

		serialPortOperate.setFocusable(false);
		serialPortOperate.setBounds(210, 40, 90, 30);
		operatePanel.add(serialPortOperate);

		sendData.setFocusable(false);
		sendData.setBounds(70, 40, 90, 30);
		operatePanel.add(sendData);
	}

	@SuppressWarnings("unchecked")
	public void initData() {
		commList = SerialPortManager.findPort();
		// 检查是否有可用串口，有则加入选项中
		if (commList == null || commList.size() < 1) {
			ShowUtils.warningMessage("没有搜索到有效串口！");
		} else {
			for (String s : commList) {
				commChoice.addItem(s);
			}
		}

		baudrateChoice.addItem("9600");
		baudrateChoice.addItem("19200");
		baudrateChoice.addItem("38400");
		baudrateChoice.addItem("57600");
		baudrateChoice.addItem("115200");
	}

	public void actionListener() {
		serialPortOperate.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ("关闭串口".equals(serialPortOperate.getText())
						&& serialport == null) {
					//openSerialPort(e);
				} else {
					closeSerialPort(e);
				}
			}
		});

		sendData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				sendData(e);
			}
		});
	}


	/**
	 * 打开串口
	 * 
	 * @param evt
	 *            点击事件
	 *//*
	private void openSerialPort(java.awt.event.ActionEvent evt) {
		// 获取串口名称
		String commName = (String) commChoice.getSelectedItem();
		// 获取波特率
		int baudrate = 9600;
		String bps = (String) baudrateChoice.getSelectedItem();
		baudrate = Integer.parseInt(bps);

		// 检查串口名称是否获取正确
		if (commName == null || commName.equals("")) {
			ShowUtils.warningMessage("没有搜索到有效串口！");
		} else {
			try {
				serialport = SerialPortManager.openPort(commName, baudrate);
				if (serialport != null) {
					dataView.setText("串口已打开" + "\r\n");
					serialPortOperate.setText("关闭串口");
				}
			} catch (SerialPortParameterFailure e) {
				e.printStackTrace();
			} catch (NotASerialPort e) {
				e.printStackTrace();
			} catch (NoSuchPort e) {
				e.printStackTrace();
			} catch (PortInUse e) {
				e.printStackTrace();
				ShowUtils.warningMessage("串口已被占用！");
			}
		}

		try {
			SerialPortManager.addListener(serialport, new SerialListener());
		} catch (TooManyListeners e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * 关闭串口
	 * 
	 * @param evt
	 *            点击事件
	 */
	private void closeSerialPort(java.awt.event.ActionEvent evt) {
		System.exit(0);
	}

	/**
	 * 发送数据
	 * 
	 * @param evt
	 *            点击事件
	 */
	private void sendData(java.awt.event.ActionEvent evt) {
		
		//初始化
		OutputStream out = null;
		byte[] order = null;
		InputStream in = null;
		byte[] bytes = null;
		
		try {
        	
			FileInputStream in1 = new FileInputStream("IPconfig.txt");  
            InputStreamReader inReader = new InputStreamReader(in1, "UTF-8");  
            BufferedReader bufReader = new BufferedReader(inReader);  
            String line = null; 
            int writetime=0;
				
		    while((line = bufReader.readLine()) != null){ 
		    	if(writetime==0){
	                IP=line;
	                writetime++;
		    	}
		    	else{
		    		fitemid=line;
		    		writetime=0;
		    		}
	    		}  
		    
			if(fitemid.length()!=2){
	    		int count = 2-fitemid.length();
	    		for(int i=0;i<count;i++){
	    			fitemid="0"+fitemid;
	    		}
	    	}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new Thread(reciver).start();;
		
		
		/*new Thread(soctrannormal).start();
		new Thread(soctrantechnique).start();
		new Thread(response).start();*/
			

		/*//打开串口
		String commName = (String) commChoice.getSelectedItem();
		// 获取波特率
		int baudrate = 9600;
		String bps = (String) baudrateChoice.getSelectedItem();
		baudrate = Integer.parseInt(bps);

		// 检查串口名称是否获取正确
		if (commName == null || commName.equals("")) {
			ShowUtils.warningMessage("没有搜索到有效串口！");
		} else {
			try {

				serialport = SerialPortManager.openPort(commName, baudrate);
				if (serialport != null) {
					dataView.setText("串口已打开" + "\r\n");
				}
				
				
				try {
					SerialPortManager.addListener(serialport, new SerialListener());
				} catch (TooManyListeners e) {
					e.printStackTrace();
				}
		    	
				
				
				
				Timer tExit = null; 
				tExit = new Timer();  
		        tExit.schedule(new TimerTask() {  
		            @Override  
		            public void run() { 
						Date date=new Date();
				    	SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
				    	String time=format.format(date);
				    	String time1="";
				    	String time2="";
				    	
				    	for (int i = 0; i < time.length()/2; i++)
						{
							String tstr1=time.substring(i*2, i*2+2);
							Integer k=Integer.valueOf(tstr1).intValue();
							time1=Integer.toHexString(k);
							time1=time1.toUpperCase();
							if(time1.length()==1){
				    			time1='0'+time1;
				        	}
							time2+=time1;
						}
				    	
				    	String data1 = "FAFFFF0804";
				    	String data2 = time2;
				    	String data3 = data1+data2;
				    	int data4 = 0;
				    	
					     for (int i11 = 1; i11 < data3.length()/2; i11++)
					     {
					    	String tstr1=data3.substring(i11*2, i11*2+2);
					    	data4+=Integer.valueOf(tstr1,16);
					     }
					    String data5 = ((Integer.toHexString(data4)).toUpperCase()).substring(1,3);
				    	String data  = data1+data2+data5+"F5";
				    	try {
							SerialPortManager.sendToPort(serialport,
									ByteUtils.hexStr2Byte(data));
						} catch (SendDataToSerialPortFailure e) {
							e.printStackTrace();
						} catch (SerialPortOutputStreamCloseFailure e) {
							e.printStackTrace();
						}
		            }  
		        }, 0,30*100); 
				
				
				
			} catch (SerialPortParameterFailure e) {
				e.printStackTrace();
			} catch (NotASerialPort e) {
				e.printStackTrace();
			} catch (NoSuchPort e) {
				e.printStackTrace();
			} catch (PortInUse e) {
				e.printStackTrace();
				ShowUtils.warningMessage("串口已被占用！");
			}
		}

		
		//读取数据
		try {
			in = serialport.getInputStream();
			// 获取buffer里的数据长度
			int bufflenth = in.available();
			while (bufflenth != 0) {
				// 初始化byte数组为buffer中数据的长度
				bytes = new byte[bufflenth];
				in.read(bytes);
				bufflenth = in.available();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}
	
	public Runnable reciver = new Runnable() {

		Thread workThread;
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				
				ServerSocket serverSocket = new ServerSocket(SERVERPORT);
				
				while (true) {  
					
					synchronized(this) {
						
				        client = serverSocket.accept();   
				        
				        clientcount++;
				        String countclient=Integer.toString(clientcount);
				        clientList.put(countclient, client);
				        
				        Date date=new Date();
				    	SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
				    	String time=format.format(date);
				    	String time1="";
				    	String time2="";
				    	
				    	for (int i = 0; i < time.length()/2; i++)
						{
							String tstr1=time.substring(i*2, i*2+2);
							Integer k=Integer.valueOf(tstr1).intValue();
							time1=Integer.toHexString(k);
							time1=time1.toUpperCase();
							if(time1.length()==1){
				    			time1='0'+time1;
				        	}
							time2+=time1;
						}
				    	
				    	String data1 = "FAFFFF0804";
				    	String data2 = time2;
				    	String data3 = data1+data2;
				    	int data4 = 0;
				    	
					     for (int i11 = 1; i11 < data3.length()/2; i11++)
					     {
					    	String tstr1=data3.substring(i11*2, i11*2+2);
					    	data4+=Integer.valueOf(tstr1,16);
					     }
					    String data5 = ((Integer.toHexString(data4)).toUpperCase()).substring(1,3);
				    	String data  = data1+data2+data5+"F5";
				    	
				    	DataOutputStream out = new DataOutputStream(client.getOutputStream());
				    	out.write(ByteUtils.hexStr2Byte(data));
				    	
				        Handler handler = new Handler(countclient,clientList); 
		                workThread = new Thread(handler);  
		                workThread.start();
						
					}
				}
			} catch (Exception ex) {    
	             ex.printStackTrace();  
	        }
		}
		
	};
	
	
	
	class Handler implements Runnable {

		public HashMap<String, Socket> clientList = new HashMap<>();
	    public String clientcount="";
		
		public Handler(String clientcount, HashMap<String, Socket> clientList) {
			// TODO Auto-generated constructor stub
			this.clientList=clientList;
			this.clientcount=clientcount;
			
		} 

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
                
				while (true) {  
					
					synchronized(this) {   
	                  
	                try {  
	                	client = clientList.get(clientcount);
	                    BufferedReader in = new BufferedReader(  
	                            new InputStreamReader(client.getInputStream()));  
	                      
	                    PrintWriter out = new PrintWriter(new BufferedWriter(  
	                            new OutputStreamWriter(client.getOutputStream())),true);  
	                      
	                    int zeroc=0;
	                    int i1=0;
	                    int zerocount=0;
	                    int linecount=0;
	                    str = "";
	                    byte[] datas1 = new byte[1024]; 
	                    client.getInputStream().read(datas1);
	                    for(i1=0;i1<datas1.length;i1++){
	                    	//判断为数字还是字母，若为字母+256取正数
	                    	if(datas1[i1]<0){
	                    		String r = Integer.toHexString(datas1[i1]+256);
	                    		String rr=r.toUpperCase();
	                        	//数字补为两位数
	                        	if(rr.length()==1){
	                    			rr='0'+rr;
	                        	}
	                        	//strdata为总接收数据
	                    		str += rr;
	                    	}
	                    	else{
	                    		String r = Integer.toHexString(datas1[i1]);

	                        	if(r.length()==1)
	                    			r='0'+r;
	                        	r=r.toUpperCase();
	                    		str+=r;	
	                    	}
	                    	linecount+=2;
	                    	
	                    	//去掉后面的0
	                    	if(datas1[i1]==0){
	                    		zerocount++;
	                    		if(zerocount>25){
	                    			str=str.substring(0, linecount-52);
	                    			break;
	                    		}	
	                    	}else{
                   			zerocount=0;
	                    	}
	                    }
						
						try {    
     		            	if(socketChannel==null){
     		            		socketChannel = SocketChannel.open(); 
	     		                SocketAddress socketAddress = new InetSocketAddress(IP, 5555);    
	     		                socketChannel.connect(socketAddress);
     		            	}
							
							str=str.substring(0,106)+fitemid+"F5";
     		            	
     		                SendAndReceiveUtil.sendData(socketChannel, str); 
     		                
     		                dataView.append(str + "\r\n");
     		                    
     		                socketnortype = 0;
     		                sockettetype = 1;
     		                
     		                /*String msg = SendAndReceiveUtil.receiveData(socketChannel);    
     		                if(msg != null){
     		                	System.out.println(msg);
     		                }*/

     		            
     		            } catch (Exception ex) { 
     		            	dataView.append("服务器未开启！" + "\r\n");
     		                ex.printStackTrace();  
     		            }
						

	                } catch (Exception e) {  
	                    System.out.println("S: Error "+e.getMessage());  
	                    e.printStackTrace();  
	                    
	                    	break;
	                    
	                } 
	                
				}
	                
	            }  
	        } catch (Exception e) {  
	            System.out.println("S: Error 2");  
	            e.printStackTrace();  
	        } 
		}
		
	}
	
	
	   /*public Runnable reciver = new Runnable() {
			private long timetran1;
			private long timetran2;
			private long timetran3;
			private Date time11;
			private Date time22;
			private Date time33;

			public void run() {
				  
			synchronized(this) {
				
	           try {
	                
					while (true) {  
						
						synchronized(this) {   
		                  
		                try {  
		                    BufferedReader in = new BufferedReader(  
		                            new InputStreamReader(client.getInputStream()));  
		                      
		                    PrintWriter out = new PrintWriter(new BufferedWriter(  
		                            new OutputStreamWriter(client.getOutputStream())),true);  
		                      
		                    int zeroc=0;
		                    int i1=0;
		                    int zerocount=0;
		                    int linecount=0;
		                    str = "";
		                    byte[] datas1 = new byte[1024]; 
		                    client.getInputStream().read(datas1);
		                    for(i1=0;i1<datas1.length;i1++){
		                    	//判断为数字还是字母，若为字母+256取正数
		                    	if(datas1[i1]<0){
		                    		String r = Integer.toHexString(datas1[i1]+256);
		                    		String rr=r.toUpperCase();
		                        	//数字补为两位数
		                        	if(rr.length()==1){
		                    			rr='0'+rr;
		                        	}
		                        	//strdata为总接收数据
		                    		str += rr;
		                    	}
		                    	else{
		                    		String r = Integer.toHexString(datas1[i1]);

		                        	if(r.length()==1)
		                    			r='0'+r;
		                        	r=r.toUpperCase();
		                    		str+=r;	
		                    	}
		                    	linecount+=2;
		                    	
		                    	//去掉后面的0
		                    	if(datas1[i1]==0){
		                    		zerocount++;
		                    		if(zerocount>25){
		                    			str=str.substring(0, linecount-52);
		                    			break;
		                    		}	
		                    	}else{
	                   			zerocount=0;
		                    	}
		                    }
							
							try {    
	     		            	if(socketChannel==null){
	     		            		socketChannel = SocketChannel.open(); 
		     		                SocketAddress socketAddress = new InetSocketAddress(IP, 5555);    
		     		                socketChannel.connect(socketAddress);
	     		            	}
								
								str=str.substring(0,106)+fitemid+"F5";
	     		            	
	     		                SendAndReceiveUtil.sendData(socketChannel, str); 
	     		                
	     		                dataView.append(str + "\r\n");
	     		                    
	     		                socketnortype = 0;
	     		                sockettetype = 1;
	     		                
	     		                String msg = SendAndReceiveUtil.receiveData(socketChannel);    
	     		                if(msg != null){
	     		                	System.out.println(msg);
	     		                }

	     		            
	     		            } catch (Exception ex) {    
	     		                ex.printStackTrace();  
	     		            }
							
		                    if(str.length()>30){
		                    	dataView.append(str + "\r\n");
		                    }
		                    
		                    if(str.subSequence(4,6).equals("22")){
		                    	String strdata1=str;
			                    String strdata2=strdata1.replaceAll("7C20", "00");
			                    String strdata3=strdata2.replaceAll("7C5E", "7E");
			                    String strdata4=strdata3.replaceAll("7C5C", "7C");
			                    String strdata =strdata4.replaceAll("7C5D", "7D");
			                    
			                    String weld = strdata.substring(2,4);
			                    if(weld.length()<4){
			                    	int length = 4 - weld.length();
			                    	for(int i=0;i<length;i++){
			                    		weld = "0" + weld;
			                    	}
			                    }
			                    
			                    String welder = strdata.substring(24,28);                
			                    String electricity = strdata.substring(28,32);
			                    String voltage = strdata.substring(32,36);
			                    int voltageint = Integer.valueOf(voltage,16) / 10;
			                    voltage = Integer.toHexString(voltageint);
			                    if(voltage.length()!=4){
			                    	int lenth = 4 - voltage.length();
			                    	for(int i=0;i<lenth;i++){
			                    		voltage = "0" + voltage;
			                    	}
			                    }
			                    
			                    String sensor = strdata.substring(48,52);
			                    
			                    if(Firsttime){
			                    	Date timetran = new Date();
				                    timetran1 = timetran.getTime();
				                    Date time11 = new Date(timetran1);
				                    timetran2 = timetran1 + 1000;
				                    Date time22 = new Date(timetran2);
				                    timetran3 = timetran2 + 1000;
				                    Date time33 = new Date(timetran3);
				                    timetran1=timetran3;
				                    Firsttime=false;
			                    }else{
			                    	timetran1=timetran1 + 1000;
			                    	Date time11 = new Date(timetran1);
			                    	timetran2 = timetran1 + 1000;
				                    Date time22 = new Date(timetran2);
				                    timetran3 = timetran2 + 1000;
				                    Date time33 = new Date(timetran3);
				                    timetran1=timetran3;
			                    }
			                    
			                    String time1 = DateTools.format("yyMMddHHmmss", time11);
			                    String time2 = DateTools.format("yyMMddHHmmss", time22);
			                    String time3 = DateTools.format("yyMMddHHmmss", time33);
			                    
			                    String year1 = time1.substring(0,2);
			                    String year161 = Integer.toHexString(Integer.valueOf(year1));
			                    year161=year161.toUpperCase();
			                    if(year161.length()==1){
			                    	year161='0'+year161;
		                      	}
			                    String month1 = time1.substring(2,4);
			                    String month161 = Integer.toHexString(Integer.valueOf(month1));
			                    month161=month161.toUpperCase();
			                    if(month161.length()==1){
			                    	month161='0'+month161;
		                      	}
			                    String day1 = time1.substring(4,6);
			                    String day161 = Integer.toHexString(Integer.valueOf(day1));
			                    day161=day161.toUpperCase();
			                    if(day161.length()==1){
			                    	day161='0'+day161;
		                      	}
			                    String hour1 = time1.substring(6,8);
			                    String hour161 = Integer.toHexString(Integer.valueOf(hour1));
			                    hour161=hour161.toUpperCase();
			                    if(hour161.length()==1){
			                    	hour161='0'+hour161;
		                      	}
			                    String minute1 = time1.substring(8,10);
			                    String minute161 = Integer.toHexString(Integer.valueOf(minute1));
			                    minute161=minute161.toUpperCase();
			                    if(minute161.length()==1){
			                    	minute161='0'+minute161;
		                      	}
			                    String second1 = time1.substring(10,12);
			                    String second161 = Integer.toHexString(Integer.valueOf(second1));
			                    second161=second161.toUpperCase();
			                    if(second161.length()==1){
			                    	second161='0'+second161;
		                      	}
			                    
			                    String year2 = time2.substring(0,2);
			                    String year162 = Integer.toHexString(Integer.valueOf(year2));
			                    year162=year162.toUpperCase();
			                    if(year162.length()==1){
			                    	year162='0'+year162;
		                      	}
			                    String month2 = time2.substring(2,4);
			                    String month162 = Integer.toHexString(Integer.valueOf(month2));
			                    month162=month162.toUpperCase();
			                    if(month162.length()==1){
			                    	month162='0'+month162;
		                      	}
			                    String day2 = time2.substring(4,6);
			                    String day162 = Integer.toHexString(Integer.valueOf(day2));
			                    day162=day162.toUpperCase();
			                    if(day162.length()==1){
			                    	day162='0'+day162;
		                      	}
			                    String hour2 = time2.substring(6,8);
			                    String hour162 = Integer.toHexString(Integer.valueOf(hour2));
			                    hour162=hour162.toUpperCase();
			                    if(hour162.length()==1){
			                    	hour162='0'+hour162;
		                      	}
			                    String minute2 = time2.substring(8,10);
			                    String minute162 = Integer.toHexString(Integer.valueOf(minute2));
			                    minute162=minute162.toUpperCase();
			                    if(minute162.length()==1){
			                    	minute162='0'+minute162;
		                      	}
			                    String second2 = time2.substring(10,12);
			                    String second162 = Integer.toHexString(Integer.valueOf(second2));
			                    second162=second162.toUpperCase();
			                    if(second162.length()==1){
			                    	second162='0'+second162;
		                      	}
			                    
			                    String year3 = time3.substring(0,2);
			                    String year163 = Integer.toHexString(Integer.valueOf(year3));
			                    year163=year163.toUpperCase();
			                    if(year163.length()==1){
			                    	year163='0'+year163;
		                      	}
			                    String month3 = time3.substring(2,4);
			                    String month163 = Integer.toHexString(Integer.valueOf(month3));
			                    month163=month163.toUpperCase();
			                    if(month163.length()==1){
			                    	month163='0'+month163;
		                      	}
			                    String day3 = time3.substring(4,6);
			                    String day163 = Integer.toHexString(Integer.valueOf(day3));
			                    day163=day163.toUpperCase();
			                    if(day163.length()==1){
			                    	day163='0'+day163;
		                      	}
			                    String hour3 = time3.substring(6,8);
			                    String hour163 = Integer.toHexString(Integer.valueOf(hour3));
			                    hour163=hour163.toUpperCase();
			                    if(hour163.length()==1){
			                    	hour163='0'+hour163;
		                      	}
			                    String minute3 = time3.substring(8,10);
			                    String minute163 = Integer.toHexString(Integer.valueOf(minute3));
			                    minute163=minute163.toUpperCase();
			                    if(minute163.length()==1){
			                    	minute163='0'+minute163;
		                      	}
			                    String second3 = time3.substring(10,12);
			                    String second163 = Integer.toHexString(Integer.valueOf(second3));
			                    second163=second163.toUpperCase();
			                    if(second163.length()==1){
			                    	second163='0'+second163;
		                      	}
			                    
			                    String status = "09";
			                    int electri = Integer.valueOf(electricity,16);
			                    if(electri>=0 && electri<=5){
			                    	status = "00";
			                    }
			                    else{
			                    	status = "03";
			                    }
			                    
			                    datesend = "00003101" + weld + welder + "00000001" 
			                    + electricity + voltage + sensor + status + year161 + month161 + day161 + hour161 + minute161 + second161 
			                    + electricity + voltage + sensor + status + year162 + month162 + day162 + hour162 + minute162 + second162
			                    + electricity + voltage + sensor + status + year163 + month163 + day163 + hour163 + minute163 + second163;
			                    
			                    int check = 0;
			                    byte[] data1=new byte[datesend.length()/2];
								for (int i = 0; i < data1.length; i++)
								{
									String tstr1=datesend.substring(i*2, i*2+2);
									Integer k=Integer.valueOf(tstr1, 16);
									check += k;
								}
		       
								
								try {
		     						FileInputStream in1 = new FileInputStream("IPconfig.txt");  
		     			            InputStreamReader inReader = new InputStreamReader(in1, "UTF-8");  
		     			            BufferedReader bufReader = new BufferedReader(inReader);  
		     			            String line = null; 
		     			            int writetime=0;
		     						
		     					    while((line = bufReader.readLine()) != null){ 
		     					    	if(writetime==0){
		     				                IP=line;
		     				                writetime++;
		     					    	}
		     					    	else{
		     					    		fitemid=line;
		     					    		writetime=0;
		     					    	}
		     			            }  

		     					} catch (FileNotFoundException e) {
		     						// TODO Auto-generated catch block
		     						e.printStackTrace();
		     					} catch (IOException e) {
		     						// TODO Auto-generated catch block
		     						e.printStackTrace();
		     					}
								
								
								String checksend = Integer.toHexString(check);
								checksend = checksend.substring(1,3);
								checksend = checksend.toUpperCase();
								
								datesend = "FA" + datesend + checksend + fitemid + "F5";
								
								dataView.append(datesend + "\r\n");
								
								socketnortype = 1;
		                    }

							
							/*byte[] data2=new byte[datesend.length()/2];
							for (int i = 0; i < data2.length; i++)
							{
								String tstr1=datesend.substring(i*2, i*2+2);
								Integer k=Integer.valueOf(tstr1, 16);
								data2[i]=(byte)k.byteValue();
							}
							
							data = data2;

		                } catch (Exception e) {  
		                    System.out.println("S: Error "+e.getMessage());  
		                    e.printStackTrace();  
		                } 
		                
					}
		                
		            }  
		        } catch (Exception e) {  
		            System.out.println("S: Error 2");  
		            e.printStackTrace();  
		        }  
	           
			}
	 
			}
	   };*/
	   
	   
	   
	   
/*	   

	private class SerialListener implements SerialPortEventListener {
		*//**
		 * 处理监控到的串口事件
		 *//*
		public void serialEvent(SerialPortEvent serialPortEvent) {

			switch (serialPortEvent.getEventType()) {

			case SerialPortEvent.BI: // 10 通讯中断
				ShowUtils.errorMessage("与串口设备通讯中断");
				break;

			case SerialPortEvent.DATA_AVAILABLE: // 1 串口存在可用数据
				
				
				try {
					Thread.sleep(20);
				} catch (InterruptedException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				
				 InputStream in1 = null;
				
				//读取串口数据处理
					try {
							in1 = serialport.getInputStream();
						//获取buffer里的数据长度
							int bufflenth = in1.available();
	
							while (bufflenth != 0) {                             
				                data = new byte[bufflenth];    //初始化byte数组为buffer中数据的长度
				                try {
									in1.read(data);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				                try {
									bufflenth = in1.available();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				            }
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					//new Thread(sqlite).start();
					new Thread(soctran).start();
					//new Thread(websocketstart).start();
				
				
			}
		}
	}*/

	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new MainFrame().setVisible(true);
			}
		});
	}
	
	 public Runnable sqlite = new Runnable() {
			public void run() {
				String strdata = "";
				String insql;
			    // 接收服务器发送过来的消息
			    String response;
			    
			    
				try {
					if (serialport == null) {
						ShowUtils.errorMessage("串口对象为空！监听失败！");
					}
					
					else {
						dataView.append(ByteUtils.byteArrayToHexString(data,
								true) + "\r\n");
						
						 for(int i=0;i<data.length;i++){
                         	
                         	//判断为数字还是字母，若为字母+256取正数
                         	if(data[i]<0){
                         		String r = Integer.toHexString(data[i]+256);
                         		String rr=r.toUpperCase();
                             	System.out.print(rr);
                             	//数字补为两位数
                             	if(rr.length()==1){
                         			rr='0'+rr;
                             	}
                             	//strdata为总接收数据
                         		strdata += rr;
                         		
                         	}
                         	else{
                         		String r = Integer.toHexString(data[i]);
                             	System.out.print(r);
                             	if(r.length()==1)
                         			r='0'+r;
                         		strdata+=r;	
                         		
                         	}
                         }
                         
                         response=strdata;
                         
                         //数据写入数据库
                         String [] stringArr = strdata.split("FD");
                         for(int i =0;i < stringArr.length;i++)
         		        {
                      		String electricity = stringArr[i].subSequence(5, 9).toString();
                            String voltage = stringArr[i].subSequence(9, 13).toString();
                            String sensor_Num = stringArr[i].subSequence(13, 17).toString();
                            String machine_id = stringArr[i].subSequence(17, 21).toString();
                            String welder_id = stringArr[i].subSequence(21, 25).toString();
                            String code = stringArr[i].subSequence(25, 33).toString();
                            String year = stringArr[i].subSequence(33, 35).toString();
                            String month = stringArr[i].subSequence(35, 37).toString();
                            String day = stringArr[i].subSequence(37, 39).toString();
                            String hour = stringArr[i].subSequence(39, 41).toString();
                            String minute = stringArr[i].subSequence(41, 43).toString();
                            String second = stringArr[i].subSequence(43, 45).toString();
                            String status = stringArr[i].subSequence(45, 47).toString();
                            
                            insql = "insert into Tenghan(electricity,voltage,sensor_Num,machine_id,welder_id,code,year,month,day,hour,minute,second,status) "
                            		+ "values('"+ electricity +"','" + voltage + "','" + sensor_Num + "'"
                            				+ ","+ "'" + machine_id + "','" + welder_id + "','" + code + "'"
                            						+ ",'" + year + "','" + month + "','" + day + "','" + hour + "'"
                            								+ ",'" + minute + "','" + second + "','" + status + "')";
                            stmt.executeUpdate(insql);

         		        }
                         System.out.println("The data has writed");
                         
					}
				} catch (Exception e) {
					ShowUtils.errorMessage(e.toString());
					// 发生读取错误时显示错误信息后退出系统
					System.exit(0);
				}
			}
	 };
	 
	 public Runnable soctrannormal = new Runnable() {
			public void run() {
				while(true){
					
					synchronized(this) { 
		
						if(socketnortype == 1){
							
							
							String strdata = "";
							/*String insql;
						    // 接收服务器发送过来的消息
						    String response;
				
							 for(int i=0;i<data.length;i++){
		                     	
		                     	//判断为数字还是字母，若为字母+256取正数
		                     	if(data[i]<0){
		                     		String r = Integer.toHexString(data[i]+256);
		                     		String rr=r.toUpperCase();
		                         	//数字补为两位数
		                         	if(rr.length()==1){
		                     			rr='0'+rr;
		                         	}
		                         	//strdata为总接收数据
		                     		strdata += rr;
		                     		
		                     	}
		                     	else{
		                     		String r = Integer.toHexString(data[i]);
		                         	if(r.length()==1)
		                     			r='0'+r;
		                         	r=r.toUpperCase();
		                     		strdata+=r;	
		                     		
		                     	}
		                     }
		                     response=strdata;
		                     
		                    byte[] bb3=new byte[strdata.length()/2];
		 					for (int i1 = 0; i1 < bb3.length; i1++)
		 					{
		 						String tstr1=strdata.substring(i1*2, i1*2+2);
		 						Integer k=Integer.valueOf(tstr1, 16);
		 						bb3[i1]=(byte)k.byteValue();
		 					}*/
		                     
		 					
		 					try {
		 				    	File f = new File("IPconfig.txt");   
		 				        InputStream ing;
		 						ing = new FileInputStream(f);
		 				        byte[] b = new byte[1024];   
		 				        int len = 0;   
		 				        int temp=0;          //所有读取的内容都使用temp接收   
		 				        try {
		 							while((temp=ing.read())!=-1){    //当没有读取完时，继续读取   
		 							    b[len]=(byte)temp;   
		 							    len++;   
		 							}
		 						} catch (IOException e) {
		 							// TODO Auto-generated catch block
		 							e.printStackTrace();
		 						} 
		 				        IP=new String(b,0,len);
		 					} catch (FileNotFoundException e) {
		 						// TODO Auto-generated catch block
		 						e.printStackTrace();
		 					} 
		 					
		 					
		 					try {    
	     		            	if(socketChannel==null){
	     		            		socketChannel = SocketChannel.open(); 
		     		                SocketAddress socketAddress = new InetSocketAddress(IP, 5555);    
		     		                socketChannel.connect(socketAddress);
	     		            	}
	     		                
	     		                SendAndReceiveUtil.sendData(socketChannel, datesend); 
	     		                
	     		                dataView.append(datesend + "\r\n");
	     		                    
	     		                socketnortype = 0;
	     		                sockettetype = 1;
	     		                
	     		                /*String msg = SendAndReceiveUtil.receiveData(socketChannel);    
	     		                if(msg != null){
	     		                	System.out.println(msg);
	     		                }*/

	     		            
	     		            } catch (Exception ex) {    
	     		                ex.printStackTrace();  
	     		            }
		 					
		 					
		 					/*if(IP!=null){
		 						if(socket==null){
		 							try {
										socket = new Socket(IP, 5555);
									} catch (IOException e1) {
										dataView.setText("服务器连接失败" + "\r\n");
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
		 						}
	 							
			                    
			                    if(socket!=null){
			                    	System.out.print("服务器连接成功"+ "\r\n");
			                    }
			                    else{
			                    	dataView.setText("服务器连接失败" + "\r\n");
			                    	System.out.print("服务器连接失败"+ "\r\n");
			                    }


				                try {
				                	//发送消息
				                    // 步骤1：从Socket 获得输出流对象OutputStream
				                    // 该对象作用：发送数据
				                    outputStream = socket.getOutputStream();
			
				                    // 步骤2：写入需要发送的数据到输出流对象中
									dataView.append(ByteUtils.byteArrayToHexString(data,
											true) + "\r\n");
				                    outputStream.write(bb3);
				                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
			
				                    // 步骤3：发送数据到服务端
				                    outputStream.flush();
			
				                    responsetype = 1;
				                    sockettype = 0;  

				 		           
				                } catch (IOException e1) {
				                    e1.printStackTrace();
				                }
		 					}*/
							
						}
										
					}
				
				}
			}
	 };
	 
	 
	 public Runnable soctrantechnique = new Runnable() {
			public void run() {
				
			while(true){
				
				synchronized(this) { 
	
					if(sockettetype == 1){
						
						msg = SendAndReceiveUtil.receiveData(socketChannel);
						if(msg .length()>0){
 		                	/*if (rmsg.length()>0)
 		                	{
 		                		//add list or pro msg
 		                		msg=rmsg;
 		                	}*/
							responsetype = 1;
							sockettetype = 0;
							
 		                	System.out.println(msg);
 		                }
						
						
						/*String rmsg="";
						rmsg = SendAndReceiveUtil.receiveData(socketChannel);    
     		                if(rmsg != null){
     		                	if (rmsg.length()>0)
     		                	{
     		                		//add list or pro msg
     		                		msg=rmsg;
     		                	}
     		                	System.out.println(msg);
     		                }

     		               responsetype = 1;*/
     		                
	 					/*if(IP!=null){
	 						if(socket==null){
	 							try {
									socket = new Socket(IP, 5555);
								} catch (IOException e1) {
									dataView.setText("服务器连接失败" + "\r\n");
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
	 						}
 							
		                    
		                    if(socket!=null){
		                    	System.out.print("服务器连接成功"+ "\r\n");
		                    }
		                    else{
		                    	dataView.setText("服务器连接失败" + "\r\n");
		                    	System.out.print("服务器连接失败"+ "\r\n");
		                    }


			                try {
			                	//发送消息
			                    // 步骤1：从Socket 获得输出流对象OutputStream
			                    // 该对象作用：发送数据
			                    outputStream = socket.getOutputStream();
		
			                    // 步骤2：写入需要发送的数据到输出流对象中
								dataView.append(ByteUtils.byteArrayToHexString(data,
										true) + "\r\n");
			                    outputStream.write(bb3);
			                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
		
			                    // 步骤3：发送数据到服务端
			                    outputStream.flush();
		
			                    responsetype = 1;
			                    sockettype = 0;  

			 		           
			                } catch (IOException e1) {
			                    e1.printStackTrace();
			                }
	 					}*/
						
					}
									
				}
			
			}
			
		}

	 };
	 
	 public Runnable response = new Runnable() {
			public void run() {
				
				while(true){
					
					synchronized(this) {
						
						if(responsetype == 1){
							
							try {
								/*BufferedReader in = new BufferedReader(  
			                            new InputStreamReader(socket.getInputStream()));  
			                      
			                    PrintWriter out = new PrintWriter(new BufferedWriter(  
			                            new OutputStreamWriter(socket.getOutputStream())),true);  
			                      
			                    int zeroc=0;
			                    int i1=0;
			                    int zerocount=0;
			                    int linecount=0;
			                    responstr = "";
			                    byte[] datas1 = new byte[1024]; 
			                    
								socket.getInputStream().read(datas1);
								
			                    for(i1=0;i1<datas1.length;i1++){
			                    	//判断为数字还是字母，若为字母+256取正数
			                    	if(datas1[i1]<0){
			                    		String r = Integer.toHexString(datas1[i1]+256);
			                    		String rr=r.toUpperCase();
			                        	//数字补为两位数
			                        	if(rr.length()==1){
			                    			rr='0'+rr;
			                        	}
			                        	//strdata为总接收数据
			                        	responstr += rr;
			                    	}
			                    	else{
			                    		String r = Integer.toHexString(datas1[i1]);
	
			                        	if(r.length()==1)
			                    			r='0'+r;
			                        	r=r.toUpperCase();
			                        	responstr+=r;	
			                    	}
			                    	linecount+=2;
			                    	
			                    	//去掉后面的0
			                    	if(datas1[i1]==0){
			                    		zerocount++;
			                    		if(zerocount>25){
			                    			responstr=responstr.substring(0, linecount-52);
			                    			break;
			                    		}	
			                    	}else{
		                   			zerocount=0;
		                   		}
			                    }

							System.out.println(responstr);*/
							
							byte[] te = new byte[msg.length()/2];
                            for (int i = 0; i < msg.length()/2; i++)
                            {
                              String tstr1=msg.substring(i*2, i*2+2);
                              Integer k=Integer.valueOf(tstr1,16);
                              te[i]=(byte)k.byteValue();
                            }
                    	    
                    	    OutputStream outputStream = client.getOutputStream();
                    	    
                    	    outputStream.write(te);
                    	    
                    	    outputStream.flush();
                    	    
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							responsetype = 0;
							 
						}
					}		
				}
			}
	 };

	public void DateView(String datesend) {
		// TODO Auto-generated method stub
		dataView.setText(datesend + "\r\n");
		
	}
				
 }  
	 
