package org.tunelar.tunelar_server;


import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.glxn.qrgen.QRCode;

import javax.swing.SwingConstants;

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

public class MainWindow extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow frame = new MainWindow();
					ImageIcon frameIcon = new ImageIcon("small_icon.png");
					frame.setIconImage(frameIcon.getImage());
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public MainWindow() throws IOException {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//setBounds(X, Y, ancho, alto);
		setBounds(600, 400, 460, 300);
		contentPane = new JPanel();
		contentPane.setBackground(Color.white);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		Font font = null;
		Font sizedFont = null;
		
		try {
			InputStream is = MainWindow.class.getResourceAsStream("Roboto-Bold.ttf");
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
		contentPane.add(lblTunelarServer);		
		

		
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
		contentPane.add(lblScanTheImage);
			
		//Find external IP
		URL whatismyip = new URL("http://checkip.amazonaws.com/");
		BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
		String extIp = in.readLine();
		System.out.println("External ip: "+extIp);		

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
		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(2042);
		sshd.setHost(Host);
		
		PasswordAuthenticator passAuth = new PasswordAuthenticator() {
			
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
		};
		
		sshd.setPasswordAuthenticator(passAuth);
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
		
		//Open External Port (UPNP)
		try {
			mapPort(Host,Port);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		//Configuration string (ShadowSocks format):
		//String config = RandomUser+":"+RandomPass+"@"+extIp+":"+Port; //To use app outside net
		String config = RandomUser+":"+RandomPass+"@"+Host+":"+Port; //To use app inside net (for testing)
		//System.out.println("Config: "+config);		
		
		//Convert string to Base64
		String config_base64 = Base64.encodeBytes(config.getBytes());		
		//System.out.println("Config en Base64: "+config_base64);		
		
		ByteArrayOutputStream output = QRCode.from("ss://"+config_base64).stream();

		//Displays QR
	    ImageIcon icon2 = new ImageIcon(output.toByteArray());
		JLabel QRImage = new JLabel("", icon2, JLabel.CENTER);
		QRImage.setBounds(149, 105, 142, 123);
		contentPane.add(QRImage);	
		
		/*
		// save the image to the output stream
		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		String dirName="C:\\Users\\Gaspar\\Documents\\TunelAr_Server";				
		try {
			BufferedImage image = ImageIO.read(input);
			ImageIO.write(image, "jpg", new File(dirName,"QR.jpg"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/	
		
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
}
