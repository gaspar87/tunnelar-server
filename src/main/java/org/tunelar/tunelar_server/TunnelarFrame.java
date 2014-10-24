package org.tunelar.tunelar_server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import net.glxn.qrgen.QRCode;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.support.igd.PortMappingListener;
import org.teleal.cling.support.model.PortMapping;

public class TunnelarFrame extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4433646950779301755L;
	private JPanel contentPane = new JPanel();
	private static boolean RIGHT_TO_LEFT = false;
	private SshServer sshd;

	
	/**
	 * Create the frame.
	 * @throws MalformedURLException 
	 * @throws IOException 
	 */
	public TunnelarFrame() throws MalformedURLException, IOException {
		fullFillGUI();						
		
		try {
			getPublicIP();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			throw e1;
		} catch (IOException e1) {
			e1.printStackTrace();
			throw e1;
		}		

		//Server data
		String Host = InetAddress.getLocalHost().getHostAddress(); //esta es la internal address
		System.out.println("Internal ip: "+Host);
		int Port = 22; 
		//Generates random user and pass for additional security
		
		RandomString rs = new RandomString(8);
		final String RandomUser = rs.nextString();
		final String RandomPass = rs.nextString();
		System.out.println("Random User: "+RandomUser);
		System.out.println("Random Pass: "+RandomPass);
							
		//SSH Server
		startSSHServer(Host, RandomUser, RandomPass);		
				
		//Open External Port (UPNP)
		try {
			mapPort(Host,Port);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		ByteArrayOutputStream output = generateQRCode(Host, Port, RandomUser,
				RandomPass);

		showQRCode(output);	
	}

	private ByteArrayOutputStream generateQRCode(String Host, int Port,
			final String RandomUser, final String RandomPass) {
		//Configuration string (ShadowSocks format):
		//String config = RandomUser+":"+RandomPass+"@"+extIp+":"+Port; //To use app outside net
		String config = RandomUser+":"+RandomPass+"@"+Host+":"+Port; //To use app inside net (for testing)
		//System.out.println("Config: "+config);		
		
		//Convert string to Base64
		String config_base64 = Base64.encodeBytes(config.getBytes());		
		//System.out.println("Config en Base64: "+config_base64);		
		
		ByteArrayOutputStream output = QRCode.from("ss://"+config_base64).stream();
		return output;
	}

	private void showQRCode(ByteArrayOutputStream output) {
		//Displays QR
	    ImageIcon icon2 = new ImageIcon(output.toByteArray());
		JLabel QRImage = new JLabel("", icon2, JLabel.CENTER);
		QRImage.setBounds(149, 105, 142, 123);
		contentPane.add(QRImage, BorderLayout.CENTER);
	}

	private void startSSHServer(String Host, final String RandomUser, final String RandomPass) throws IOException {
		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(2042);
		sshd.setHost(Host);
		
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			
			public boolean authenticate(String user, String pass, ServerSession session) {
				if(user.equals(RandomUser) && pass.equals(RandomPass)){
					System.out.println("Autenticación correcta");
					return true;
				}
				else{
					System.out.println("Autenticación fallida");
					return false;
				}
			}
		});
		
		//Create a new host key or read an existing one
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("key.ser"));
		
		ForwardingFilter bogusForwardingFilter = new ForwardingFilter (){

			public boolean canForwardAgent(Session session) {
				// TODO Auto-generated method stub
				return true;
			}

			public boolean canForwardX11(Session session) {
				// TODO Auto-generated method stub
				return true;
			}

			public boolean canListen(SshdSocketAddress address, Session session) {
				// TODO Auto-generated method stub
				return true;
			}

			public boolean canConnect(SshdSocketAddress address, Session session) {
				// TODO Auto-generated method stub
				return true;
			}
			
		};
		
		sshd.setTcpipForwardingFilter(bogusForwardingFilter);
		sshd.start();
		System.out.println("Servidor SSH iniciado en el puerto "+sshd.getPort());		
	}

	private String getPublicIP() throws MalformedURLException, IOException {
		//Find external IP
		URL whatismyip = new URL("http://checkip.amazonaws.com/");
		BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
		String extIp = in.readLine();
		System.out.println("External ip: "+extIp);
		return extIp;
	}
	
	private void mapPort(String Host, int Port) throws IOException, InterruptedException{
		
		PortMapping desiredMapping =
		        new PortMapping(
		                Port,
		                Host,
		                PortMapping.Protocol.TCP,
		                "TunnelAr Server"
		        );
		
		UpnpService upnpService =
		        new UpnpServiceImpl(
		                new PortMappingListener(desiredMapping)
		        );
		
		upnpService.getControlPoint().search();		
	}	
	
	private void fullFillGUI() throws IOException {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//setBounds(X, Y, ancho, alto);
				setBounds(600, 400, 460, 300);
         
        if (RIGHT_TO_LEFT) {
            contentPane.setComponentOrientation(
                    java.awt.ComponentOrientation.RIGHT_TO_LEFT);
        }				
		
		contentPane.setBackground(Color.white);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));		
		contentPane.setLayout(new BorderLayout());
		
		Font font = null;
		Font sizedFont = null;
		
		try {
			InputStream is = MainWindow.class.getResourceAsStream("fonts/Roboto-Bold.ttf");
			font = Font.createFont(Font.TRUETYPE_FONT, is);
			sizedFont = font.deriveFont(32f);
		} catch (FontFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		JLabel lblTunelarServer = new JLabel("TunnelAr");
		lblTunelarServer.setHorizontalAlignment(SwingConstants.CENTER);
		lblTunelarServer.setBounds(149, 23, 133, 36);
		lblTunelarServer.setForeground(new Color(116,136,161));
		try {
			lblTunelarServer.setFont(sizedFont);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		contentPane.add(lblTunelarServer, BorderLayout.PAGE_START);		
		
		//Displays image title
		/*
		JLabel title = new JLabel(createImageIcon("tunnelar.jpg", "title"));
		//title.setBounds(149, 23, 133, 36);
		title.setBounds(125, 23, 200, 50);
	    contentPane.add(title);*/				
				
		
		//JLabel lblScanTheImage = new JLabel("Scan the code below to configure mobile device");
		JLabel lblScanTheImage = new JLabel("Escanear el código QR para configurar el dispositivo");
		lblScanTheImage.setHorizontalAlignment(SwingConstants.CENTER);
		lblScanTheImage.setBounds(83, 80, 300, 14);
		contentPane.add(lblScanTheImage, BorderLayout.PAGE_END);
		
		setContentPane(contentPane);
	}
}
